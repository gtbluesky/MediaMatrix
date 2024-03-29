package com.gtbluesky.gles.egl

/**
 * Off-screen EGL surface (pbuffer).
 * It's good practice to explicitly release() the surface, preferably from a "finally" block.
 */
class OffscreenSurface(eglCore: EglCore, width: Int, height: Int) : EglSurfaceBase(eglCore) {

    init {
        /**
         * Creates an off-screen surface with the specified width and height.
         */
        createOffscreenSurface(width, height)
    }

    /**
     * Releases any resources associated with the surface.
     */
    fun release() {
        releaseEglSurface()
    }
}