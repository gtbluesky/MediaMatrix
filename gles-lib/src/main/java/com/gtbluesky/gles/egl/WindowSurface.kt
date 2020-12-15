package com.gtbluesky.gles.egl

import android.graphics.SurfaceTexture
import android.view.Surface
import java.lang.RuntimeException


/**
 * Recordable EGL window surface.
 * It's good practice to explicitly release() the surface, preferably from a "finally" block.
 */
class WindowSurface : EglSurfaceBase {

    private var surface: Surface? = null
    private var releaseSurface = false

    /**
     * Associates an EGL surface with the native window surface.
     *
     * Set releaseSurface to true if you want the Surface to be released when release() is
     * called.  This is convenient, but can interfere with framework classes that expect to
     * manage the Surface themselves (e.g. if you release a SurfaceView's Surface, the
     * surfaceDestroyed() callback won't fire).
     */
    constructor(eglCore: EglCore, surface: Surface, releaseSurface: Boolean) : super(eglCore) {
        createWindowSurface(surface)
        this.surface = surface
        this.releaseSurface = releaseSurface
    }

    /**
     * Associates an EGL surface with the SurfaceTexture.
     */
    constructor(eglCore: EglCore, surfaceTexture: SurfaceTexture) : super(eglCore) {
        createWindowSurface(surfaceTexture)
    }

    /**
     * Releases any resources associated with the EGL surface (and, if configured to do so,
     * with the Surface as well).
     *
     * Does not require that the surface's EGL context be current.
     */
    fun release() {
        releaseEglSurface()
        surface?.apply {
            if (releaseSurface) {
                release()
            }
        }
        surface = null
    }

    /**
     * Recreate the EGLSurface, using the new EglBase.  The caller should have already
     * freed the old EGLSurface with releaseEglSurface().
     *
     * This is useful when we want to update the EGLSurface associated with a Surface.
     * For example, if we want to share with a different EGLContext, which can only
     * be done by tearing down and recreating the context.  (That's handled by the caller;
     * this just creates a new EGLSurface for the Surface we were handed earlier.)
     *
     * If the previous EGLSurface isn't fully destroyed, e.g. it's still current on a
     * context somewhere, the create call will fail with complaints from the Surface
     * about already being connected.
     */
    fun recreate(newEglCore: EglCore) {
        checkNotNull(surface) {
            "not yet implemented for SurfaceTexture"
        }
        eglCore = newEglCore          // switch to new context
        surface?.let {
            createWindowSurface(it)  // create new surface
        }
    }
}