package com.gtbluesky.gles.filter

import android.content.Context
import android.opengl.GLES30


class ShakeEffectFilter(context: Context) :
    ChangeableEffectFilter(
        context,
        "shader/shake_effect.frag"
    ) {
    private var scaleHandle = GLES30.GL_NONE

    override fun initProgram() {
        super.initProgram()
        scaleHandle = GLES30.glGetUniformLocation(program, "scale")
    }

    override fun preDraw() {
        val interval = (System.currentTimeMillis() - baseTimestamp) / 33f
        scale = 1f + 0.3f * getInterpolation(interval)
        GLES30.glUniform1f(scaleHandle, scale)
    }


}