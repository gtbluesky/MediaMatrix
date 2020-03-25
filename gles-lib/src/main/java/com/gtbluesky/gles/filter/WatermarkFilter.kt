package com.gtbluesky.gles.filter

import android.content.Context
import android.opengl.GLES30
import android.opengl.Matrix
import com.gtbluesky.gles.constant.FilterConstant
import com.gtbluesky.gles.util.GLHelper

open class WatermarkFilter : NormalFilter() {
    // 水印的位置和宽高
    private var x = 0
    private var y = 0
    private var width = 0
    private var height = 0
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
            //将顶点坐标的原点移动到屏幕左上角
            Matrix.translateM(it, 0, -1f, 1f, 0f)
            //缩放+上下翻转
            Matrix.scaleM(it, 0, width.toFloat() / viewWidth, -height.toFloat() / viewHeight, 1f)
            //移动坐标使其左下角与屏幕左上角重合
            Matrix.translateM(it, 0, 1f, 1f, 0f)
            //根据x和y值移动坐标
            Matrix.translateM(it, 0, 2f * x / width, 2f * y / height, 0f)
        }
        watermark.setViewSize(frameWidth, frameHeight)
        watermark.drawFrame(
            textureId,
            GLHelper.createFloatBuffer(FilterConstant.VERTEX_COORDS),
            GLHelper.createFloatBuffer(FilterConstant.TEXTURE_COORDS),
            false
        )
        GLES30.glDisable(GLES30.GL_BLEND)
    }

    open fun setBlendFun() {
        GLES30.glBlendFunc(GLES30.GL_SRC_COLOR, GLES30.GL_DST_ALPHA)
    }

    override fun destroy() {
        super.destroy()
        watermark.destroy()
    }
}