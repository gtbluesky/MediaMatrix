package com.gtbluesky.codec

import android.media.*
import android.opengl.EGLContext
import android.os.HandlerThread
import java.io.IOException

/**
 * Hardware Encoder
 */
class HwEncoder(private val rotation: Int) {

    var mediaMuxer: MediaMuxer? = null
        private set
    @Volatile var videoTrackIndex = INVALID_TRACK_INDEX
    @Volatile var audioTrackIndex = INVALID_TRACK_INDEX
    var muxerStarted = false
    val muxerLock = Object()
    private var isEncoding = false
    private var startTimeUs = 0L
    var duration = 0L
        private set

    var isMute = false

    // 视频编码线程
    private var videoEncodeThread: HandlerThread? = null
    private var videoHandler: HwVideoHandler? = null
    // 音频编码线程
    private var audioEncodeThread: HandlerThread? = null
    private var audioHandler: HwAudioHandler? = null

    companion object {
        const val INVALID_TRACK_INDEX = -1
        const val MSG_RENDER = 0x1
        const val MSG_AUDIO_RECORDING = 0x2
        const val MSG_START_ENCODING = 0x3
        const val MSG_STOP_ENCODING = 0x4
        const val MSG_QUIT = 0x5
    }

    init {
        videoEncodeThread = HandlerThread("VideoEncodeThread")
            .apply {
                start()
                videoHandler = HwVideoHandler(looper, this@HwEncoder).also {
                    it.createVideoEncoder(rotation)
                }
            }
        if (!isMute) {
            audioEncodeThread = HandlerThread("AudioEncodeThread")
                .apply {
                    start()
                    audioHandler = HwAudioHandler(looper, this@HwEncoder).also {
                        it.createAudioEncoder()
                    }
                }
        }
    }

    private fun createMuxer(filePath: String): Boolean {
        try {
            mediaMuxer = MediaMuxer(filePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
        return true
    }

    fun start(
        eglContext: EGLContext,
        filePath: String
    ) {
        createMuxer(filePath)
        videoHandler?.apply {
            sendMessage(
                obtainMessage(
                    MSG_START_ENCODING,
                    rotation,
                    rotation,
                    eglContext
                )
            )
        }
        audioHandler?.apply {
            sendMessage(obtainMessage(MSG_START_ENCODING))
        }
    }

    fun stop() {
        videoHandler?.apply {
            sendMessage(obtainMessage(MSG_STOP_ENCODING))
            sendMessage(obtainMessage(MSG_QUIT))
        }
        audioHandler?.apply {
            sendMessage(obtainMessage(MSG_STOP_ENCODING))
            sendMessage(obtainMessage(MSG_QUIT))
        }
        Thread{
            try {
                videoEncodeThread?.join()
                videoEncodeThread = null
                audioEncodeThread?.join()
                audioEncodeThread = null
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            mediaMuxer?.apply {
                if (muxerStarted) {
                    stop()
                    muxerStarted = false
                }
                release()
            }
            mediaMuxer = null
        }.start()
    }

    fun onFrameAvailable(textureId: Int, timeStamp: Long) {
        videoHandler?.apply {
            sendMessage(obtainMessage(
                MSG_RENDER,
                textureId,
                0,
                timeStamp
            ))
        }
    }

    private fun calculateDuration(bufferInfo: MediaCodec.BufferInfo) {
        if (startTimeUs == 0L) {
            startTimeUs = bufferInfo.presentationTimeUs
        } else {
            duration = bufferInfo.presentationTimeUs - startTimeUs
        }
    }
}