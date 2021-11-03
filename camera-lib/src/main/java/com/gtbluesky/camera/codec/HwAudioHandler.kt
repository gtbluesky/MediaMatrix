package com.gtbluesky.camera.codec

import android.media.*
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import com.gtbluesky.camera.codec.HwEncoder
import java.io.IOException
import java.util.concurrent.TimeUnit

class HwAudioHandler(
    looper: Looper,
    private val encoder: HwEncoder
) : Handler(looper) {
    private var audioEncoder: MediaCodec? = null
    private lateinit var audioBufferInfo: MediaCodec.BufferInfo
    private var isEncoding = false
    private val codecParam = CodecParam.getInstance()
    private val minBufferSize: Int
    private var audioRecord: AudioRecord? = null
    private var presentationTimeUs = 0L
    private var totalReadedBytes = 0
    private var baseTimeStampNs = -1L

    companion object {
        private val TAG = HwAudioHandler::class.java.simpleName

        //"audio/mp4a-latm"
        private const val AUDIO_ENCODE_MIME_TYPE = MediaFormat.MIMETYPE_AUDIO_AAC
    }

    init {
        minBufferSize = AudioRecord.getMinBufferSize(
            codecParam.sampleRate,
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            codecParam.sampleRate,
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize
        )
    }

    override fun handleMessage(msg: Message) {
        when (msg.what) {
            HwEncoder.MSG_START_ENCODING -> {
                handleStart()
            }
            HwEncoder.MSG_AUDIO_RECORDING -> {
                handleAudioData(false)
            }
            HwEncoder.MSG_STOP_ENCODING -> {
                handleAudioData(true)
            }
            HwEncoder.MSG_QUIT -> {
                looper.quit()
            }
        }
    }

    fun createAudioEncoder(): Boolean {
        audioBufferInfo = MediaCodec.BufferInfo()
        val format = MediaFormat.createAudioFormat(
            AUDIO_ENCODE_MIME_TYPE,
            codecParam.sampleRate,
            codecParam.channelCount
        ).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, codecParam.audioBitRate)
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        }
        try {
            audioEncoder = MediaCodec.createEncoderByType(AUDIO_ENCODE_MIME_TYPE).apply {
                configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
        return true
    }

    private fun handleStart() {
        baseTimeStampNs = System.nanoTime()
        audioRecord?.startRecording()
        audioEncoder?.start()
        isEncoding = true
        sendMessage(obtainMessage(HwEncoder.MSG_AUDIO_RECORDING))
    }

    private fun release() {
        audioEncoder?.apply {
            stop()
            release()
        }
        audioEncoder = null
        audioRecord?.apply {
            stop()
            release()
        }
        audioRecord = null
    }

    private fun handleAudioData(endOfStream: Boolean) {
        isEncoding = !endOfStream
        Log.d(TAG, "endOfStream=$endOfStream")
        if (endOfStream) {
            removeMessages(HwEncoder.MSG_AUDIO_RECORDING)
            while (!drainAudioData()){}
            release()
        } else {
            drainAudioData()
            sendMessage(obtainMessage(HwEncoder.MSG_AUDIO_RECORDING))
        }
    }

    private fun drainAudioData(): Boolean {
        audioEncoder?.let {
            presentationTimeUs = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - baseTimeStampNs)
            val index = it.dequeueInputBuffer(0)
            if (index >= 0) {
                var length = 0
                CodecUtil.getInputBuffer(it, index)?.also { buffer ->
                    buffer.clear()
                    length = audioRecord?.read(buffer, minBufferSize) ?: 0
                }
                if (length > 0) {
                    totalReadedBytes += length
                    it.queueInputBuffer(
                        index,
                        0,
                        length,
                        presentationTimeUs,
                        if (isEncoding) 0 else MediaCodec.BUFFER_FLAG_END_OF_STREAM
                    )
                    Log.d(TAG, "presentationTime(ms):  ${TimeUnit.MICROSECONDS.toMillis(presentationTimeUs)}")
//                    presentationTimeUs = 1000000L * totalReadedBytes / (codecParam.channelCount * 2 * codecParam.sampleRate)
                }
            }
            var outIndex: Int
            do {
                outIndex = it.dequeueOutputBuffer(audioBufferInfo, 0)
                when {
                    outIndex >= 0 -> {
                        if (audioBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            Log.d(TAG, "audio encode end")
                            it.releaseOutputBuffer(outIndex, false)
                            return true
                        }
                        CodecUtil.getOutputBuffer(it, outIndex)?.let { outputBuffer ->
                            outputBuffer.position(audioBufferInfo.offset)
                            if (encoder.muxerStarted && audioBufferInfo.presentationTimeUs > 0) {
                                try {
                                    encoder.mediaMuxer?.writeSampleData(
                                        encoder.audioTrackIndex,
                                        outputBuffer,
                                        audioBufferInfo
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            it.releaseOutputBuffer(outIndex, false)
                        }

                    }
                    outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    }
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        synchronized(encoder.muxerLock) {
                            encoder.audioTrackIndex = encoder.mediaMuxer
                                ?.addTrack(it.outputFormat)
                                ?: HwEncoder.INVALID_TRACK_INDEX
                            if (encoder.videoTrackIndex > HwEncoder.INVALID_TRACK_INDEX
                                && encoder.audioTrackIndex > HwEncoder.INVALID_TRACK_INDEX
                            ) {
                                encoder.mediaMuxer?.start()
                                encoder.muxerStarted = true
                                encoder.muxerLock.notifyAll()
                            }
                            while (encoder.videoTrackIndex == HwEncoder.INVALID_TRACK_INDEX
                                || encoder.audioTrackIndex == HwEncoder.INVALID_TRACK_INDEX
                            ) {
                                try {
                                    encoder.muxerLock.wait(100)
                                } catch (e: InterruptedException) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                }
            } while (outIndex >= 0)
            return false
        }
        return false
    }
}