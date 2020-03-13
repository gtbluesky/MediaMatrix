package com.github.gtbluesky.codec

class CodecParam private constructor() {

    // Video
    var videoWidth = VIDEO_WIDTH
    var videoHeight = VIDEO_HEIGHT
    var videoBitRate = VIDEO_BIT_RATE
    var frameRate = FRAME_RATE
    var iFrameInterval = I_FRAME_INTERVAL

    // Audio
    var sampleRate = SAMPLE_RATE
    var channelCount = CHANNEL_COUNT
    var audioBitRate = AUDIO_BIT_RATE


    companion object {
        const val VIDEO_WIDTH = 720
        const val VIDEO_HEIGHT = 1280
        const val VIDEO_BIT_RATE = 1 shl 22
        const val FRAME_RATE = 30
        const val I_FRAME_INTERVAL = 2

        const val CHANNEL_COUNT = 2
        const val SAMPLE_RATE = 44100
        const val AUDIO_BIT_RATE = 64 * 1000

        fun getInstance() = CodecParamHolder.holder
    }

    private object CodecParamHolder {
        val holder = CodecParam()
    }
}