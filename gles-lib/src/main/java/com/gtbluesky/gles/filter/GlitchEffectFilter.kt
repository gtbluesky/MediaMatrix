package com.gtbluesky.gles.filter

import android.content.Context
import android.opengl.GLES30
import kotlin.math.roundToInt


class GlitchEffectFilter(context: Context) :
    ChangeableEffectFilter(
        context,
        "shader/glitch_effect.frag"
    ) {
    private var maxOffsetHandle = GLES30.GL_NONE
    private var thresholdHandle = GLES30.GL_NONE
    private var colorDriftHandle = GLES30.GL_NONE

    private val driftSequence =
        floatArrayOf(0f, 0.03f, 0.032f, 0.035f, 0.03f, 0.032f, 0.031f, 0.029f, 0.025f)

    private val jitterSequence =
        floatArrayOf(0f, 0.03f, 0.01f, 0.02f, 0.05f, 0.055f, 0.03f, 0.02f, 0.025f)

    private val threshHoldSequence =
        floatArrayOf(1.0f, 0.965f, 0.9f, 0.9f, 0.9f, 0.6f, 0.8f, 0.5f, 0.5f)

    override fun initProgram() {
        super.initProgram()
        maxOffsetHandle = GLES30.glGetUniformLocation(program, "uMaxOffset")
        thresholdHandle = GLES30.glGetUniformLocation(program, "uThreshold")
        colorDriftHandle = GLES30.glGetUniformLocation(program, "uColorDrift")
    }

    override fun preDraw() {
        val interval = (System.currentTimeMillis() - baseTimestamp) / 300f
        val index = (getInterpolation(interval) * 8).roundToInt()
        GLES30.glUniform1f(maxOffsetHandle, jitterSequence[index])
        GLES30.glUniform1f(thresholdHandle, threshHoldSequence[index])
        GLES30.glUniform1f(colorDriftHandle, driftSequence[index])
    }


}