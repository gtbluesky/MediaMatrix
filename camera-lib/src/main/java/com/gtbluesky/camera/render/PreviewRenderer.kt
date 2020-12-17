package com.gtbluesky.camera.render

import android.content.Context
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.SurfaceHolder
import com.gtbluesky.camera.AspectRatioType
import com.gtbluesky.camera.ResolutionType
import com.gtbluesky.camera.entity.SnapInfoEntity

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
            sendMessage(
                obtainMessage(
                    RenderHandler.MSG_SURFACE_CREATED,
                    surface
                )
            )
        }
    }

    fun bindSurface(surfaceHolder: SurfaceHolder) {
        renderHandler.apply {
            sendMessage(
                obtainMessage(
                    RenderHandler.MSG_SURFACE_CREATED,
                    surfaceHolder
                )
            )
        }
    }

    fun bindSurface(surfaceTexture: SurfaceTexture) {
        renderHandler.apply {
            sendMessage(
                obtainMessage(
                    RenderHandler.MSG_SURFACE_CREATED,
                    surfaceTexture
                )
            )
        }
    }

    fun unBindSurface() {
        renderHandler.apply {
            sendMessage(
                obtainMessage(
                    RenderHandler.MSG_SURFACE_DESTROYED
                )
            )
        }
    }

    fun changePreviewSize() {
        renderHandler.apply {
            sendMessage(
                obtainMessage(
                    RenderHandler.MSG_SURFACE_CHANGED
                )
            )
        }
    }

    fun switchCamera() {
        renderHandler.apply {
            sendMessage(
                obtainMessage(
                    RenderHandler.MSG_SWITCH_CAMERA
                )
            )
        }
    }

    fun startRecording(filePath: String, rotation: Int) {
        renderHandler.apply {
            sendMessage(
                obtainMessage(
                    RenderHandler.MSG_START_RECORDING,
                    rotation,
                    rotation,
                    filePath
                )
            )
        }
    }

    fun stopRecording() {
        renderHandler.apply {
            sendMessage(
                obtainMessage(
                    RenderHandler.MSG_STOP_RECORDING
                )
            )
        }
    }

    fun takePicture(filePath: String, rotation: Int) {
        renderHandler.apply {
            sendMessage(
                obtainMessage(
                    RenderHandler.MSG_TAKE_PICTURE,
                    SnapInfoEntity(
                        filePath,
                        rotation
                    )
                )
            )
        }
    }

    fun takePicture(snapInfoEntity: SnapInfoEntity) {
        renderHandler.apply {
            sendMessage(
                obtainMessage(
                    RenderHandler.MSG_TAKE_PICTURE,
                    snapInfoEntity
                )
            )
        }
    }

    fun toggleTorch(turnOn: Boolean) {
        renderHandler.apply {
            sendMessage(
                obtainMessage(
                    RenderHandler.MSG_TOGGLE_TORCH,
                    turnOn
                )
            )
        }
    }

    fun changeResolution(
        resolutionType: ResolutionType,
        aspectRatioType: AspectRatioType
    ) {
        renderHandler.apply {
            sendMessage(
                obtainMessage(
                    RenderHandler.MSG_CHANGE_RESOLUTION,
                    resolutionType.ordinal,
                    aspectRatioType.ordinal
                )
            )
        }
    }

    fun setBeautyFilter(enable: Boolean) {
        renderHandler.apply {
            sendMessage(
                obtainMessage(
                    RenderHandler.MSG_USE_BEAUTY,
                    enable
                )
            )
        }
    }
}