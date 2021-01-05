package com.gtbluesky.gles.egl

import android.graphics.SurfaceTexture
import android.opengl.*
import android.util.Log
import android.view.Surface
import kotlin.IllegalStateException


/**
 * Core EGL state (display, context, config).
 * The EGLContext must only be attached to one thread at a time.  This class is not thread-safe.
 */
class EglCore @JvmOverloads constructor(sharedContext: EGLContext? = null, flags: Int = 0) {

    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglConfig: EGLConfig? = null
    var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
        private set
    private var glVersion = -1

    companion object {

        private val TAG = this::class.java.simpleName

        /**
         * Constructor flag: surface must be recordable.  This discourages EGL from using a
         * pixel format that cannot be converted efficiently to something usable by the video
         * encoder.
         */
        const val FLAG_RECORDABLE = 0x01

        /**
         * Constructor flag: ask for GLES3, fall back to GLES2 if not available.  Without this
         * flag, GLES2 is used.
         */
        const val FLAG_TRY_GLES3 = 0x02

        // Android-specific extension.
        private const val EGL_RECORDABLE_ANDROID = 0x3142

        /**
         * Writes the current display, context, and surface to the log.
         */
        @JvmStatic
        fun logCurrent(msg: String) {
            val display = EGL14.eglGetCurrentDisplay()
            val context = EGL14.eglGetCurrentContext()
            val surface = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW)
            Log.i(TAG, "Current EGL ($msg): display=$display, context=$context, surface=$surface")
        }
    }

    init {
        check(eglDisplay == EGL14.EGL_NO_DISPLAY) {
            "EGL already set up"
        }

        val sharedEglContext = sharedContext ?: EGL14.EGL_NO_CONTEXT

        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        check(eglDisplay != EGL14.EGL_NO_DISPLAY) {
            "unable to get EGL14 display"
        }
        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            eglDisplay = EGL14.EGL_NO_DISPLAY
            throw IllegalStateException("unable to initialize EGL14")
        }

        // Try to get a GLES3 context, if requested.
        if ((flags and FLAG_TRY_GLES3) != 0) {
            val config = getConfig(flags, 3)
            if (config != null) {
                val attrib3List = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE)
                val context = EGL14.eglCreateContext(
                    eglDisplay, config, sharedEglContext,
                    attrib3List, 0
                )
                checkEglError("eglCreateContext")
                if (EGL14.eglGetError() == EGL14.EGL_SUCCESS) {
                    //Log.d(TAG, "Got GLES 3 config");
                    eglConfig = config
                    eglContext = context
                    glVersion = 3
                }
            }
        }
        // GLES 2 only, or GLES 3 attempt failed
        if (eglContext == EGL14.EGL_NO_CONTEXT) {
            val config = getConfig(flags, 2) ?: throw IllegalStateException("Unable to find a suitable EGLConfig")
            val attrib2List = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
            val context = EGL14.eglCreateContext(
                eglDisplay, config, sharedEglContext,
                attrib2List, 0
            )
            checkEglError("eglCreateContext")
            eglConfig = config
            eglContext = context
            glVersion = 2
        }

        // Confirm with query.
        val values = IntArray(1)
        EGL14.eglQueryContext(
            eglDisplay, eglContext, EGL14.EGL_CONTEXT_CLIENT_VERSION, values, 0
        )
        Log.d(TAG, "EGLContext created, client version ${values[0]}")
    }

    /**
     * Finds a suitable EGLConfig.
     *
     * @param flags Bit flags from constructor.
     * @param version Must be 2 or 3.
     */
    private fun getConfig(flags: Int, version: Int): EGLConfig? {
        var renderableType = EGL14.EGL_OPENGL_ES2_BIT
        if (version >= 3) {
            renderableType = renderableType or EGLExt.EGL_OPENGL_ES3_BIT_KHR
        }

        // The actual surface is generally RGBA or RGBX, so situationally omitting alpha
        // doesn't really help.  It can also lead to a huge performance hit on glReadPixels()
        // when reading into a GL_RGBA buffer.
        val attribList = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_DEPTH_SIZE, 16,
            EGL14.EGL_STENCIL_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, renderableType,
            EGL14.EGL_NONE, 0, // placeholder for recordable [@-3]
            EGL14.EGL_NONE
        )
        if ((flags and FLAG_RECORDABLE) != 0) {
            attribList[attribList.size - 3] =
                EGL_RECORDABLE_ANDROID
            attribList[attribList.size - 2] = 1
        }
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(
                eglDisplay, attribList, 0, configs, 0, configs.size,
                numConfigs, 0
            )
        ) {
            Log.w(TAG, "unable to find RGB8888 / $version EGLConfig")
            return null
        }
        return configs[0]
    }

    /**
     * Discards all resources held by this class, notably the EGL context.  This must be
     * called from the thread where the context was created.
     *
     *
     * On completion, no context will be current.
     */
    fun release() {
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
            // Android is unusual in that it uses a reference-counted EGLDisplay.  So for
            // every eglInitialize() we need an eglTerminate().
            EGL14.eglMakeCurrent(
                eglDisplay,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT
            )
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(eglDisplay)
        }

        eglDisplay = EGL14.EGL_NO_DISPLAY
        eglContext = EGL14.EGL_NO_CONTEXT
        eglConfig = null
    }

    /**
     * Destroys the specified surface.  Note the EGLSurface won't actually be destroyed if it's
     * still current in a context.
     */
    fun releaseSurface(eglSurface: EGLSurface) {
        EGL14.eglDestroySurface(eglDisplay, eglSurface)
    }

    /**
     * Creates an EGL surface associated with a Surface.
     * If this is destined for MediaCodec, the EGLConfig should have the "recordable" attribute.
     */
    fun createWindowSurface(surface: Any): EGLSurface {
        check(!((surface !is Surface) && (surface !is SurfaceTexture))) {
            "invalid surface: $surface"
        }

        // Create a window surface, and attach it to the Surface we received.
        val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
        val eglSurface = EGL14.eglCreateWindowSurface(
            eglDisplay,
            eglConfig,
            surface,
            surfaceAttribs,
            0
        )
        checkEglError("eglCreateWindowSurface")
        checkNotNull(eglSurface) { "surface was null" }
        return eglSurface
    }

    /**
     * Creates an EGL surface associated with an offscreen buffer.
     */
    fun createOffscreenSurface(width: Int, height: Int): EGLSurface {
        val surfaceAttribs = intArrayOf(
            EGL14.EGL_WIDTH, width,
            EGL14.EGL_HEIGHT, height,
            EGL14.EGL_NONE
        )
        val eglSurface = EGL14.eglCreatePbufferSurface(
            eglDisplay, eglConfig,
            surfaceAttribs, 0
        )
        checkEglError("eglCreatePbufferSurface")
        checkNotNull(eglSurface) { "surface was null" }
        return eglSurface
    }

    /**
     * Makes our EGL context current, using the supplied surface for both "draw" and "read".
     */
    fun makeCurrent(eglSurface: EGLSurface) {
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
            // called makeCurrent() before create?
            Log.d(TAG, "NOTE: makeCurrent w/o display")
        }
        check(EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            "eglMakeCurrent failed"
        }
    }

    /**
     * Makes our EGL context current, using the supplied "draw" and "read" surfaces.
     */
    fun makeCurrent(drawSurface: EGLSurface, readSurface: EGLSurface) {
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
            // called makeCurrent() before create?
            Log.d(TAG, "NOTE: makeCurrent w/o display")
        }
        check(EGL14.eglMakeCurrent(eglDisplay, drawSurface, readSurface, eglContext)) {
            "eglMakeCurrent(draw,read) failed"
        }
    }

    /**
     * Makes no context current.
     */
    fun makeNothingCurrent() {
        check(
            EGL14.eglMakeCurrent(
                eglDisplay,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT
            )
        ) {
            "eglMakeCurrent failed"
        }
    }

    /**
     * Calls eglSwapBuffers.  Use this to "publish" the current frame.
     *
     * @return false on failure
     */
    fun swapBuffers(eglSurface: EGLSurface): Boolean {
        return EGL14.eglSwapBuffers(eglDisplay, eglSurface)
    }

    /**
     * Sends the presentation time stamp to EGL.  Time is expressed in nanoseconds.
     */
    fun setPresentationTime(eglSurface: EGLSurface, nsecs: Long) {
        EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, nsecs)
    }

    /**
     * Returns true if our context and the specified surface are current.
     */
    fun isCurrent(eglSurface: EGLSurface): Boolean {
        return (eglContext == EGL14.eglGetCurrentContext())
                && (eglSurface == EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW))
    }

    /**
     * Performs a simple surface query.
     */
    fun querySurface(eglSurface: EGLSurface, what: Int): Int {
        val value = IntArray(1)
        EGL14.eglQuerySurface(eglDisplay, eglSurface, what, value, 0)
        return value[0]
    }

    /**
     * Queries a string value.
     */
    fun queryString(what: Int): String {
        return EGL14.eglQueryString(eglDisplay, what)
    }

    /**
     * Returns the GLES version this context is configured for (currently 2 or 3).
     */
    fun getGlVersion(): Int {
        return glVersion
    }

    @Throws(Throwable::class)
    protected fun finalize() {
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
            // We're limited here -- finalizers don't run on the thread that holds
            // the EGL state, so if a surface or context is still current on another
            // thread we can't fully release it here.  Exceptions thrown from here
            // are quietly discarded.  Complain in the log file.
            Log.w(TAG, "WARNING: EglCore was not explicitly released -- state may be leaked")
            release()
        }
    }

    /**
     * Checks for EGL errors.  Throws an exception if an error has been raised.
     */
    private fun checkEglError(msg: String) {
        val error = EGL14.eglGetError()
        check(error == EGL14.EGL_SUCCESS) {
            msg + ": EGL error: 0x" + Integer.toHexString(error)
        }
    }
}