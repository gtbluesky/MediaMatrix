package com.github.gtbluesky.gles

import android.view.Surface

class NativeGL {

    companion object {
        init {
            System.loadLibrary("native_gl")
        }

        @JvmStatic
        external fun nativeInit()

        @JvmStatic
        external fun nativeRelease()

        @JvmStatic
        external fun nativeSurfaceCreated(surface: Surface)

        @JvmStatic
        external fun nativeSurfaceChanged(width: Int, heigt: Int)

        @JvmStatic
        external fun nativeSurfaceDestroyed()

        @JvmStatic
        external fun nativeRequestRender()
    }


}