package com.github.gtbluesky.codec.hardware

import android.media.*
import android.opengl.EGLContext
import android.os.Build
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import java.io.IOException
import java.nio.ByteBuffer

class Encoder {

    private var videoEncoder: MediaCodec? = null
    private var videoBufferInfo: MediaCodec.BufferInfo? = null
    private var videoTrackIndex = INVALID_TRACK_INDEX

    private var audioEncoder: MediaCodec? = null
    private var audioBufferInfo: MediaCodec.BufferInfo? = null
    private var audioTrackIndex = INVALID_TRACK_INDEX

    var inputSurface: Surface? = null
        private set
    private var mediaMuxer: MediaMuxer? = null
    private val encodeSync = Any()

    private var isEncoding = false
    private var muxerStarted = false

    private var startTimeUs = 0L
    var duration = 0L
        private set

    var enableAudio = true

    @Volatile private var requestDrainCount: Int = 0

    // 视频编码线程
    private var videoEncodeThread: HandlerThread? = null
    private var videoEncodeHandler: VideoEncodeHandler? = null
    // 视频复用线程
    private var videoMuxThread: HandlerThread? = null
    // 音频编码线程
    private var audioEncodeThread: HandlerThread? = null
    private var audioEncodeHandler: AudioEncodeHandler? = null
    // 音频复用线程
    private var audioMuxThread: HandlerThread? = null

    companion object {
        //"video/avc"
        private const val VIDEO_ENCODER_MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
        //"audio/mp4a-latm"
        private const val AUDIO_ENCODER_MIME_TYPE = MediaFormat.MIMETYPE_AUDIO_AAC
        private const val AUDIO_CHANNEL_COUNT = 2
        private const val INVALID_TRACK_INDEX = -1
        private const val FRAME_RATE = 30
        private const val IFRAME_INTERVAL = 5

        private val TAG = Encoder::class.java.simpleName

    }

    fun initEncoder() {
        initThread()
        createMuxer()
        createVideoEncoder()
        createAudioEncoder()
    }

