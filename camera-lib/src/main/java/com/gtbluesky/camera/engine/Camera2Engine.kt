package com.gtbluesky.camera.engine

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.*
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.core.content.ContextCompat
import com.gtbluesky.gles.util.GLHelper
import java.util.*
import kotlin.Comparator
import kotlin.math.abs

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class Camera2Engine private constructor() {

    private var cameraManager: CameraManager? = null
    private var imageReader: ImageReader? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var cameraDevice: CameraDevice? = null
    var surfaceTexture: SurfaceTexture? = null
    private var width = 0
    private var height = 0
    private lateinit var cameraId: String
    private lateinit var frontCameraId: String
    private lateinit var backCameraId: String
    private lateinit var frontCharacteristics: CameraCharacteristics
    private lateinit var backCharacteristics: CameraCharacteristics
    private lateinit var previewSize: Size
    private lateinit var pictureSize: Size
    private var cameraThread: HandlerThread? = null
    private var cameraHandler: Handler? = null
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private val sizeComparator = Comparator<Size> { o1, o2 ->
        when {
            o1.height == o2.height -> 0
            o1.height > o2.height -> 1
            else -> -1
        }
    }
    private val captureCallBack = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            super.onCaptureCompleted(session, request, result)
            Log.d(TAG, "onCaptureCompleted")
        }

        override fun onCaptureFailed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            failure: CaptureFailure
        ) {
            super.onCaptureFailed(session, request, failure)
            Log.e(TAG, "onCaptureFailed")
        }
    }

    fun checkHardwareLevel(): Boolean {
        val sortedLevels = intArrayOf(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
        val backSupportLevel = backCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
        val frontSupportLevel = frontCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
        return (frontSupportLevel!! !in sortedLevels && backSupportLevel!! !in sortedLevels)
    }

    companion object {

        private val TAG = Camera2Engine::class.java.simpleName

        fun getInstance() = Camera2EngineHolder.holder
    }

    private fun openCamera(context: Context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        if (cameraManager == null) {
            cameraManager = context.applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            cameraManager?.also {
                val cameraIdList = it.cameraIdList
                for (id in cameraIdList) {
                    val cameraCharacteristics = it.getCameraCharacteristics(id)
                    when(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)) {
                        CameraCharacteristics.LENS_FACING_FRONT -> {
                            frontCameraId = id
                            frontCharacteristics = cameraCharacteristics
                        }
                        else -> {
                            backCameraId = id
                            cameraId = id
                            backCharacteristics = cameraCharacteristics
                        }
                    }
                }
                cameraThread = HandlerThread("Camera2 Thread").also { thread ->
                    thread.start()
                    cameraHandler = Handler(thread.looper)
                }
            }
        }
        cameraHandler?.post {
            cameraManager?.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    createCaptureSession(camera)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e(TAG, "Open camera2 error!")
                }

            }, cameraHandler)
        }
    }

    private fun closeCamera() {
        cameraCaptureSession?.close()
        cameraCaptureSession = null
        cameraDevice?.close()
        cameraDevice = null
        imageReader?.close()
        imageReader = null
    }

    private fun createCaptureSession(cameraDevice: CameraDevice) {
        val surface = Surface(surfaceTexture)
        imageReader = ImageReader.newInstance(720, 1280, ImageFormat.JPEG, 1)
        cameraDevice.createCaptureSession(arrayListOf(surface, imageReader?.surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigureFailed(session: CameraCaptureSession) {
                Log.e(TAG, "Create captureSession error!")
            }

            override fun onConfigured(session: CameraCaptureSession) {
                cameraCaptureSession = session
                captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                    addTarget(surface)
                    set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
                    session.setRepeatingRequest(build(), captureCallBack, cameraHandler)
                }
                setAutoFocus()
            }

        }, cameraHandler)
    }

    fun startPreview(context: Context, width: Int, height: Int) {
        if (surfaceTexture == null) {
            val textureId = GLHelper.createOESTexture()
            surfaceTexture = SurfaceTexture(textureId)
        }
        this.width = width
        this.height = height
        surfaceTexture?.setDefaultBufferSize(720, 1280)
//        val matrix = FloatArray(16)
//        Matrix.setIdentityM(matrix, 0)
//        getCameraDisplayOrientation(context)
//        Matrix.translateM(matrix, 0, 0.5f, 0.5f, 0f)
//        Matrix.rotateM(matrix, 0, -getCameraDisplayOrientation().toFloat(), 0f, 0f, 1f)
//        Matrix.translateM(matrix, 0, -0.5f, -0.5f, 0f)
//        mCameraRenderer.transformMatrix = matrix
        openCamera(context)
    }

    fun stopPreview() {
        cameraCaptureSession?.stopRepeating()
    }

    fun destroy() {
        closeCamera()
        cameraThread?.apply {
            quitSafely()
            join()
        }
        cameraThread = null
        cameraHandler = null
    }

    fun setAutoFocus(point: Point = Point(width / 2, height / 2), areaSize: Int = 100) {
        val cameraCharacteristics = getCameraCharacteristics()
        val supportCustomFocus = cameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF) ?: 0 > 0
        val supportMetering = cameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE) ?: 0 > 0
        val arraySize = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
        Log.e(TAG, "supportCustomFocus: $supportCustomFocus , supportMetering: $supportMetering, arraySize: ${arraySize?.width} * ${arraySize?.height}")
        captureRequestBuilder.apply {
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
//            set(CaptureRequest.CONTROL_AE_REGIONS, )
//            set(CaptureRequest.CONTROL_AF_REGIONS, )
//            set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START)
//            set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START)
            cameraCaptureSession?.capture(build(), null, cameraHandler)
        }
    }

    fun switchCamera(context: Context) {
        cameraId = when(cameraId) {
            frontCameraId -> backCameraId
            else -> frontCameraId
        }
        closeCamera()
        startPreview(context, width, height)
    }

    fun toggleTorch(toggle: Boolean) {
        if (!isFlashSupported()) {
            return
        }
        captureRequestBuilder.apply {
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
            set(CaptureRequest.FLASH_MODE, if (toggle) CaptureRequest.FLASH_MODE_TORCH else CameraMetadata.FLASH_MODE_OFF)
            cameraCaptureSession?.setRepeatingRequest(build(), captureCallBack, cameraHandler)
        }

    }

    fun isFlashSupported() =
        getCameraCharacteristics().get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false

    private fun getCameraDisplayOrientation(context: Context): Int {
        val degrees = when((context as Activity).windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
        val cameraCharacteristics = getCameraCharacteristics()
        val sensorOrientation = cameraCharacteristics[CameraCharacteristics.SENSOR_ORIENTATION] ?: 0
        return when (cameraCharacteristics[CameraCharacteristics.LENS_FACING]) {
            CameraCharacteristics.LENS_FACING_FRONT -> (360 - (sensorOrientation + degrees) % 360) % 360
            else -> (sensorOrientation - degrees + 360) % 360
        }
    }

    private fun getCameraCharacteristics(): CameraCharacteristics {
        return when(cameraId) {
            frontCameraId -> frontCharacteristics
            else -> backCharacteristics
        }
    }

    private fun getOptimalSize(list: List<Size>, rate: Float, minWidth: Int): Size {
        Collections.sort(list, sizeComparator)
        var size = list[0]
        for (s in list) {
            Log.e(TAG, "height: ${s.height}, width: ${s.width}")
            if (equalRate(s, rate)) {
                if (s.height >= minWidth) {
                    return s
                }
                if (s.height > size.height) {
                    size = s
                }
            }
        }
        return size
    }

    private fun equalRate(size: Size, rate: Float): Boolean {
        return abs(size.width.toFloat() / size.height.toFloat() - rate) <= 0.03f
    }

    private object Camera2EngineHolder {
        val holder = Camera2Engine()
    }

}