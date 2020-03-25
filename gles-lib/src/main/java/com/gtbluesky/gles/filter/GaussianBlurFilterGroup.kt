package com.gtbluesky.gles.filter

import android.content.Context

class GaussianBlurFilterGroup(context: Context) : BaseFilterGroup() {
    private val horizontalPassFilter: GaussianBlurSinglePassFilter by lazy {
        GaussianBlurSinglePassFilter(context).also {
            it.isHorizontalPass = true
        }
    }
    private val verticalPassFilter: GaussianBlurSinglePassFilter by lazy {
        GaussianBlurSinglePassFilter(context).also {
            it.isHorizontalPass = false
        }
    }

    init {
        filterList.let {
            it.add(verticalPassFilter)
            it.add(horizontalPassFilter)
        }
    }
}