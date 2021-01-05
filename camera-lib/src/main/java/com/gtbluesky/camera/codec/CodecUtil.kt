package com.gtbluesky.camera.codec

import android.media.MediaCodec
import android.os.Build
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.experimental.and

object CodecUtil {
    fun getInputBuffer(codec: MediaCodec, index: Int): ByteBuffer? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            codec.getInputBuffer(index)
        } else {
            codec.inputBuffers[index]
        }
    }

    fun getOutputBuffer(codec: MediaCodec, index: Int): ByteBuffer? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            codec.getOutputBuffer(index)
        } else {
            codec.outputBuffers[index]
        }
    }

    /**
     * pcm data with 8 bit storage format convert to 16 bit storage format
     * @param data
     * @return
     */
    fun byteArray2ShortArray(data: ByteArray): ShortArray {
        val samples = ShortArray(data.size / 2)
        for (i in samples.indices) {
            val bl = data[i * 2]
            val bh = data[i * 2 + 1]
            samples[i] = (bh.toInt() shl 8 or bl.toInt()).toShort()
        }
        return samples
    }

    fun pcm2Wav(
        inFilename: String?,
        outFilename: String?,
        sampleRate: Int,
        channels: Int,
        bufferSize: Int
    ) {
        var fis: FileInputStream? = null
        var fos: FileOutputStream? = null
        val totalAudioLen: Long
        val totalDataLen: Long
        val byteRate = 2 * sampleRate * channels
        val data = ByteArray(bufferSize)
        try {
            fis = FileInputStream(inFilename)
            fos = FileOutputStream(outFilename)
            totalAudioLen = fis.channel.size()
            totalDataLen = totalAudioLen + 36
            writeWaveFileHeader(
                fos, totalAudioLen, totalDataLen,
                sampleRate, channels, byteRate
            )
            while (fis.read(data) != -1) {
                fos.write(data)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            fis?.close()
            fos?.close()
        }
    }

    @Throws(IOException::class)
    private fun writeWaveFileHeader(
        out: FileOutputStream,
        totalAudioLen: Long,
        totalDataLen: Long,
        longSampleRate: Int,
        channels: Int,
        byteRate: Int
    ) {
        val header = ByteArray(44)
        // RIFF/WAVE header
        header[0] = 'R'.toByte()
        header[1] = 'I'.toByte()
        header[2] = 'F'.toByte()
        header[3] = 'F'.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = (totalDataLen shr 8 and 0xff).toByte()
        header[6] = (totalDataLen shr 16 and 0xff).toByte()
        header[7] = (totalDataLen shr 24 and 0xff).toByte()
        //WAVE
        header[8] = 'W'.toByte()
        header[9] = 'A'.toByte()
        header[10] = 'V'.toByte()
        header[11] = 'E'.toByte()
        // 'fmt ' chunk
        header[12] = 'f'.toByte()
        header[13] = 'm'.toByte()
        header[14] = 't'.toByte()
        header[15] = ' '.toByte()
        // 4 bytes: size of 'fmt ' chunk
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        // format = 1
        header[20] = 1
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (longSampleRate and 0xff).toByte()
        header[25] = (longSampleRate shr 8 and 0xff).toByte()
        header[26] = (longSampleRate shr 16 and 0xff).toByte()
        header[27] = (longSampleRate shr 24 and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = (byteRate shr 8 and 0xff).toByte()
        header[30] = (byteRate shr 16 and 0xff).toByte()
        header[31] = (byteRate shr 24 and 0xff).toByte()
        // block align
        header[32] = (2 * 16 / 8).toByte()
        header[33] = 0
        // bits per sample
        header[34] = 16
        header[35] = 0
        //data
        header[36] = 'd'.toByte()
        header[37] = 'a'.toByte()
        header[38] = 't'.toByte()
        header[39] = 'a'.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = (totalAudioLen shr 8 and 0xff).toByte()
        header[42] = (totalAudioLen shr 16 and 0xff).toByte()
        header[43] = (totalAudioLen shr 24 and 0xff).toByte()
        out.write(header, 0, 44)
    }
}