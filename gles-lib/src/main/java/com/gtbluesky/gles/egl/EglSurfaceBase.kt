package com.gtbluesky.gles.egl

import android.graphics.Bitmap
import android.opengl.EGL14
import android.opengl.GLES30
import android.util.Log
import com.gtbluesky.gles.util.GLHelper
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Common base class for EGL surfaces.
 * There can be multiple surfaces associated with a single context.
 */
open class EglSurfaceBase(protected var eglCore: EglCore) {

    private var eglSurface = EGL14.EGL_NO_SURFACE
    private var width = -1
    private var height = -1

    companion object {
        private val TAG = EglSurfaceBase::class.java.simpleName
    }

    /**
     * Creates a window surface.
     *
     * @param surface May be a Surface or SurfaceTexture.
     */
    fun createWindowSurface(surface: Any) {
        check(eglSurface == EGL14.EGL_NO_SURFACE) {
            "surface already created"
        }
        eglSurface = eglCore.createWindowSurface(surface)

        // Don't cache width/height here, because the size of the underlying surface can change
        // out from under us.
        //width = eglCore.querySurface(eglSurface, EGL14.EGL_WIDTH);
        //height = eglCore.querySurface(eglSurface, EGL14.EGL_HEIGHT);
    }

    /**
     * Creates an off-screen surface.
     */
    fun createOffscreenSurface(width: Int, height: Int) {
        check(eglSurface == EGL14.EGL_NO_SURFACE) {
            "surface already created"
        }
        eglSurface = eglCore.createOffscreenSurface(width, height)
        this.width = width
        this.height = height
    }

    /**
     * Returns the surface's width, in pixels.
     *
     *
     * If this is called on a window surface, and the underlying surface is in the process
     * of changing size, we may not see the new size right away (e.g. in the "surfaceChanged"
     * callback).  The size should match after the next buffer swap.
     */
    fun getWidth(): Int {
        return if (width < 0) {
            eglCore.querySurface(eglSurface, EGL14.EGL_WIDTH)
        } else {
            width
        }
    }

    /**
     * Returns the surface's height, in pixels.
     */
    fun getHeight(): Int {
        return if (height < 0) {
            eglCore.querySurface(eglSurface, EGL14.EGL_HEIGHT)
        } else {
            height
        }
    }

    /**
     * Release the EGL surface.
     */
    fun releaseEglSurface() {
        eglCore.releaseSurface(eglSurface)
        eglSurface = EGL14.EGL_NO_SURFACE
        width = -1
        height = -1
    }

    /**
     * Makes our EGL context and surface current.
     */
    fun makeCurrent() {
        eglCore.makeCurrent(eglSurface)
    }

    /**
     * Makes our EGL context and surface current for drawing, using the supplied surface
     * for reading.
     */
    fun makeCurrentReadFrom(readSurface: EglSurfaceBase) {
        eglCore.makeCurrent(eglSurface, readSurface.eglSurface)
    }

    /**
     * Calls eglSwapBuffers.  Use this to "publish" the current frame.
     *
     * @return false on failure
     */
    fun swapBuffers(): Boolean {
        val result = eglCore.swapBuffers(eglSurface)
        if (!result) {
            Log.d(TAG, "WARNING: swapBuffers() failed")
        }
        return result
    }

    /**
     * Sends the presentation time stamp to EGL.
     *
     * @param nsecs Timestamp, in nanoseconds.
     */
    fun setPresentationTime(nsecs: Long) {
        eglCore.setPresentationTime(eglSurface, nsecs)
    }

    /**
     * Saves the EGL surface to a file.
     *
     *
     * Expects that this object's EGL surface is current.
     */
    @Throws(IOException::class)
    fun saveFrame(file: File) {
        check(eglCore.isCurrent(eglSurface)) {
            "Expected EGL context/surface is not current"
        }

        // glReadPixels fills in a "direct" ByteBuffer with what is essentially big-endian RGBA
        // data (i.e. a byte of red, followed by a byte of green...).  While the Bitmap
        // constructor that takes an int[] wants little-endian ARGB (blue/red swapped), the
        // Bitmap "copy pixels" method wants the same format GL provides.
        //
        // Ideally we'd have some way to re-use the ByteBuffer, especially if we're calling
        // here often.
        //
        // Making this even more interesting is the upside-down nature of GL, which means
        // our output will look upside down relative to what appears on screen if the
        // typical GL conventions are used.

        val filename = file.toString()

        val width = getWidth()
        val height = getHeight()
        val buf = ByteBuffer.allocateDirect(width * height * 4)
        buf.order(ByteOrder.LITTLE_ENDIAN)
        GLES30.glReadPixels(
            0, 0,
            width, height,
            GLES30.GL_RGBA,
            GLES30.GL_UNSIGNED_BYTE,
            buf
        )
        GLHelper.checkGlError("glReadPixels")
        buf.rewind()

        var bos: BufferedOutputStream? = null
        try {
            bos = BufferedOutputStream(FileOutputStream(filename))
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bmp.copyPixelsFromBuffer(buf)
            bmp.compress(Bitmap.CompressFormat.PNG, 90, bos)
            bmp.recycle()
        } finally {
            bos?.close()
        }
        Log.d(TAG, "Saved $width x $height frame as '$filename'")
    }
}