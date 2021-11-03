package com.gtbluesky.camera.engine

import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.util.Log
import android.view.Surface
import com.gtbluesky.camera.AspectRatioType
import com.gtbluesky.camera.CameraParam
import com.gtbluesky.camera.ResolutionType

class CameraEngine private constructor() {
    private var camera: Camera? = null
    private lateinit var frontCameraInfo: Camera.CameraInfo
    private lateinit var backCameraInfo: Camera.CameraInfo
    private var frontParams: Camera.Parameters? = null
    private var backParams: Camera.Parameters? = null
    private val cameraParam = CameraParam.getInstance()
    private var isPreviewing = false

    init {
        val cameraNum = Camera.getNumberOfCameras()
        for (cameraId in 0 until cameraNum) {
            val cameraInfo = Camera.CameraInfo()
            Camera.getCameraInfo(cameraId, cameraInfo)
            when (cameraInfo.facing) {
                CameraParam.FRONT_CAMERA_ID -> frontCameraInfo = cameraInfo
                CameraParam.BACK_CAMERA_ID -> backCameraInfo = cameraInfo
            }
        }
    }

    companion object {
        private val TAG = CameraEngine::class.java.simpleName
        private const val ZOOM_RATIO_MIN = 100f
        fun getInstance() = CameraEngineHolder.holder
    }

    private fun openCamera() {
        camera = Camera.open(cameraParam.cameraId)
        getCameraParams()?.apply {
            val previewSize = cameraParam.setPreviewSize(
                supportedPreviewSizes
            )
            setPreviewSize(previewSize.width, previewSize.height)
            setRecordingHint(true)
        }
        isPreviewing = true
        setAutoFocus()
    }

