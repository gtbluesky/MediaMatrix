package com.gtbluesky.gles.filter

import android.content.Context
import android.opengl.GLES30
import com.gtbluesky.gles.util.GLHelper

class MosaicSquareFilter(context: Context) : NormalFilter(
    fragmentShader = GLHelper.getShaderFromAssets(
        context,
        "shader/mosaic_square.frag"
    )!!
) {
    var mosaicSize = 50f
    private var mosaicSizeHandle = GLES30.GL_NONE
    private var textureWidthHandle = GLES30.GL_NONE
    private var textureHeightHandle = GLES30.GL_NONE

    override fun initProgram() {
        super.initProgram()
        mosaicSizeHandle = GLES30.glGetUniformLocation(program, "uMosaicSize")
        textureWidthHandle = GLES30.glGetUniformLocation(program, "uTextureWidth")
        textureHeightHandle = GLES30.glGetUniformLocation(program, "uTextureHeight")
    }

    override fun preDraw() {
        GLES30.glUniform1f(mosaicSizeHandle, mosaicSize)
        GLES30.glUniform1f(textureWidthHandle, textureWidth.toFloat())
        GLES30.glUniform1f(textureHeightHandle, textureHeight.toFloat())
    }
}