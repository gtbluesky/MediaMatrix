package com.github.gtbluesky.camera

import android.hardware.Camera
import android.util.Log
import com.github.gtbluesky.codec.CodecParam
import java.util.*
import kotlin.math.abs
import kotlin.math.sign

class CameraParam private constructor() {

    var resolutionType = ResolutionType.R_720
    var aspectRatioType = AspectRatioType.W_9_H_16
    var ratio = 0f
        private set
    // 期望帧率
    var expectFps = EXPECTED_PREVIEW_FPS
    // 实际帧率
    var previewFps = 0
        private set
    // 期望预览宽度
    var expectWidth = 0
        private set
    // 期望预览高度
    var expectHeight = (expectWidth / ratio).toInt()
        private set
    // 实际预览宽度
    var previewWidth = 0
        private set
    // 实际预览高度
    var previewHeight = 0
        private set

    var cameraAutoFocusCallback: Camera.AutoFocusCallback? = null

    var viewWidth = 0
    var viewHeight = 0
    var cameraId = BACK_CAMERA_ID

    companion object {
        private val TAG = CameraParam::class.java.simpleName
        const val FRONT_CAMERA_ID = Camera.CameraInfo.CAMERA_FACING_FRONT
        const val BACK_CAMERA_ID = Camera.CameraInfo.CAMERA_FACING_BACK
        private const val MAX_FOCUS_WEIGHT = 1000 // 对焦区域最大权重
        private const val DEFAULT_RECORD_TIME = 15000 //ms
        private const val EXPECTED_PREVIEW_FPS = 30
        private const val RATIO_1_1 = 1f
        private const val RATIO_3_4 = 0.75f
        private const val RATIO_9_16 = 0.5625f

        fun getInstance() =
            CameraParamHolder.holder

        private val sizeComparator = Comparator<Camera.Size> { o1, o2 ->
            (o1.width * o1.height - o2.width * o2.height).sign
        }

        internal fun getOptimalSize(
            sizes: List<Camera.Size>,
            expectWidth: Int,
            expectHeight: Int,
            closeEnough: Double = 0.0
        ): Camera.Size {
            Collections.sort(sizes,
                sizeComparator
            )
            val targetRatio = expectWidth.toDouble() / expectHeight.toDouble()
            var optimalSize: Camera.Size? = null
            var minDiff = Double.MAX_VALUE

            sizes.forEach {
                if (expectWidth == it.height && expectHeight == it.width) {
                    optimalSize = it
                    return@forEach
                }
//                val ratio = it.width.toDouble() / it.height.toDouble()
//
//                if (abs(ratio - targetRatio) < minDiff) {
//                    optimalSize = it
//                    minDiff = abs(ratio - targetRatio)
//                }
//
//                if (minDiff <= closeEnough) {
//                    return@forEach
//                }
            }

            Log.d(
                TAG,
                "width: ${optimalSize?.width}, height: ${optimalSize?.height}"
            )

            return optimalSize!!
        }
    }

    init {
        initParams(
            ResolutionType.R_720,
            AspectRatioType.W_9_H_16
        )
    }

    private fun initParams(
        resolutionType: ResolutionType,
        aspectRatioType: AspectRatioType
    ) {
        this.resolutionType = resolutionType
        this.aspectRatioType = aspectRatioType
        expectWidth = when (resolutionType) {
            ResolutionType.R_540 -> 540
            ResolutionType.R_720 -> 720
            ResolutionType.R_1080 -> 1080
        }
        ratio = when (aspectRatioType) {
            AspectRatioType.W_1_H_1 -> RATIO_1_1
            AspectRatioType.W_3_H_4 -> RATIO_3_4
            AspectRatioType.W_9_H_16 -> RATIO_9_16
        }
        expectHeight = (expectWidth / ratio).toInt()
    }

    internal fun reset() {
        ratio = 0f
        expectFps =
            EXPECTED_PREVIEW_FPS
        previewFps = 0
        expectWidth = 0
        expectHeight = (expectWidth / ratio).toInt()
        previewWidth = 0
        previewHeight = 0
        cameraAutoFocusCallback = null
        viewWidth = 0
        viewHeight = 0
        cameraId =
            BACK_CAMERA_ID
        initParams(
            ResolutionType.R_720,
            AspectRatioType.W_9_H_16
        )
    }

    internal fun setResolution(
        sizes: List<Camera.Size>,
        resolutionType: ResolutionType,
        aspectRatioType: AspectRatioType
    ) {
        initParams(resolutionType, aspectRatioType)
        setPreviewSize(sizes)
    }

    internal fun setPreviewSize(sizes: List<Camera.Size>): Camera.Size {
        return getOptimalSize(
            sizes,
            expectWidth,
            expectHeight
        ).also {
            previewWidth = it.height
            previewHeight = it.width
            CodecParam.getInstance().videoWidth = previewWidth
            CodecParam.getInstance().videoHeight = previewHeight
            CodecParam.getInstance().videoBitRate = previewWidth * previewHeight * 3
        }
    }

    private object CameraParamHolder {
        val holder = CameraParam()
    }
}

enum class AspectRatioType {
    W_1_H_1,
    W_3_H_4,
    W_9_H_16
}

enum class ResolutionType {
    R_540,
    R_720,
    R_1080
}