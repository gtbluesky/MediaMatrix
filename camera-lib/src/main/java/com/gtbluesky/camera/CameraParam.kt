package com.gtbluesky.camera

import android.hardware.Camera
import android.util.Log
import com.gtbluesky.camera.codec.CodecParam
import com.gtbluesky.camera.listener.OnCameraFocusListener
import java.util.*
import kotlin.math.sign

class CameraParam private constructor() {

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
    var expectHeight = 0
        private set

    // 相机预览宽度
    var previewWidth = 0
        private set

    // 相机预览高度
    var previewHeight = 0
        private set

    var onCameraFocusListener: OnCameraFocusListener? = null

    var viewWidth = 0
        private set
    var viewHeight = 0
        private set
    var cameraId = BACK_CAMERA_ID

    private var isLandscape = false

    companion object {
        private val TAG = CameraParam::class.java.simpleName
        const val FRONT_CAMERA_ID = Camera.CameraInfo.CAMERA_FACING_FRONT
        const val BACK_CAMERA_ID = Camera.CameraInfo.CAMERA_FACING_BACK
        private const val MAX_FOCUS_WEIGHT = 1000 // 对焦区域最大权重
        private const val EXPECTED_PREVIEW_FPS = 30
        private const val RATIO_1_1 = 1f
        private const val RATIO_3_4 = 0.75f
        private const val RATIO_9_16 = 0.5625f

        fun getInstance() = CameraParamHolder.holder

        // From big to small
        private val sizeComparator = Comparator<Camera.Size> { o1, o2 ->
            (o2.width * o2.height - o1.width * o1.height).sign
        }
    }

    fun initParams(
        resolutionType: ResolutionType,
        aspectRatioType: AspectRatioType,
        viewWidth: Int,
        viewHeight: Int,
        isLandscape: Boolean = false
    ) {
        this.viewWidth = viewWidth
        this.viewHeight = viewHeight
        this.isLandscape = isLandscape
        when (resolutionType) {
            ResolutionType.R_540 -> 540
            ResolutionType.R_720 -> 720
            ResolutionType.R_1080 -> 1080
        }.let {
            if (isLandscape) {
                expectHeight = it
            } else {
                expectWidth = it
            }
        }
        ratio = when (aspectRatioType) {
            AspectRatioType.W_1_H_1 -> RATIO_1_1
            AspectRatioType.W_3_H_4 -> RATIO_3_4
            AspectRatioType.W_9_H_16 -> RATIO_9_16
            AspectRatioType.FULL -> if (isLandscape) {
                viewHeight.toFloat() / viewWidth
            } else {
                viewWidth.toFloat() / viewHeight
            }
        }
        if (isLandscape) {
            expectWidth = ((expectHeight / ratio).toInt() + 1) / 2 * 2
        } else {
            expectHeight = ((expectWidth / ratio).toInt() + 1) / 2 * 2
        }
        CodecParam.getInstance().videoWidth = expectWidth
        CodecParam.getInstance().videoHeight = expectHeight
        CodecParam.getInstance().videoBitRate = expectWidth * expectHeight * 3
    }

    internal fun reset() {
        ratio = 0f
        expectFps = EXPECTED_PREVIEW_FPS
        previewFps = 0
        expectWidth = 0
        expectHeight = 0
        previewWidth = 0
        previewHeight = 0
        onCameraFocusListener = null
        cameraId = BACK_CAMERA_ID
        initParams(
            ResolutionType.R_720,
            AspectRatioType.W_9_H_16,
            viewWidth, viewHeight
        )
    }

    internal fun setResolution(
        sizes: List<Camera.Size>,
        resolutionType: ResolutionType,
        aspectRatioType: AspectRatioType
    ) {
        initParams(resolutionType, aspectRatioType, viewWidth, viewHeight)
        setPreviewSize(sizes)
    }

    internal fun setPreviewSize(sizes: List<Camera.Size>): Camera.Size {
        return if (isLandscape) {
            getOptimalSize(
                sizes,
                expectHeight,
                expectWidth
            ).also {
                previewWidth = it.width
                previewHeight = it.height
            }
        } else {
            getOptimalSize(
                sizes,
                expectWidth,
                expectHeight
            ).also {
                previewWidth = it.height
                previewHeight = it.width
            }
        }
    }

    /**
     * 相机预览尺寸选取算法（兼顾分辨率和画幅）
     * 第一种：期望的宽高值都有
     * 第二种：期望的宽有，但预览尺寸的width大于期望高，则需要裁剪
     * 第三种：不符合上述条件但其宽高比与某个预览尺寸的宽高比相同，且预览的分辨率大于期望值，则缩小处理
     * 第四种：不符合上述条件，则取最大预览尺寸，先缩放后裁剪
     */
    private fun getOptimalSize(
        sizes: List<Camera.Size>,
        expectWidth: Int,
        expectHeight: Int
    ): Camera.Size {
        Collections.sort(
            sizes,
            sizeComparator
        )
        val targetRatio = expectWidth.toFloat() / expectHeight
        var bestSize: Camera.Size? = null
        var widthEqualSize: Camera.Size? = null
        var ratioEqualSize: Camera.Size? = null

        sizes.forEach {
            Log.d(
                TAG,
                "width=${it.width}, height=${it.height}, ratio=${it.height.toFloat() / it.width}"
            )
            Log.d(TAG, "expectRatio=${targetRatio}")
            if (expectWidth == it.height && expectHeight == it.width) {
                bestSize = it
                return@forEach
            }
            if (expectHeight < it.width && expectWidth == it.height) {
                widthEqualSize = it
            }
            if (expectHeight * it.height == expectWidth * it.width && it.width > expectHeight) {
                ratioEqualSize = it
            }
        }

        val choosedSize = bestSize ?: widthEqualSize ?: ratioEqualSize ?: sizes[0]
        Log.d(
            TAG,
            "choosed size: width: ${choosedSize.width}, height: ${choosedSize.height}"
        )

        return choosedSize
    }

    private object CameraParamHolder {
        val holder = CameraParam()
    }
}

enum class AspectRatioType {
    W_1_H_1,
    W_3_H_4,
    W_9_H_16,
    FULL
}

enum class ResolutionType {
    R_540,
    R_720,
    R_1080
}