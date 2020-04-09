package com.gtbluesky.gles.filter

import android.content.Context
import android.opengl.GLES30
import com.gtbluesky.gles.R
import com.gtbluesky.gles.util.GLHelper
import java.nio.FloatBuffer


class IllusionEffectFilter(context: Context) :
    ChangeableEffectFilter(
        context,
        "shader/illusion_effect.frag"
    ) {
    private var lutTextureId = GLES30.GL_NONE
    private val lutTextureUnit = 1
    private var lutTextureUnitHandle = GLES30.GL_NONE
    private var lastFrameTextureId = GLES30.GL_NONE
    private val lastFrameTextureUnit = 2
    private var lastFrameTextureUnitHandle = GLES30.GL_NONE
    private val lastFrameFilter: NormalFilter by lazy {
        NormalFilter()
    }

    override fun initProgram() {
        super.initProgram()
        lutTextureUnitHandle = GLES30.glGetUniformLocation(program, "uLutTextureUnit")
        lastFrameTextureUnitHandle = GLES30.glGetUniformLocation(program, "uLastFrameTextureUnit")
    }

    init {
        lutTextureId = GLHelper.createTexture(GLES30.GL_TEXTURE_2D, context, R.drawable.lookup_amatorka, lutTextureUnit)
    }

    override fun initFrameBuffer(width: Int, height: Int) {
        super.initFrameBuffer(width, height)
        lastFrameFilter.initFrameBuffer(width, height)
    }

    override fun drawFrame(
        textureId: Int,
        vertexBuffer: FloatBuffer,
        textureBuffer: FloatBuffer,
        clearColor: Boolean
    ) {
        super.drawFrame(textureId, vertexBuffer, textureBuffer, clearColor)
        lastFrameTextureId = lastFrameFilter.drawFrameBuffer(textureId, vertexBuffer, textureBuffer)
    }

    override fun drawFrameBuffer(
        textureId: Int,
        vertexBuffer: FloatBuffer,
        textureBuffer: FloatBuffer
    ): Int {
        super.drawFrameBuffer(textureId, vertexBuffer, textureBuffer)
        lastFrameTextureId = lastFrameFilter.drawFrameBuffer(textureId, vertexBuffer, textureBuffer)
        return frameBufferTextureId
    }

    override fun preDraw() {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + lutTextureUnit)
        GLES30.glBindTexture(textureType, lutTextureId)
        GLES30.glUniform1i(lutTextureUnitHandle, lutTextureUnit)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + lastFrameTextureUnit)
        GLES30.glBindTexture(textureType, lastFrameTextureId)
        GLES30.glUniform1i(lastFrameTextureUnitHandle, lastFrameTextureUnit)
    }

    override fun setViewSize(width: Int, height: Int) {
        super.setViewSize(width, height)
        lastFrameFilter.setViewSize(width, height)
    }

    override fun setTextureSize(width: Int, height: Int) {
        super.setTextureSize(width, height)
        lastFrameFilter.setTextureSize(width, height)
    }

    override fun destroy() {
        super.destroy()
        if (lutTextureId != GLES30.GL_NONE) {
            GLES30.glDeleteTextures(1, intArrayOf(lutTextureId), 0)
            lutTextureId = GLES30.GL_NONE
        }
        lastFrameFilter.destroy()
    }
}