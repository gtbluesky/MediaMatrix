package com.gtbluesky.gles.filter

import android.content.Context

class GaussianBlurFilter(
    context: Context,
    private val blurRadius: Float = 1f
) : BaseFilterGroup() {
    private val horizontalPassFilter: GaussianBlurSinglePassFilter by lazy {
        GaussianBlurSinglePassFilter(context).also {
            it.blurRadius = blurRadius
            it.isHorizontalPass = true
        }
    }
    private val verticalPassFilter: GaussianBlurSinglePassFilter by lazy {
        GaussianBlurSinglePassFilter(context).also {
            it.blurRadius = blurRadius
            it.isHorizontalPass = false
        }
    }
    private val scaleFilter1: NormalFilter by lazy {
        NormalFilter().also { it.scale = 1f / 4f }
    }
    private val scaleFilter2: NormalFilter by lazy {
        NormalFilter().also { it.scale = 1f / 16 }
    }

    init {
        filterList.let {
            it.add(scaleFilter1)
            it.add(verticalPassFilter)
            it.add(horizontalPassFilter)
//            it.add(scaleFilter2)
//            it.add(verticalPassFilter)
//            it.add(horizontalPassFilter)
        }
    }
}