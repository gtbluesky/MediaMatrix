package com.gtbluesky.gles.filter

import android.content.Context
import android.opengl.GLES30
import android.opengl.Matrix
import com.gtbluesky.gles.util.GLHelper
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

class GaussianBlurSinglePassFilter(context: Context) : NormalFilter(
    fragmentShader = GLHelper.getShaderFromAssets(
        context,
        "shader/gaussian_blur_normal.frag"
    ) ?: ""
) {
//    var blurSize = 1f
//    private var widthOffsetHandle = GLES30.GL_NONE
//    private var heightOffsetHandle = GLES30.GL_NONE
    var isHorizontalPass = true
    private val blurRadius = 1
    private val sampleNum = (1 + 2f * blurRadius).pow(2).toInt()
    private val gaussianWeight = FloatArray(sampleNum)
    private var weightHandle = GLES30.GL_NONE
    private var vecStepHandle = GLES30.GL_NONE

    override fun initProgram() {
        super.initProgram()
        weightHandle = GLES30.glGetUniformLocation(program, "uWeight")
        vecStepHandle = GLES30.glGetUniformLocation(program, "uVecStep")
    }

    override fun preDraw() {
        val shift = 0.5f
        val sigma = shift * 5
        var total = 0f
        for (i in 0 until sampleNum) {
            gaussianWeight[i] = (1 / (sqrt(2 * Math.PI) * sigma) * exp(
                -((i - sampleNum / 2).toFloat().pow(2) / (2 * sigma.pow(2))))).toFloat()
            total += gaussianWeight[i]
        }
        for (i in 0 until sampleNum) {
            gaussianWeight[i] /= total
        }
        GLES30.glUniform1fv(weightHandle, sampleNum, GLHelper.createFloatBuffer(gaussianWeight))
        if (isHorizontalPass) {
            GLES30.glUniform2f(vecStepHandle, 10f/viewWidth, 0f)
        } else {
            GLES30.glUniform2f(vecStepHandle, 0f, 10f/viewHeight)
        }
//        GLES30.glUniform1f(
//            widthOffsetHandle, if (isHorizontalPass) {
//                blurSize / textureWidth
//            } else {
//                0f
//            }
//        )
//        GLES30.glUniform1f(
//            heightOffsetHandle, if (isHorizontalPass) {
//                0f
//            } else {
//                blurSize / textureHeight
//            }
//        )
    }
}