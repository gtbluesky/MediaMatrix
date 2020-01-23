package com.github.gtbluesky.gles.render

import android.os.Handler
import android.os.HandlerThread
import android.view.Surface

class GLLooper {

    private var glRenderer = GLRenderer()

    private var glThread: HandlerThread? = HandlerThread("Android Java GLThread")

    private var glHandler: Handler? = null

    init {
        glThread?.apply {
            start()
            glHandler = Handler(looper)
        }

    }

    companion object {
        const val MSG_INIT = 0x01
        const val MSG_CHANGE = 0x02
        const val MSG_DRAW = 0x03
        const val MSG_DESTROY = 0x04
    }

    fun postMessage(what: Int, arg1: Int = 0, arg2: Int = 0, obj: Any? = null) {
        when (what) {
            MSG_INIT -> glHandler?.post {
                glRenderer.init(obj as Surface)
            }
            MSG_CHANGE -> glHandler?.post {
                glRenderer.change(arg1, arg2)
            }
            MSG_DRAW -> glHandler?.post {
                glRenderer.drawFrame()
            }
            MSG_DESTROY -> glHandler?.post {
                glRenderer.destroy()
            }
        }
    }

    fun release() {
        glThread?.apply {
            quitSafely()
            join()
        }
        glThread = null
        glHandler = null
    }

}

