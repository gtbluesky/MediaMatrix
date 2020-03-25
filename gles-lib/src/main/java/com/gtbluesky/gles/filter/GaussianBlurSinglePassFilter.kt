package com.gtbluesky.gles.filter

import android.content.Context
import android.opengl.GLES30
import com.gtbluesky.gles.util.GLHelper

class GaussianBlurSinglePassFilter(context: Context) : NormalFilter(
    vertexShader = GLHelper.getShaderFromAssets(
        context,
        "shader/gaussian_blur_pro.vert"
    ) ?: "",
    fragmentShader = GLHelper.getShaderFromAssets(
        context,
        "shader/gaussian_blur_pro.frag"
    ) ?: ""
) {
    var blurSize = 1f
    private var widthOffsetHandle = GLES30.GL_NONE
    private var heightOffsetHandle = GLES30.GL_NONE
    var isHorizontalPass = true

    override fun initProgram() {
        super.initProgram()
        widthOffsetHandle = GLES30.glGetUniformLocation(program, "uWidthOffset")
        heightOffsetHandle = GLES30.glGetUniformLocation(program, "uHeightOffset")
    }

    override fun preDraw() {
        GLES30.glUniform1f(
            widthOffsetHandle, if (isHorizontalPass) {
                blurSize / textureWidth
            } else {
                0f
            }
        )
        GLES30.glUniform1f(
            heightOffsetHandle, if (isHorizontalPass) {
                0f
            } else {
                blurSize / textureHeight
            }
        )
    }
}