package com.github.gtbluesky.camera.render

import android.content.Context
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.SurfaceHolder

class PreviewRenderer(private val context: Context) {
    private var renderThread: HandlerThread
    private var renderHandler: Handler

    init {
        renderThread = HandlerThread("Java Render Thread")
            .apply {
                start()
                renderHandler = RenderHandler(context, looper)
            }
    }

    fun bindSurface(surface: Surface) {
        renderHandler.apply {
            sendMessage(obtainMessage(
                RenderHandler.MSG_SURFACE_CREATED,
                surface
            ))
        }
    }

    fun bindSurface(surfaceHolder: SurfaceHolder) {
        renderHandler.apply {
            sendMessage(obtainMessage(
                RenderHandler.MSG_SURFACE_CREATED,
                surfaceHolder
            ))
        }
    }

    fun bindSurface(surfaceTexture: SurfaceTexture) {
        renderHandler.apply {
            sendMessage(obtainMessage(
                RenderHandler.MSG_SURFACE_CREATED,
                surfaceTexture
            ))
        }
    }

    fun unBindSurface() {
        renderHandler.apply {
            sendMessage(obtainMessage(
                RenderHandler.MSG_SURFACE_DESTROYED
            ))
        }
    }

    fun changePreviewSize() {
        renderHandler.apply {
            sendMessage(obtainMessage(
                RenderHandler.MSG_SURFACE_CHANGED
            ))
        }
    }

    fun switchCamera() {
        renderHandler.apply {
            sendMessage(obtainMessage(
                RenderHandler.MSG_SWITCH_CAMERA
            ))
        }
    }

    fun startRecording(filePath: String) {
        renderHandler.apply {
            sendMessage(obtainMessage(
                RenderHandler.MSG_START_RECORDING,
                filePath
            ))
        }
    }

    fun stopRecording() {
        renderHandler.apply {
            sendMessage(obtainMessage(
                RenderHandler.MSG_STOP_RECORDING
            ))
        }
    }

    fun takePicture(filePath: String) {
        renderHandler.apply {
            sendMessage(obtainMessage(
                RenderHandler.MSG_TAKE_PICTURE,
                filePath
            ))
        }
    }

    fun toggleTorch(turnOn: Boolean) {
        renderHandler.apply {
            sendMessage(obtainMessage(
                RenderHandler.MSG_TOGGLE_TORCH,
                turnOn
            ))
        }
    }
}