    private fun closeCamera() {
        isPreviewing = false
        stopPreview()
        camera?.apply {
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
        try {
            camera?.apply {
                setPreviewTexture(surfaceTexture)
                setDisplayOrientation(getCameraDisplayOrientation(context))
                startPreview()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopPreview() {
        try {
            camera?.stopPreview()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun destroy() {
        closeCamera()
        resetCamera()
    }

    fun setAutoFocus(
        point: Point = Point(cameraParam.viewWidth / 2, cameraParam.viewHeight / 2),
        areaSize: Int = 100
    ) {
        if (!isPreviewing) {
            return
        }
        val cameraParams = getCameraParams()
        cameraParams?.supportedFocusModes?.let {
            if (Camera.Parameters.FOCUS_MODE_AUTO !in it) {
                Log.e(TAG, "FOCUS_MODE_AUTO isn't supported")
                try {
                    camera?.parameters = cameraParams
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return
            }
        }
        val fpsRange = IntArray(2)
        cameraParams?.getPreviewFpsRange(fpsRange)
        Log.e(TAG, "fps range ${fpsRange[0]} to ${fpsRange[1]}")
        if (fpsRange[1] > 30 * 1000) {
            cameraParams?.setPreviewFpsRange(30000, 30000)
        } else {
            cameraParams?.setPreviewFpsRange(fpsRange[1], fpsRange[1])
        }
        cameraParams?.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
        val supportCustomFocus = cameraParams?.maxNumFocusAreas?.let {
            it > 0
        } ?: false
        val supportMetering = cameraParams?.maxNumMeteringAreas?.let {
            it > 0
        } ?: false
        val areaHalf = areaSize / 2
        val left = clamp((point.x - areaHalf) * 2000 / cameraParam.viewWidth - 1000)
        val top = clamp((point.y - areaHalf) * 2000 / cameraParam.viewHeight - 1000)
        val right = clamp((point.x + areaHalf) * 2000 / cameraParam.viewWidth - 1000)
        val bottom = clamp((point.y + areaHalf) * 2000 / cameraParam.viewHeight - 1000)
        val areas = arrayListOf(Camera.Area(Rect(left, top, right, bottom), 1000))
        if (supportCustomFocus) {
            cameraParams?.focusAreas = areas
        }
        if (supportMetering) {
            cameraParams?.meteringAreas = areas
        }
        try {
            camera?.apply {
                cancelAutoFocus()
                parameters = cameraParams
                // 部分手机会出Exception
                autoFocus { success, _ ->
                    cameraParam.onCameraFocusListener?.onCameraFocus(success)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun clamp(value: Int, min: Int = -1000, max: Int = 1000): Int {
        return when {
            value < min -> min
            value > max -> max
            else -> value
        }
    }

    fun switchCamera(context: Context, surfaceTexture: SurfaceTexture) {
        cameraParam.cameraId = when (cameraParam.cameraId) {
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
        cameraParams?.flashMode =
            if (toggle) Camera.Parameters.FLASH_MODE_TORCH
            else Camera.Parameters.FLASH_MODE_OFF
        try {
            camera?.parameters = cameraParams
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun changeResolution(
        context: Context,
        resolutionType: ResolutionType,
        aspectRatioType: AspectRatioType,
        surfaceTexture: SurfaceTexture
    ) {
        getCameraParams()?.let {
            cameraParam.setResolution(
                it.supportedPreviewSizes,
                resolutionType,
                aspectRatioType
            )
            closeCamera()
            startPreview(context, surfaceTexture)
        }
    }

    private fun isFlashSupported(): Boolean {
        val cameraParams = getCameraParams()
        val supportedFlashModes = cameraParams?.supportedFlashModes
        return (supportedFlashModes != null
                && supportedFlashModes.isNotEmpty()
                && Camera.Parameters.FLASH_MODE_TORCH in supportedFlashModes
                && Camera.Parameters.FLASH_MODE_OFF in supportedFlashModes)
    }

    fun changeZoom(scale: Float): Float {
        val cameraParams = getCameraParams()
        var index = 0
        if (cameraParams?.isZoomSupported == true) {
            index = getZoomRatioIndex(scale)
            cameraParams.zoom = index
            try {
                camera?.parameters = cameraParams
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            Log.e(TAG, "This device doesn't support zoom")
        }
        return cameraParams?.let {
            it.zoomRatios[index] / ZOOM_RATIO_MIN
        } ?: 1f
    }

    private fun getCameraDisplayOrientation(context: Context): Int {
        val degrees = when ((context as Activity).windowManager.defaultDisplay.rotation) {
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

    private fun getCameraParams(): Camera.Parameters? {
        return try {
            when (cameraParam.cameraId) {
                CameraParam.FRONT_CAMERA_ID -> {
                    if (frontParams == null) {
                        frontParams = camera?.parameters
                    }
                    frontParams
                }
                else -> {
                    if (backParams == null) {
                        backParams = camera?.parameters
                    }
                    backParams
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getCameraInfo(): Camera.CameraInfo {
        return when (cameraParam.cameraId) {
            CameraParam.FRONT_CAMERA_ID -> frontCameraInfo
            else -> backCameraInfo
        }
    }

    private fun getZoomRatioIndex(scale: Float): Int {
        var index = 0
        getCameraParams()?.zoomRatios?.let {
            var i = 0
            val size = it.size
            while (i < size) {
                if (i == 0 && scale * ZOOM_RATIO_MIN < it[i]) {
                    index = i
                    break
                }
                if (i == size - 1 && scale * ZOOM_RATIO_MIN > it[i]) {
                    index = i
                    break
                }
                if ((scale * ZOOM_RATIO_MIN).toInt() == it[i]) {
                    index = i
                    break
                }
                if (scale * ZOOM_RATIO_MIN > it[i] && scale * ZOOM_RATIO_MIN < it[i + 1]) {
                    index = i
                    break
                }
                i++
            }
        }
        return index
    }

    fun getCurrentZoomScale(): Float {
        return getCameraParams()?.let {
            it.zoomRatios[it.zoom] / ZOOM_RATIO_MIN
        } ?: 1f
    }

    private object CameraEngineHolder {
        val holder = CameraEngine()
    }

}