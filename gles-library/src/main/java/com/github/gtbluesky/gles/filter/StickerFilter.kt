package com.github.gtbluesky.gles.filter

import android.opengl.GLES30

class StickerFilter : WatermarkFilter() {
    override fun setBlendFun() {
        GLES30.glBlendEquation(GLES30.GL_FUNC_ADD)
        GLES30.glBlendFuncSeparate(
            GLES30.GL_ONE,
            GLES30.GL_ONE_MINUS_SRC_ALPHA,
            GLES30.GL_ONE,
            GLES30.GL_ONE
        )
    }
}