package com.github.gtbluesky.codec

class CodecParam private constructor() {

    // Video
    var videoWidth = 0
    var videoHeight = 0
    var videoBitRate = 0
    var frameRate = FRAME_RATE
    var iFrameInterval = I_FRAME_INTERVAL

    // Audio
    var sampleRate = SAMPLE_RATE
    var channelCount = CHANNEL_COUNT
    var audioBitRate = AUDIO_BIT_RATE

    companion object {
        const val FRAME_RATE = 30
        const val I_FRAME_INTERVAL = 1

        const val CHANNEL_COUNT = 2
        const val SAMPLE_RATE = 44100
        const val AUDIO_BIT_RATE = 128000

        fun getInstance() = CodecParamHolder.holder
    }

    fun reset() {
        videoWidth = 0
        videoHeight = 0
        videoBitRate = 0
        frameRate = FRAME_RATE
        iFrameInterval = I_FRAME_INTERVAL
        sampleRate = SAMPLE_RATE
        channelCount = CHANNEL_COUNT
        audioBitRate = AUDIO_BIT_RATE
    }

    private object CodecParamHolder {
        val holder = CodecParam()
    }
}