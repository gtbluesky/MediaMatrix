package com.gtbluesky.gles.filter

import android.content.Context
import android.opengl.GLES30
import android.opengl.Matrix
import com.gtbluesky.gles.constant.FilterConstant
import com.gtbluesky.gles.util.GLHelper

open class WatermarkFilter : NormalFilter() {
    // 水印的位置和宽高
    private var x: Int = 0
    private var y: Int = 0
    private var width: Int = 0
    private var height: Int = 0
    private var textureId = GLES30.GL_NONE

    private val watermark: NormalFilter by lazy {
        NormalFilter()
    }

    fun setResource(
        context: Context,
        resId: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ) {
        textureId = GLHelper.createTexture(GLES30.GL_TEXTURE_2D, context, resId)
        this.x = x
        this.y = y
        this.width = width
        this.height = height
    }

    override fun postDraw() {
        GLES30.glEnable(GLES30.GL_BLEND)
        setBlendFun()
        watermark.mvpMatrix.let {
            Matrix.setIdentityM(it, 0)
            Matrix.scaleM(it, 0, 1f, -1f, 1f)
        }
        watermark.setViewSize(width, height)
        watermark.drawFrame(
            textureId,
            GLHelper.createFloatBuffer(FilterConstant.VERTEX_COORDS),
            GLHelper.createFloatBuffer(FilterConstant.TEXTURE_COORDS),
            false,
            x,
            viewHeight - y - height
        )
        GLES30.glDisable(GLES30.GL_BLEND)
    }

    open fun setBlendFun() {
        GLES30.glBlendFunc(GLES30.GL_SRC_COLOR, GLES30.GL_DST_ALPHA)
    }
}