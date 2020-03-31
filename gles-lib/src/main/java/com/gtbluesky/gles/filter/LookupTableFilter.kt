package com.gtbluesky.gles.filter

import android.content.Context
import android.opengl.GLES30
import com.gtbluesky.gles.R
import com.gtbluesky.gles.util.GLHelper

class LookupTableFilter(context: Context) : NormalFilter(
    fragmentShader = GLHelper.getShaderFromAssets(
        context,
        "shader/lookup_table.frag"
    ) ?: ""
) {
    var intensity = 1f
    private var intensityHandle = GLES30.GL_NONE
    private var lutTextureId = GLES30.GL_NONE
    private val lutTextureUnit = 1
    private var lutTextureUnitHandle = GLES30.GL_NONE

    override fun initProgram() {
        super.initProgram()
        lutTextureUnitHandle = GLES30.glGetUniformLocation(program, "uLutTextureUnit")
        intensityHandle = GLES30.glGetUniformLocation(program, "intensity")
    }

    init {
        lutTextureId = GLHelper.createTexture(GLES30.GL_TEXTURE_2D, context, R.drawable.lookup_amatorka, lutTextureUnit)
    }

    override fun preDraw() {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + lutTextureUnit)
        GLES30.glBindTexture(textureType, lutTextureId)
        GLES30.glUniform1i(lutTextureUnitHandle, lutTextureUnit)
        GLES30.glUniform1f(intensityHandle, intensity)
    }
}