    private fun initThread() {
        videoEncodeThread = HandlerThread("VideoEncodeThread")
            .apply {
                start()
                videoEncodeHandler = VideoEncodeHandler(looper, this@Encoder)
            }

        videoMuxThread = HandlerThread("VideoMuxThread")
            .apply {
                start()
            }

        audioEncodeThread = HandlerThread("AudioEncodeThread")
            .apply {
                start()
                audioEncodeHandler = AudioEncodeHandler(looper)
            }

        audioMuxThread = HandlerThread("AudioMuxThread")
            .apply {
                start()
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

    private fun createVideoEncoder(): Boolean {
        videoBufferInfo = MediaCodec.BufferInfo()
        val format = MediaFormat.createVideoFormat(
            VIDEO_ENCODER_MIME_TYPE,
            videoWidth, videoHeight
        ).apply {
                setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
                )
                setInteger(MediaFormat.KEY_BIT_RATE, params.getBitRate())
                setInteger(MediaFormat.KEY_FRAME_RATE, VideoParams.FRAME_RATE)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, VideoParams.I_FRAME_INTERVAL)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setInteger(
                        MediaFormat.KEY_BITRATE_MODE,
                        MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR
                    )
                    setInteger(
                        MediaFormat.KEY_COMPLEXITY,
                        MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR
                    )
                }
        }
        try {
            videoEncoder = MediaCodec.createEncoderByType(VIDEO_ENCODER_MIME_TYPE).apply {
                configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                inputSurface = createInputSurface()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
        return true
    }

    private fun createAudioEncoder(samplerate: Int, channelCount: Int, bitrate: Int): Boolean {
        audioBufferInfo = MediaCodec.BufferInfo()
        val format = MediaFormat.createAudioFormat(
            AUDIO_ENCODER_MIME_TYPE,
            samplerate,
            channelCount
        ).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectHE)
            setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_MONO)
        }
        try {
            audioEncoder = MediaCodec.createEncoderByType(AUDIO_ENCODER_MIME_TYPE).apply {
                configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
        return true
    }

    fun start(textureWidth: Int, textureHeight: Int, eglContext: EGLContext) {
        videoEncodeHandler?.apply {
            sendMessage(obtainMessage(
                VideoEncodeHandler.MSG_START_ENCODING,
                textureWidth,
                textureHeight,
                eglContext
            ))
        }
    }

    fun stop() {
        //videoencode thread
    }

    fun finish() {
        //videoencode thread
        release()
    }

    private fun drainVideoEncoder(endOfStream: Boolean) {
        if (endOfStream) {
            videoEncoder?.signalEndOfInputStream()
        }

        while (true) {
            val outputBufferIndex = videoEncoder!!.dequeueOutputBuffer(videoBufferInfo!!, 10000)
            if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (!endOfStream) {
                    break // out of while
                } else {
                    Log.d(
                        TAG,
                        "no output available, spinning to await EOS"
                    )
                }
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not expected for an encoder
                Log.d(
                    TAG,
                    "encoder output buffer changed"
                )
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                synchronized(encodeSync) {
                    // should happen before receiving buffers, and should only happen once
                    if (muxerStarted) {
                        throw RuntimeException("format changed twice")
                    }
                    val newFormat = videoEncoder!!.outputFormat
                    Log.d(
                        TAG,
                        "encoder output format changed: ${newFormat.getString(MediaFormat.KEY_MIME)}"
                    )
                    // now that we have the Magic Goodies, start the muxer
                    videoTrackIndex = mediaMuxer!!.addTrack(newFormat)
                    if (videoTrackIndex > INVALID_TRACK_INDEX && audioTrackIndex > INVALID_TRACK_INDEX) {
                        mediaMuxer?.start()
                        muxerStarted = true
                    }
                }
            } else if (outputBufferIndex < 0) {
                Log.w(
                    TAG,
                    "unexpected result from encoder.dequeueOutputBuffer: $outputBufferIndex"
                )
            } else {
                val encodedBuffer = getOutputBuffer(videoEncoder!!, outputBufferIndex) ?: throw RuntimeException("encoderOutputBuffer $outputBufferIndex was null")

                if (videoBufferInfo!!.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                    Log.d(
                        TAG,
                        "ignoring BUFFER_FLAG_CODEC_CONFIG"
                    )
                    videoEncoder!!.outputFormat.setByteBuffer("csd-0", encodedBuffer)
                    videoBufferInfo!!.size = 0
                }
                if (videoBufferInfo!!.size > 0 && muxerStarted) {
                    calculateDuration(videoBufferInfo!!)
                    encodedBuffer.position(videoBufferInfo!!.offset)
                    encodedBuffer.limit(videoBufferInfo!!.offset + videoBufferInfo!!.size)
                    mediaMuxer?.writeSampleData(videoTrackIndex, encodedBuffer, videoBufferInfo!!)
                    Log.d(
                        TAG,
                        "sent ${videoBufferInfo?.size} bytes to muxer, ts=${videoBufferInfo?.presentationTimeUs}"
                    )
                }
                videoEncoder?.releaseOutputBuffer(outputBufferIndex, false)
                if (videoBufferInfo!!.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    if (!endOfStream) {
                        Log.w(
                            TAG,
                            "reached end of stream unexpectedly"
                        )
                    } else {
                        Log.d(
                            TAG,
                            "end of stream reached"
                        )
                    }
                    break // out of while
                }
            }
        }

    }

    private fun drainAudioEncoder(endOfStream: Boolean) {

    }

    fun onFrameAvailable() {
        //videoencode thread
    }

    private fun release() {
        videoEncoder?.apply {
            stop()
            release()
        }
        videoEncoder = null

        audioEncoder?.apply {
            stop()
            release()
        }
        audioEncoder = null

        mediaMuxer?.apply {
            if (muxerStarted) {
                stop()
                muxerStarted = false
            }
            release()
        }
        mediaMuxer = null
    }

    private fun calculateDuration(bufferInfo: MediaCodec.BufferInfo) {
        if (startTimeUs == 0L) {
            startTimeUs = bufferInfo.presentationTimeUs
        } else {
            duration = bufferInfo.presentationTimeUs - startTimeUs
        }
    }

    private fun getInputBuffer(codec: MediaCodec, index: Int): ByteBuffer? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            codec.getInputBuffer(index)
        } else {
            codec.inputBuffers[index]
        }
    }

    private fun getOutputBuffer(codec: MediaCodec, index: Int): ByteBuffer? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            codec.getOutputBuffer(index)
        } else {
            codec.outputBuffers[index]
        }
    }
}