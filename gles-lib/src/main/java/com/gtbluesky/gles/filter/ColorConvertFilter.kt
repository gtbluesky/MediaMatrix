package com.gtbluesky.gles.filter

import android.content.Context
import android.opengl.GLES30
import com.gtbluesky.gles.util.GLHelper

class ColorConvertFilter(context: Context) : NormalFilter(
    fragmentShader = GLHelper.getShaderFromAssets(
        context,
        "shader/color_convert.frag"
    ) ?: ""
) {
    private var hue = 0f // [-0.5, 0.5]
    private var saturation = 1f // [0.0, 2.0]
    private var value = 1f // [0.0, 2.0]
    private var contrast = 0f // [-1, 1]
    private var hueHandle = GLES30.GL_NONE
    private var saturationHandle = GLES30.GL_NONE
    private var valueHandle = GLES30.GL_NONE
    private var contrastHandle = GLES30.GL_NONE

    override fun initProgram() {
        super.initProgram()
        hueHandle = GLES30.glGetUniformLocation(program, "u_hue")
        saturationHandle = GLES30.glGetUniformLocation(program, "u_saturation")
        valueHandle = GLES30.glGetUniformLocation(program, "u_value")
        contrastHandle = GLES30.glGetUniformLocation(program, "u_contrast")
    }

    override fun preDraw() {
        GLES30.glUniform1f(hueHandle, hue)
        GLES30.glUniform1f(saturationHandle, saturation)
        GLES30.glUniform1f(valueHandle, value)
        GLES30.glUniform1f(contrastHandle, contrast)
    }
}