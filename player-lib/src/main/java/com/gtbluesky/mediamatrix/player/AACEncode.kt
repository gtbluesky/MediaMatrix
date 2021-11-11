package com.gtbluesky.mediamatrix.player

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import com.gtbluesky.mediamatrix.player.listener.OnRecordTimeListener
import com.gtbluesky.mediamatrix.player.util.LogUtil
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer

class AACEncode {
    private var encoderFormat: MediaFormat? = null
    private var encoder: MediaCodec? = null
    private var outputStream: FileOutputStream? = null
    private val info: MediaCodec.BufferInfo by lazy {
        MediaCodec.BufferInfo()
    }
    private var perPcmSize = 0
    private var outByteBuffer: ByteArray? = null
    private var sampleRate = 44100
    private var recordTime = 0.0 //ms
    private val audioSampleRate = 0
    private var mOnRecordTimeListener: OnRecordTimeListener? = null

    fun initMediacodec(sampleRate: Int, outfile: File?) {
        try {
            this.sampleRate = sampleRate
            encoderFormat =
                MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, 2)
            encoderFormat?.setInteger(MediaFormat.KEY_BIT_RATE, 96000)
            encoderFormat?.setInteger(
                MediaFormat.KEY_AAC_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AACObjectLC
            )
            encoderFormat?.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 4096)
            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            if (encoder == null) {
                LogUtil.e("craete encoder wrong")
            }
            recordTime = 0.0
            encoder?.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            outputStream = FileOutputStream(outfile)
            encoder?.start()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun convertPcmToAac(size: Int, buffer: ByteArray?) {
        LogUtil.d("buffer size is: $size")
        if (buffer != null && encoder != null) {
            recordTime += (size * 1000f / (audioSampleRate * 2 * 2)).toDouble()
            mOnRecordTimeListener?.onRecordTime(recordTime.toInt())
            val inputBufferIndex = encoder?.dequeueInputBuffer(0) ?: -1
            if (inputBufferIndex >= 0) {
                val byteBuffer: ByteBuffer? =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        encoder?.getInputBuffer(inputBufferIndex)
                    } else {
                        encoder?.inputBuffers?.get(inputBufferIndex)
                    }
                byteBuffer?.clear()
                byteBuffer?.put(buffer)
                encoder?.queueInputBuffer(inputBufferIndex, 0, size, 0, 0)
            }
            var index = encoder?.dequeueOutputBuffer(info, 0) ?: -1
            while (index >= 0) {
                try {
                    perPcmSize = info.size + 7
                    outByteBuffer = ByteArray(perPcmSize)
                    val byteBuffer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        encoder?.getOutputBuffer(index)
                    } else {
                        encoder?.outputBuffers?.get(index)
                    }
                    byteBuffer?.position(info.offset)
                    byteBuffer?.limit(info.offset + info.size)
                    outByteBuffer?.let {
                        addAdtsHeader(it, perPcmSize)
                        byteBuffer?.get(it, 7, info.size)
                        byteBuffer?.position(info.offset)
                        outputStream?.write(it, 0, perPcmSize)
                    }
                    encoder?.releaseOutputBuffer(index, false)
                    index = encoder?.dequeueOutputBuffer(info, 0) ?: -1
                    outByteBuffer = null
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Add ADTS header at the beginning of each and every AAC packet.
     * This is needed as MediaCodec encoder generates a packet of raw
     * AAC data.
     */
    private fun addAdtsHeader(packet: ByteArray, packetLen: Int) {
        val profile = 2 // AAC LC，MediaCodecInfo.CodecProfileLevel.AACObjectLC
        val freqIndex = getAdtsSampleRate(sampleRate) // 采样率数组下标，来自ffmpeg源码
        val channelConfig = 2 // channel_configuration
        /**
         * channel_configuration: 表示声道数
         * 0: Defined in AOT Specifc Config
         * 1: 1 channel: front-center
         * 2: 2 channels: front-left, front-right
         * 3: 3 channels: front-center, front-left, front-right
         * 4: 4 channels: front-center, front-left, front-right, back-center
         * 5: 5 channels: front-center, front-left, front-right, back-left, back-right
         * 6: 6 channels: front-center, front-left, front-right, back-left, back-right, LFE-channel
         * 7: 8 channels: front-center, front-left, front-right, side-left, side-right, back-left, back-right, LFE-channel
         * 8-15: Reserved
         */
        packet[0] = 0xFF.toByte() // 0xFFF(12bit) 这里只取了8位，所以还差4位放到下一个里面
        packet[1] = 0xF9.toByte() // 第一个t位放F
        packet[2] = (((profile - 1) shl 6) + (freqIndex shl 2) + (channelConfig shr 2)).toByte()
        packet[3] = (((channelConfig and 3) shl 6) + (packetLen shr 11)).toByte()
        packet[4] = ((packetLen and 0x7FF) shr 3).toByte()
        packet[5] = (((packetLen and 7) shl 5) + 0x1F).toByte()
        packet[6] = 0xFC.toByte()
    }

    private fun getAdtsSampleRate(sampleRate: Int): Int {
        return when (sampleRate) {
            96000 -> 0
            88200 -> 1
            64000 -> 2
            48000 -> 3
            44100 -> 4
            32000 -> 5
            24000 -> 6
            22050 -> 7
            16000 -> 8
            12000 -> 9
            11025 -> 10
            8000 -> 11
            7350 -> 12
            else -> 4
        }
    }

    fun releaseMediacodec() {
        if (encoder == null) {
            return
        }
        try {
            recordTime = 0.0
            outputStream?.close()
            outputStream = null
            encoder?.stop()
            encoder?.release()
            encoder = null
            encoderFormat = null
            LogUtil.d("录制完成...")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun setOnRecordTimeListener(listener: OnRecordTimeListener?) {
        mOnRecordTimeListener = listener
    }
}