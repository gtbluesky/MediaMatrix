package com.gtbluesky.camera.listener

import java.nio.ByteBuffer

interface OnCaptureFrameListener {
    fun onCaptureFrame(
        byteBuffer: ByteBuffer?,
        width: Int,
        height: Int
    )
}