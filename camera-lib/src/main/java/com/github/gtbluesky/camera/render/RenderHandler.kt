package com.github.gtbluesky.camera.render

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES30
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import com.github.gtbluesky.camera.AspectRatioType
import com.github.gtbluesky.camera.engine.CameraEngine
import com.github.gtbluesky.camera.CameraParam
import com.github.gtbluesky.camera.ResolutionType
import com.github.gtbluesky.codec.CodecParam
import com.github.gtbluesky.codec.HwEncoder
import com.github.gtbluesky.gles.egl.EglCore
import com.github.gtbluesky.gles.egl.WindowSurface
import com.github.gtbluesky.gles.util.BitmapUtil
import com.github.gtbluesky.gles.util.GLHelper

class RenderHandler(private val context: Context, looper: Looper) :
    Handler(looper), SurfaceTexture.OnFrameAvailableListener {

    private var eglCore: EglCore? = null
    private var windowSurface: WindowSurface? = null
    private var surfaceTexture: SurfaceTexture? = null
    private var textureId = GLES30.GL_NONE
    private val renderManager: RenderManager by lazy {
        RenderManager(context)
    }
    private val transformMatrix = FloatArray(16)
    private var isRecording = false
    private var encoder: HwEncoder? = null

    companion object {
        private val TAG = RenderHandler::class.java.simpleName
        // Surface创建
        const val MSG_SURFACE_CREATED = 0x01
        // Surface改变
        const val MSG_SURFACE_CHANGED = 0x02
        // Surface销毁
        const val MSG_SURFACE_DESTROYED = 0x03
        // 渲染
        const val MSG_RENDER = 0x04
        // 开始录制
        const val MSG_START_RECORDING = 0x05
        // 停止录制
        const val MSG_STOP_RECORDING = 0x06
        // 切换相机
        const val MSG_SWITCH_CAMERA = 0x08
        // 预览帧回调
        const val MSG_PREVIEW_CALLBACK = 0x09
        // 拍照
        const val MSG_TAKE_PICTURE = 0x0a
        // 计算fps
        const val MSG_CALCULATE_FPS = 0x0b
        // 切换边框模糊功能
        const val MSG_CHANGE_EDGE_BLUR = 0x0c
        // 切换动态滤镜
        const val MSG_CHANGE_DYNAMIC_COLOR = 0x0d
        // 切换动态彩妆
        const val MSG_CHANGE_DYNAMIC_MAKEUP = 0x0e
        // 切换动态动态资源
        const val MSG_CHANGE_DYNAMIC_RESOURCE = 0x0f
        // 开关闪关灯
        const val MSG_TOGGLE_TORCH = 0x10
        // 调整分辨率和画幅
        const val MSG_CHANGE_RESOLUTION = 0x11
    }

    override fun handleMessage(msg: Message) {
        when (msg.what) {
            MSG_SURFACE_CREATED -> {
                when (msg.obj) {
                    is SurfaceHolder -> {
                        handleSurfaceCreated((msg.obj as SurfaceHolder).surface)
                    }
                    is Surface -> {
                        handleSurfaceCreated(msg.obj as Surface)
                    }
                    is SurfaceTexture -> {
                        handleSurfaceCreated(msg.obj as SurfaceTexture)
                    }
                }
            }
            MSG_SURFACE_CHANGED -> {
                handleSurfaceChanged()
            }
            MSG_SURFACE_DESTROYED -> {
                handleSurfaceDestroyed()
            }
            MSG_RENDER -> {
                handleDrawFrame()
            }
            MSG_SWITCH_CAMERA -> {
                handleSwitchCamera()
            }
            MSG_START_RECORDING -> {
                (msg.obj as? String)?.let {
                    handleStartRecording(it, msg.arg1)
                }
            }
            MSG_STOP_RECORDING -> {
                handleStopRecording()
            }
            MSG_TAKE_PICTURE -> {
                (msg.obj as? String)?.let {
                    handleTakePicture(it, msg.arg1)
                }
            }
            MSG_TOGGLE_TORCH -> {
                (msg.obj as? Boolean)?.let {
                    handleToggleTorch(it)
                }
            }
            MSG_CHANGE_RESOLUTION -> {
                handleChangeResolution(
                    enumValues<ResolutionType>()[msg.arg1],
                    enumValues<AspectRatioType>()[msg.arg2]
                )
            }
        }
    }

    private fun handleSurfaceCreated(surface: Surface) {
        eglCore = EglCore(flags = EglCore.FLAG_RECORDABLE)
        windowSurface = WindowSurface(eglCore!!, surface, false)
        windowSurface?.makeCurrent()

        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        GLES30.glDisable(GLES30.GL_CULL_FACE)

        textureId = GLHelper.createOESTexture()
        surfaceTexture = SurfaceTexture(textureId).also {
            it.setOnFrameAvailableListener(this)
            CameraEngine.getInstance().startPreview(context, it)
        }

    }

    private fun handleSurfaceCreated(surfaceTexture: SurfaceTexture) {
        eglCore = EglCore(flags = EglCore.FLAG_RECORDABLE)
        windowSurface = WindowSurface(eglCore!!, surfaceTexture)
        windowSurface?.makeCurrent()

        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        GLES30.glDisable(GLES30.GL_CULL_FACE)

        textureId = GLHelper.createOESTexture()
        this.surfaceTexture = SurfaceTexture(textureId).also {
            it.setOnFrameAvailableListener(this)
            CameraEngine.getInstance().startPreview(context, it)
        }
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        sendMessage(obtainMessage(MSG_RENDER))
    }

    private fun handleSurfaceChanged() {
        windowSurface?.makeCurrent()
        renderManager.apply {
            setViewSize(
                CameraParam.getInstance().viewWidth,
                CameraParam.getInstance().viewHeight
            )
            setTextureSize(
                CameraParam.getInstance().previewWidth,
                CameraParam.getInstance().previewHeight
            )
        }
    }

    private fun handleSurfaceDestroyed() {
        windowSurface?.makeCurrent()
        renderManager.release()
        CameraEngine.getInstance().let {
            it.stopPreview()
            it.destroy()
        }
        surfaceTexture?.release()
        surfaceTexture = null
        windowSurface?.release()
        windowSurface = null
        eglCore?.release()
        eglCore = null
        CodecParam.getInstance().reset()
    }

    private fun handleDrawFrame() {
        surfaceTexture?.let {
            windowSurface?.makeCurrent()
            it.updateTexImage()
            it.getTransformMatrix(transformMatrix)
            val outputTextureId = renderManager.drawFrame(textureId, transformMatrix)
            windowSurface?.swapBuffers()
            if (isRecording) {
                encoder?.onFrameAvailable(outputTextureId, it.timestamp)
            }
        }
    }

    private fun handleSwitchCamera() {
        surfaceTexture?.let {
            CameraEngine.getInstance().switchCamera(context, it)
        }
    }

    private fun handleStartRecording(
        filePath: String,
        rotation: Int
    ) {
        eglCore?.let {
            if (encoder == null) {
                encoder = HwEncoder(rotation)
            }
            encoder?.start(
                it.eglContext,
                filePath
            )
            isRecording = true
        }
    }

    private fun handleStopRecording() {
        isRecording = false
        encoder?.stop()
        encoder = null
    }

    private fun handleTakePicture(
        filePath: String,
        rotation: Int
    ) {
        windowSurface?.apply {
            val buffer = GLHelper.getCurrentFrame(
                getWidth(),
                getHeight()
            )
            CameraParam.getInstance().let {
                BitmapUtil.saveBitmap(
                    filePath,
                    buffer,
                    it.previewWidth.toFloat() / getWidth(),
                    getWidth(),
                    getHeight(),
                    rotation
                )
            }
            Log.d(TAG, "照片保存在：$filePath")
        }
    }

    private fun handleToggleTorch(toggle: Boolean) {
        CameraEngine.getInstance().toggleTorch(toggle)
    }

    private fun handleChangeResolution(
        resolutionType: ResolutionType,
        aspectRatioType: AspectRatioType
    ) {
        surfaceTexture?.let {
            CameraEngine.getInstance().changeResolution(
                context,
                resolutionType,
                aspectRatioType,
                it
            )
        }
    }

}