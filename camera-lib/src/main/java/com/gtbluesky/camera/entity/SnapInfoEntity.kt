package com.gtbluesky.camera.entity

import android.graphics.Rect

data class SnapInfoEntity(
    val filePath: String,
    val rotation: Int,
    val clipRect: Rect? = null
)
