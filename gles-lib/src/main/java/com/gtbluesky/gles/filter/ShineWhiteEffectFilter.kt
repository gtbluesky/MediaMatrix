package com.gtbluesky.gles.filter

import android.content.Context
import android.opengl.GLES30


class ShineWhiteEffectFilter(context: Context) :
    ChangeableEffectFilter(
        context,
        "shader/shine_white_effect.frag"
    ) {
    private var percentHandle = GLES30.GL_NONE

    override fun initProgram() {
        super.initProgram()
        percentHandle = GLES30.glGetUniformLocation(program, "uPercent")
    }

    override fun preDraw() {
        val interval = (System.currentTimeMillis() - baseTimestamp) / 300f
        GLES30.glUniform1f(percentHandle, getInterpolation(interval))
    }


}