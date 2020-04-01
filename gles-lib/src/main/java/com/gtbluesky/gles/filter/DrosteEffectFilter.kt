package com.gtbluesky.gles.filter

import android.content.Context
import android.opengl.GLES30
import com.gtbluesky.gles.util.GLHelper

class DrosteEffectFilter(context: Context) : NormalFilter(
    fragmentShader = GLHelper.getShaderFromAssets(
        context,
        "shader/droste_effect.frag"
    ) ?: ""
) {
    var repeat = 3
    private var repeatHandle = GLES30.GL_NONE

    override fun initProgram() {
        super.initProgram()
        repeatHandle = GLES30.glGetUniformLocation(program, "repeat")
    }

    override fun preDraw() {
        GLES30.glUniform1i(repeatHandle, repeat)
    }
}