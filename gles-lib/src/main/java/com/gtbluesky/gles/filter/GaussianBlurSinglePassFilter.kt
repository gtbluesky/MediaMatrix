package com.gtbluesky.gles.filter

import android.content.Context
import android.opengl.GLES30
import com.gtbluesky.gles.util.GLHelper
import java.nio.FloatBuffer
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

internal class GaussianBlurSinglePassFilter(context: Context) : NormalFilter(
    fragmentShader = GLHelper.getShaderFromAssets(
        context,
        "shader/gaussian_blur_normal.frag"
    ) ?: ""
) {
    var isHorizontalPass = true
    var blurRadius = 1f
    private val sampleNum = 9
    private val gaussianWeight = FloatArray(sampleNum)
    private var weightHandle = GLES30.GL_NONE
    private var vecStepHandle = GLES30.GL_NONE
    private val gaussianWeightBuffer: FloatBuffer by lazy {
        val sigma = 5f
        var total = 0f
        for (i in 0 until sampleNum) {
            gaussianWeight[i] = (1 / (sqrt(2 * Math.PI) * sigma) * exp(
                -((i - sampleNum / 2).toFloat().pow(2) / (2 * sigma.pow(2)))
            )).toFloat()
            total += gaussianWeight[i]
        }
        for (i in 0 until sampleNum) {
            gaussianWeight[i] /= total
        }
        GLHelper.createFloatBuffer(gaussianWeight)
    }

    override fun initProgram() {
        super.initProgram()
        weightHandle = GLES30.glGetUniformLocation(program, "uWeight")
        vecStepHandle = GLES30.glGetUniformLocation(program, "uVecStep")
    }

    override fun preDraw() {
        GLES30.glUniform1fv(weightHandle, sampleNum, gaussianWeightBuffer)
        if (isHorizontalPass) {
            GLES30.glUniform2f(vecStepHandle, blurRadius / textureWidth, 0f)
        } else {
            GLES30.glUniform2f(vecStepHandle, 0f, blurRadius / textureHeight)
        }
    }
}