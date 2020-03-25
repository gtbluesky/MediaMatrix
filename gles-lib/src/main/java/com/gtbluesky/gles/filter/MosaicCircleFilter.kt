package com.gtbluesky.gles.filter

import android.content.Context
import android.opengl.GLES30
import com.gtbluesky.gles.util.GLHelper

class MosaicCircleFilter(context: Context) : NormalFilter(
    fragmentShader = GLHelper.getShaderFromAssets(
        context,
        "shader/mosaic_circle.frag"
    )!!
) {
    //在屏幕上显示的像素值（直径）
    var mosaicSize = 50f
    private var mosaicSizeHandle = GLES30.GL_NONE
    private var viewWidthHandle = GLES30.GL_NONE
    private var viewHeightHandle = GLES30.GL_NONE

    override fun initProgram() {
        super.initProgram()
        mosaicSizeHandle = GLES30.glGetUniformLocation(program, "uMosaicSize")
        viewWidthHandle = GLES30.glGetUniformLocation(program, "uViewWidth")
        viewHeightHandle = GLES30.glGetUniformLocation(program, "uViewHeight")
    }

    override fun preDraw() {
        GLES30.glUniform1f(mosaicSizeHandle, mosaicSize)
        GLES30.glUniform1f(viewWidthHandle, viewWidth.toFloat())
        GLES30.glUniform1f(viewHeightHandle, viewHeight.toFloat())
    }
}