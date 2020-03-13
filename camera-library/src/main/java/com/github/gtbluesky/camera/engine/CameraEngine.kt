package com.github.gtbluesky.camera.engine

import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.util.Log
import android.view.Surface
import java.util.*
import kotlin.Comparator
import kotlin.math.abs
import kotlin.math.sign

class CameraEngine private constructor() {

    private var camera: Camera? = null
    private lateinit var frontCameraInfo: Camera.CameraInfo
    private lateinit var backCameraInfo: Camera.CameraInfo
    private var frontParams: Camera.Parameters? = null
    private var backParams: Camera.Parameters? = null
    private val cameraParam = CameraParam.getInstance()

    init {
        val cameraNum = Camera.getNumberOfCameras()
        for (cameraId in 0 until cameraNum) {
            val cameraInfo = Camera.CameraInfo()
            Camera.getCameraInfo(cameraId, cameraInfo)
            when(cameraInfo.facing) {
                CameraParam.FRONT_CAMERA_ID -> frontCameraInfo = cameraInfo
                CameraParam.BACK_CAMERA_ID -> backCameraInfo = cameraInfo
            }
        }
    }

    companion object {
        private val TAG = CameraEngine::class.java.simpleName

        fun getInstance() = CameraEngineHolder.holder
    }

    private fun openCamera() {
        camera = Camera.open(cameraParam.cameraId)
        val cameraParams = getCameraParams()
        val previewSize = cameraParam.setPreviewSize(
            cameraParams.supportedPreviewSizes
        )
        val pictureSize = CameraParam.getOptimalSize(
            cameraParams.supportedPictureSizes,
            cameraParam.expectWidth,
            cameraParam.expectHeight
        )
        cameraParams.apply {
            setPreviewSize(previewSize.width, previewSize.height)
            setPictureSize(pictureSize.width, pictureSize.height)
            setRecordingHint(true)
        }
        setAutoFocus()
    }

    private fun closeCamera() {
        camera?.apply {
            stopPreview()
            setPreviewCallback(null)
            release()
        }
        camera = null
    }

    private fun resetCamera() {
        frontParams = null
        backParams = null
        cameraParam.reset()
    }

    fun startPreview(
        context: Context,
        surfaceTexture: SurfaceTexture
    ) {
        openCamera()
        camera?.apply {
            setPreviewTexture(surfaceTexture)
            setDisplayOrientation(getCameraDisplayOrientation(context))
            startPreview()
        }
    }

    fun stopPreview() {
        camera?.stopPreview()
    }

    fun destroy() {
        closeCamera()
        resetCamera()
    }

    fun setAutoFocus(point: Point = Point(cameraParam.viewWidth / 2, cameraParam.viewHeight / 2), areaSize: Int = 100) {
        val cameraParams = getCameraParams()
        if (Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO !in cameraParams.supportedFocusModes) {
            Log.e(TAG, "FOCUS_MODE_AUTO isn't supported")
            camera?.parameters = cameraParams
            return
        }
        cameraParams.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
        val supportCustomFocus = cameraParams.maxNumFocusAreas > 0
        val supportMetering = cameraParams.maxNumMeteringAreas > 0
        val left = clamp(point.x * 2000 / cameraParam.viewWidth - 1000 - areaSize / 2)
        val top = clamp(point.y * 2000 / cameraParam.viewHeight - 1000 - areaSize / 2)
        val right = clamp(point.x * 2000 / cameraParam.viewWidth - 1000 + areaSize / 2)
        val bottom = clamp(point.y * 2000 / cameraParam.viewHeight - 1000 + areaSize / 2)
        val areas = arrayListOf(Camera.Area(Rect(left, top, right, bottom), 1000))
        if (supportCustomFocus) {
            cameraParams.focusAreas = areas
        }
        if (supportMetering) {
            cameraParams.meteringAreas = areas
        }
        camera?.apply {
            cancelAutoFocus()
            parameters = cameraParams
        }
        cameraParam.cameraAutoFocusCallback?.also { camera?.autoFocus(it) }
    }

    private fun clamp(value: Int, min: Int = -1000, max: Int = 1000): Int {
        return when {
            value < min -> min
            value > max -> max
            else -> value
        }
    }

    fun changeZoom(scale: Float) {

    }

    fun switchCamera(context: Context, surfaceTexture: SurfaceTexture) {
        cameraParam.cameraId = when(cameraParam.cameraId) {
            CameraParam.FRONT_CAMERA_ID -> CameraParam.BACK_CAMERA_ID
            else -> CameraParam.FRONT_CAMERA_ID
        }
        closeCamera()
        startPreview(context, surfaceTexture)
    }

    fun toggleTorch(toggle: Boolean) {
        val cameraParams = getCameraParams()
        if (!isFlashSupported()) {
            return
        }
        cameraParams.flashMode =
            if (toggle) Camera.Parameters.FLASH_MODE_TORCH
            else Camera.Parameters.FLASH_MODE_OFF
        camera?.parameters = cameraParams

    }

    fun changeResolution(
        context: Context,
        resolutionType: ResolutionType,
        aspectRatioType: AspectRatioType,
        surfaceTexture: SurfaceTexture
    ) {
        cameraParam.setResolution(
            getCameraParams().supportedPreviewSizes,
            resolutionType,
            aspectRatioType
        )
        closeCamera()
        startPreview(context, surfaceTexture)
    }

    fun isFlashSupported(): Boolean {
        val cameraParams = getCameraParams()
        val supportedFlashModes = cameraParams.supportedFlashModes
        return (supportedFlashModes != null
                && supportedFlashModes.isNotEmpty()
                && Camera.Parameters.FLASH_MODE_TORCH in supportedFlashModes)
    }

//    fun changeZoom(scale: Float) {
//        val cameraParams = getCameraParams()
//        if (cameraParams.isZoomSupported) {
//            cameraParams.zoom =
//        } else {
//            Log.e(TAG, "This device doesn't support zoom")
//        }
//    }

    private fun getCameraDisplayOrientation(context: Context): Int {
        val degrees = when((context as Activity).windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
        val cameraInfo = getCameraInfo()
        val orientation = cameraInfo.orientation
        return when (cameraInfo.facing) {
            CameraParam.FRONT_CAMERA_ID -> {
                (360 - (orientation + degrees) % 360) % 360
            }
            else -> (orientation - degrees + 360) % 360
        }
    }

    private fun getCameraParams(): Camera.Parameters {
        return when(cameraParam.cameraId) {
            CameraParam.FRONT_CAMERA_ID -> {
                if (frontParams == null) {
                    frontParams = camera?.parameters
                }
                frontParams!!
            }
            else -> {
                if (backParams == null) {
                    backParams = camera?.parameters
                }
                backParams!!
            }
        }
    }

    private fun getCameraInfo(): Camera.CameraInfo {
        return when(cameraParam.cameraId) {
            CameraParam.FRONT_CAMERA_ID -> frontCameraInfo
            else -> backCameraInfo
        }
    }

    private object CameraEngineHolder {
        val holder = CameraEngine()
    }

}