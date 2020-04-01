package com.gtbluesky.gles.filter

import android.content.Context
import com.gtbluesky.gles.util.GLHelper
import kotlin.math.cos

abstract class ChangeableEffectFilter(context: Context, shaderPath: String) : NormalFilter(
    fragmentShader = GLHelper.getShaderFromAssets(
        context,
        shaderPath
    ) ?: ""
) {
    protected var baseTimestamp = 0L

    init {
        baseTimestamp = System.currentTimeMillis()
    }

    protected fun getInterpolation(input: Float): Float {
        return (cos((input + 1) * Math.PI) / 2.0f).toFloat() + 0.5f
    }
}