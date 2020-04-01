package com.gtbluesky.gles.filter

import android.content.Context
import android.opengl.GLES30
import com.gtbluesky.gles.util.GLHelper
import java.nio.FloatBuffer

class FrameBlurFilter(context: Context) : NormalFilter(
    fragmentShader = GLHelper.getShaderFromAssets(
        context,
        "shader/frame_blur.frag"
    ) ?: ""
) {
    private var blurOffset = 0.1f // 边框模糊偏移值：[0.0, 0.5)
    private var blurOffsetHandle = GLES30.GL_NONE
    private var blurTextureId = GLES30.GL_NONE
    private var blurTextureUnit = 1
    private var blurTextureUnitHandle = GLES30.GL_NONE
    private val gaussianBlurFilter: GaussianBlurFilter by lazy {
        GaussianBlurFilter(context)
    }

    override fun initProgram() {
        super.initProgram()
        blurTextureUnitHandle = GLES30.glGetUniformLocation(program, "uBlurTextureUnit")
        blurOffsetHandle = GLES30.glGetUniformLocation(program, "blurOffset")
    }

    override fun drawFrame(
        textureId: Int,
        vertexBuffer: FloatBuffer,
        textureBuffer: FloatBuffer,
        clearColor: Boolean
    ) {
        blurTextureId = gaussianBlurFilter.drawFrameBuffer(textureId, vertexBuffer, textureBuffer)
        super.drawFrame(textureId, vertexBuffer, textureBuffer, clearColor)
    }

    override fun drawFrameBuffer(
        textureId: Int,
        vertexBuffer: FloatBuffer,
        textureBuffer: FloatBuffer
    ): Int {
        blurTextureId = gaussianBlurFilter.drawFrameBuffer(textureId, vertexBuffer, textureBuffer)
        return super.drawFrameBuffer(textureId, vertexBuffer, textureBuffer)
    }

    override fun preDraw() {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + blurTextureUnit)
        GLES30.glBindTexture(textureType, blurTextureId)
        GLES30.glUniform1i(blurTextureUnitHandle, blurTextureUnit)
        GLES30.glUniform1f(blurOffsetHandle, blurOffset)
    }

    override fun setTextureSize(width: Int, height: Int) {
        gaussianBlurFilter.setTextureSize(width, height)
        super.setTextureSize(width, height)
    }

    override fun setViewSize(width: Int, height: Int) {
        gaussianBlurFilter.setViewSize(width, height)
        super.setViewSize(width, height)
    }

    override fun initFrameBuffer(width: Int, height: Int) {
        gaussianBlurFilter.initFrameBuffer(width, height)
        super.initFrameBuffer(width, height)
    }

    override fun destroy() {
        gaussianBlurFilter.destroy()
        super.destroy()
        if (blurTextureId != GLES30.GL_NONE) {
            GLES30.glDeleteTextures(1, intArrayOf(blurTextureId), 0)
            blurTextureId = GLES30.GL_NONE
        }
    }
}