package com.gtbluesky.gles.filter

import android.content.Context
import android.opengl.GLES30
import com.gtbluesky.gles.util.GLHelper

class BeautyFilter(context: Context) : NormalFilter(
    fragmentShader = GLHelper.getShaderFromAssets(
        context,
        "shader/beauty_effect.frag"
    ) ?: ""
) {
    private var widthHandle = GLES30.GL_NONE
    private var heightHandle = GLES30.GL_NONE
    private var smoothStrength = 1f
    private var smoothStrengthHandle = GLES30.GL_NONE
    private var whitenStrength = 0.5f
    private var whitenStrengthHandle = GLES30.GL_NONE

    override fun initProgram() {
        super.initProgram()
        widthHandle = GLES30.glGetUniformLocation(program, "width")
        heightHandle = GLES30.glGetUniformLocation(program, "height")
        smoothStrengthHandle = GLES30.glGetUniformLocation(program, "uSmoothStrength")
        whitenStrengthHandle = GLES30.glGetUniformLocation(program, "uWhitenStrength")
    }

    override fun preDraw() {
        GLES30.glUniform1i(widthHandle, frameWidth)
        GLES30.glUniform1i(heightHandle, frameHeight)
        GLES30.glUniform1f(smoothStrengthHandle, smoothStrength)
        GLES30.glUniform1f(whitenStrengthHandle, whitenStrength)
    }
}