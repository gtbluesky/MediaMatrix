package com.github.gtbluesky.gles.render

import android.view.Surface
import com.github.gtbluesky.gles.egl.EglCore
import com.github.gtbluesky.gles.egl.WindowSurface
import com.github.gtbluesky.gles.shape.BaseShape
import com.github.gtbluesky.gles.shape.Cube
import com.github.gtbluesky.gles.shape.Sphere

class GLRenderer {

    private var eglCore: EglCore? = null
    private var windowSurface: WindowSurface? = null
    private var shape: BaseShape? = null

    fun init(surface: Surface, shape: BaseShape = Cube()) {
        eglCore = eglCore ?: EglCore(
            null,
            EglCore.FLAG_RECORDABLE
        )
        windowSurface = WindowSurface(eglCore!!, surface, false)
        windowSurface?.makeCurrent()
        this.shape = shape
        this.shape?.init()
    }

    fun change(width: Int, height: Int) {
        windowSurface?.makeCurrent()
        shape?.change(width, height)
    }

    fun drawFrame() {
        shape?.drawFrame()
        windowSurface?.swapBuffers()
    }

    fun destroy() {
        shape?.destroy()
        windowSurface?.release()
        eglCore?.release()
        shape = null
        windowSurface = null
        eglCore = null
    }
}