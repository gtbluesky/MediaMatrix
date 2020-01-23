package com.github.gtbluesky.camera.engine

import android.hardware.Camera

class CameraParam private constructor() {

    // 相机长宽比类型
    var aspectRatioType = AspectRatioType.W_16_H_9
    // 当前长宽比
    var currentRatio = RATIO_16_9
    // 期望帧率
    var expectFps = EXPECTED_PREVIEW_FPS
    // 实际帧率
    var previewFps = 0
    // 期望预览宽度
    var expectWidth = DEFAULT_16_9_WIDTH
    // 期望预览高度
    var expectHeight = DEFAULT_16_9_HEIGHT
    // 实际预览宽度
    var previewWidth = 0
    // 实际预览高度
    var previewHeight = 0

    var cameraAutoFocusCallback: Camera.AutoFocusCallback? = null

    var viewWidth = 0
    var viewHeight = 0
    var cameraId = BACK_CAMERA_ID

    companion object {
        const val FRONT_CAMERA_ID = Camera.CameraInfo.CAMERA_FACING_FRONT
        const val BACK_CAMERA_ID = Camera.CameraInfo.CAMERA_FACING_BACK
        // 最大权重
        val MAX_FOCUS_WEIGHT = 1000
        // 录制时长(毫秒)
        val DEFAULT_RECORD_TIME = 15000
        // 16:9的默认宽高
        val DEFAULT_16_9_WIDTH = 1280
        val DEFAULT_16_9_HEIGHT = 720
        // 4:3的默认宽高
        val DEFAULT_4_3_WIDTH = 1024
        val DEFAULT_4_3_HEIGHT = 768
        // 期望FPS
        val EXPECTED_PREVIEW_FPS = 30
        val RATIO_4_3 = 0.75f
        val RATIO_16_9 = 0.5625f

        fun getInstance() = CameraParamHolder.holder
    }

    internal fun reset() {
        cameraId = BACK_CAMERA_ID
        aspectRatioType = AspectRatioType.W_16_H_9
        currentRatio = RATIO_16_9
        expectFps = EXPECTED_PREVIEW_FPS
        previewFps = 0
        expectWidth = DEFAULT_16_9_WIDTH
        expectHeight = DEFAULT_16_9_HEIGHT
        previewWidth = 0
        previewHeight = 0
    }

    internal fun setResolution(resolutionType: ResolutionType) {
        when (resolutionType) {
            ResolutionType.R_720 -> {
                expectWidth = 1280
                expectHeight = 720
            }
            ResolutionType.R_1080 -> {
                expectWidth = 1920
                expectHeight = 1080
            }
        }
        aspectRatioType = AspectRatioType.W_16_H_9
        currentRatio = RATIO_16_9
    }

    private object CameraParamHolder {
        val holder = CameraParam()
    }
}

enum class AspectRatioType {
    W_1_H_1,
    W_4_H_3,
    W_16_H_9
}

enum class ResolutionType {
    R_720,
    R_1080
}