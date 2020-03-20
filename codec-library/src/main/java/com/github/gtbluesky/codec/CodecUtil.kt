package com.github.gtbluesky.codec

import android.media.MediaCodec
import android.os.Build
import java.nio.ByteBuffer

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
}