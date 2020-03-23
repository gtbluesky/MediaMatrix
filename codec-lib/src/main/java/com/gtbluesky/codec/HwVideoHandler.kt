package com.gtbluesky.codec

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.opengl.EGLContext
import android.opengl.GLES30
import android.opengl.Matrix
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.Surface
import com.gtbluesky.gles.constant.FilterConstant
import com.gtbluesky.gles.egl.EglCore
import com.gtbluesky.gles.egl.WindowSurface
import com.gtbluesky.gles.filter.NormalFilter
import com.gtbluesky.gles.util.GLHelper
import java.io.IOException
import java.nio.FloatBuffer

class HwVideoHandler(
    looper: Looper,
    private val encoder: HwEncoder
) : Handler(looper) {
    private var videoEncoder: MediaCodec? = null
    private lateinit var videoBufferInfo: MediaCodec.BufferInfo

    private var inputSurface: Surface? = null
    private var eglCore: EglCore? = null
    private var windowSurface: WindowSurface? = null
    private val recordFilter : NormalFilter by lazy {
        NormalFilter()
    }
    private val vertexBuffer: FloatBuffer by lazy {
        GLHelper.createFloatBuffer(FilterConstant.VERTEX_COORDS)
    }
    private val textureBuffer: FloatBuffer by lazy {
        GLHelper.createFloatBuffer(FilterConstant.TEXTURE_COORDS)
    }
    private var isEncoding = false

    private var baseTimeStamo = -1L

    private val codecParam = CodecParam.getInstance()

    private var frameNum = 0

    companion object {
        private val TAG = HwVideoHandler::class.java.simpleName

        private const val TIMEOUT_USEC = 10000L
        //"video/avc"
        private const val VIDEO_ENCODE_MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
    }

    override fun handleMessage(msg: Message) {
        when (msg.what) {
            HwEncoder.MSG_START_ENCODING -> {
                (msg.obj as? EGLContext)?.let {
                    handleStart(it, msg.arg1)
                }
            }
            HwEncoder.MSG_RENDER -> {
                (msg.obj as? Long)?.let {
                    drawFrame(msg.arg1, it)
                }
            }
            HwEncoder.MSG_STOP_ENCODING -> {
                handleStop()
            }
            HwEncoder.MSG_QUIT -> {
                looper.quit()
            }
        }
    }

    fun createVideoEncoder(rotation: Int): Boolean {
        videoBufferInfo = MediaCodec.BufferInfo()
        val videoWidth: Int
        val videoHeight: Int
        if (rotation % 180 == 0) {
            videoWidth = codecParam.videoWidth
            videoHeight = codecParam.videoHeight
        } else {
            videoWidth = codecParam.videoHeight
            videoHeight = codecParam.videoWidth
        }
        val format = MediaFormat.createVideoFormat(
            VIDEO_ENCODE_MIME_TYPE,
            videoWidth,
            videoHeight
        ).apply {
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
            )
            setInteger(
                MediaFormat.KEY_BIT_RATE,
                codecParam.videoBitRate
            )
            setInteger(
                MediaFormat.KEY_FRAME_RATE,
                codecParam.frameRate
            )
            setInteger(
                MediaFormat.KEY_I_FRAME_INTERVAL,
                codecParam.iFrameInterval
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                setInteger(
                    MediaFormat.KEY_BITRATE_MODE,
                    MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR
                )
                setInteger(
                    MediaFormat.KEY_PROFILE,
                    MediaCodecInfo.CodecProfileLevel.AVCProfileHigh
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (codecParam.videoWidth * codecParam.videoHeight >= 1080 * 1920) {
                    MediaCodecInfo.CodecProfileLevel.AVCLevel4
                } else {
                    MediaCodecInfo.CodecProfileLevel.AVCLevel31
                }.let {
                    setInteger(
                        MediaFormat.KEY_LEVEL,
                        it
                    )
                }
            }
        }
        try {
            videoEncoder = MediaCodec.createEncoderByType(VIDEO_ENCODE_MIME_TYPE).apply {
                configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                inputSurface = createInputSurface()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
        return true
    }

    private fun drainVideoEncoder(endOfStream: Boolean) {
        Log.e(TAG, "endOfStream=$endOfStream")
        videoEncoder?.let {
            if (endOfStream) {
                it.signalEndOfInputStream()
            }
            while (true) {
                val outputBufferIndex = it.dequeueOutputBuffer(videoBufferInfo, TIMEOUT_USEC)
                Log.e(TAG, "关键帧：${videoBufferInfo.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0}")
                if (outputBufferIndex >= 0) {
                    CodecUtil.getOutputBuffer(it, outputBufferIndex)?.let { outputBuffer ->
                        if (videoBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            // The codec config data was pulled out and fed to the muxer when we got
                            // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                            Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG")
//                        it.outputFormat.setByteBuffer("csd-0", encodedBuffer)
                            videoBufferInfo.size = 0
                        }
                        if (videoBufferInfo.size > 0 && encoder.muxerStarted) {
//                            calculateDuration(videoBufferInfo)
                            outputBuffer.position(videoBufferInfo.offset)
                            outputBuffer.limit(videoBufferInfo.offset + videoBufferInfo.size)
                            encoder.mediaMuxer?.writeSampleData(encoder.videoTrackIndex, outputBuffer, videoBufferInfo)
                            Log.d(TAG, "sent ${videoBufferInfo.size} bytes to muxer, pts=${videoBufferInfo.presentationTimeUs}")
                        }
                        it.releaseOutputBuffer(outputBufferIndex, false)
                        if (videoBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            if (!endOfStream) {
                                Log.w(TAG, "reached end of video stream unexpectedly")
                            } else {
                                Log.d(TAG, "end of video stream reached")
                            }
                            return
                        }
                    }
                } else if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    if (!endOfStream) {
                        break // out of while
                    } else {
                        Log.d(TAG, "no output available, spinning to await EOS")
                    }
                } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // not expected for an encoder
                    Log.d(TAG, "encoder output buffer changed")
                } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    synchronized(encoder.muxerLock) {
                        // should happen before receiving buffers, and should only happen once
                        if (encoder.muxerStarted) {
                            throw RuntimeException("format changed twice")
                        }
                        it.outputFormat.let { outputFormat ->
                            Log.d(TAG, "encoder output format changed: ${outputFormat.getString(MediaFormat.KEY_MIME)}")
                            // now that we have the Magic Goodies, start the muxer
                            encoder.videoTrackIndex = encoder.mediaMuxer
                                ?.addTrack(outputFormat)
                                ?: HwEncoder.INVALID_TRACK_INDEX
                            if (encoder.enableAudio) {
                                if (encoder.videoTrackIndex > HwEncoder.INVALID_TRACK_INDEX
                                    && encoder.audioTrackIndex > HwEncoder.INVALID_TRACK_INDEX) {
                                    encoder.mediaMuxer?.start()
                                    encoder.muxerStarted = true
                                    encoder.muxerLock.notifyAll()
                                }
                                while (encoder.videoTrackIndex == HwEncoder.INVALID_TRACK_INDEX
                                    || encoder.audioTrackIndex == HwEncoder.INVALID_TRACK_INDEX) {
                                    try {
                                        encoder.muxerLock.wait(100)
                                    } catch (e: InterruptedException) {
                                        e.printStackTrace()
                                    }
                                }
                            } else {
                                encoder.mediaMuxer?.start()
                                encoder.muxerStarted = true
                            }
                        }
                    }
                } else {
                    Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: $outputBufferIndex")
                }
            }
        }
    }

    private fun destroy() {
        recordFilter.destroy()
        windowSurface?.release()
        windowSurface = null
        eglCore?.release()
        eglCore = null
    }

    private fun drawFrame(textureId: Int, timeStamp: Long) {
        windowSurface?.makeCurrent()
        recordFilter.drawFrame(textureId, vertexBuffer, textureBuffer)
        (System.nanoTime() - baseTimeStamo).let {
            Log.d(TAG, "presentationTime(Us)：$it, presentationTime(s):  ${it / 1000000f}")
            windowSurface?.setPresentationTime(it)
        }
        windowSurface?.swapBuffers()
        Log.e(TAG, "frame num = ${++frameNum}")
        drainVideoEncoder(false)
    }

    private fun handleStart(
        eglContext: EGLContext,
        rotation: Int
    ) {
        baseTimeStamo = System.nanoTime()
        videoEncoder?.start()
        windowSurface?.apply {
            releaseEglSurface()
        }
        eglCore?.apply {
            release()
        }
        eglCore = EglCore(eglContext, EglCore.FLAG_RECORDABLE)
        if (windowSurface == null) {
            windowSurface = WindowSurface(eglCore!!, inputSurface!!, true)
        } else {
            windowSurface?.recreate(eglCore!!)
        }
        windowSurface?.makeCurrent()

        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        GLES30.glDisable(GLES30.GL_CULL_FACE)
        recordFilter.mvpMatrix.let {
            Matrix.setIdentityM(it, 0)
            Matrix.rotateM(it, 0, -rotation.toFloat(), 0f, 0f, 1f)
        }
        val videoWidth: Int
        val videoHeight: Int
        if (rotation % 180 == 0) {
            videoWidth = codecParam.videoWidth
            videoHeight = codecParam.videoHeight
        } else {
            videoWidth = codecParam.videoHeight
            videoHeight = codecParam.videoWidth
        }
        recordFilter.setViewSize(videoWidth, videoHeight)
        isEncoding = true
    }

    private fun handleStop() {
        drainVideoEncoder(true)
        isEncoding = false
        videoEncoder?.apply {
            stop()
            release()
        }
        videoEncoder = null
        destroy()
    }
}