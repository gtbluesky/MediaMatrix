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
import com.github.gtbluesky.camera.engine.CameraEngine
import com.github.gtbluesky.camera.engine.CameraParam
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
    private var renderManager: RenderManager? = RenderManager()
    private val transformMatrix = FloatArray(16)
    private var isRecording = false
    private val encoder = HwEncoder()

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
        // 结束录制，需要释放资源
        const val MSG_FINISH_RECORDING = 0x07
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
    }

    override fun handleMessage(msg: Message) {
        when (msg.what) {
            MSG_SURFACE_CREATED -> {
                when (msg.obj) {
                    is SurfaceHolder -> {
                        surfaceCreated((msg.obj as SurfaceHolder).surface)
                    }
                    is Surface -> {
                        surfaceCreated(msg.obj as Surface)
                    }
                    is SurfaceTexture -> {
                        surfaceCreated(msg.obj as SurfaceTexture)
                    }
                }
            }
            MSG_SURFACE_CHANGED -> {
                surfaceChanged()
            }
            MSG_SURFACE_DESTROYED -> {
                surfaceDestroyed()
            }
            MSG_RENDER -> {
                drawFrame()
                if (isRecording) {
                    encoder.onFrameAvailable()
                }
            }
            MSG_SWITCH_CAMERA -> {
                switchCamera()
            }
            MSG_START_RECORDING -> {
                startRecording()
            }
            MSG_STOP_RECORDING -> {
                stopRecording()
            }
            MSG_FINISH_RECORDING -> {
                finishRecording()
            }
            MSG_TAKE_PICTURE -> {
                (msg.obj as? String)?.let {
                    takePicture(it)
                }
            }
            MSG_TOGGLE_TORCH -> {
                (msg.obj as? Boolean)?.let {
                    CameraEngine.getInstance().toggleTorch(it)
                }
            }
        }
    }

    private fun surfaceCreated(surface: Surface) {
        eglCore = EglCore(flags = EglCore.FLAG_RECORDABLE)
        windowSurface = WindowSurface(eglCore!!, surface, false)
        windowSurface?.makeCurrent()

        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        GLES30.glDisable(GLES30.GL_CULL_FACE)

        renderManager?.init()

        textureId = GLHelper.createOESTexture()
        surfaceTexture = SurfaceTexture(textureId).also {
            it.setOnFrameAvailableListener(this)
            CameraEngine.getInstance().startPreview(context, it)
        }

    }

    private fun surfaceCreated(surfaceTexture: SurfaceTexture) {
        eglCore = EglCore(flags = EglCore.FLAG_RECORDABLE)
        windowSurface = WindowSurface(eglCore!!, surfaceTexture)
        windowSurface?.makeCurrent()

        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        GLES30.glDisable(GLES30.GL_CULL_FACE)

        renderManager?.init()

        textureId = GLHelper.createOESTexture()
        this.surfaceTexture = SurfaceTexture(textureId).also {
            it.setOnFrameAvailableListener(this)
            CameraEngine.getInstance().startPreview(context, it)
        }
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        sendMessage(obtainMessage(MSG_RENDER))
    }

    private fun surfaceChanged() {
        windowSurface?.makeCurrent()
        renderManager?.setDisplaySize(
            CameraParam.getInstance().viewWidth,
            CameraParam.getInstance().viewHeight
        )
    }

    private fun surfaceDestroyed() {
        windowSurface?.makeCurrent()
        renderManager?.release()
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
    }

    private fun drawFrame() {
        if (surfaceTexture == null || windowSurface == null) {
            return
        }
        windowSurface?.makeCurrent()
        surfaceTexture?.updateTexImage()
        surfaceTexture?.getTransformMatrix(transformMatrix)
        renderManager?.drawFrame(textureId, transformMatrix)
        windowSurface?.swapBuffers()
    }

    private fun switchCamera() {
        surfaceTexture?.let {
            CameraEngine.getInstance().switchCamera(context, it)
        }
    }

    private fun startRecording() {
        eglCore?.let {
            encoder.start(720, 1080, it.eglContext)
        }
        isRecording = true
    }

    private fun stopRecording() {
        encoder.stop()
        isRecording = false
    }

    private fun finishRecording() {
        if (!isRecording) {
            encoder.finish()
        }
    }

    private fun takePicture(filePath: String) {
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
                    getHeight()
                )
            }
            Log.d(TAG, "照片保存在：$filePath")
        }
    }

}