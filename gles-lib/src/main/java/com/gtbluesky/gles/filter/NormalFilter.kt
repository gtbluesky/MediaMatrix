package com.gtbluesky.gles.filter

import android.opengl.GLES30
import android.opengl.Matrix
import com.gtbluesky.gles.constant.FilterConstant
import com.gtbluesky.gles.util.GLHelper
import java.nio.FloatBuffer

open class NormalFilter : BaseFilter {

    constructor(): super()

    constructor(vertexShader: String, fragmentShader: String)
            : super(vertexShader, fragmentShader)

    override fun initProgram() {
        program = GLHelper.createProgram(
            vertexShader,
            fragmentShader
        )

        positionHandle = GLES30.glGetAttribLocation(program, "aPosition")
        textureCoordHandle = GLES30.glGetAttribLocation(program, "aTextureCoord")
        textureUnitHandle = GLES30.glGetUniformLocation(program, "uTextureUnit")
        mvpMatrixHandle = GLES30.glGetUniformLocation(program, "uMVPMatrix")

        Matrix.setIdentityM(mvpMatrix, 0)
    }

    override fun setViewSize(width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
    }

    override fun setTextureSize(width: Int, height: Int) {
        textureWidth = width
        textureHeight = height
    }

    override fun drawFrame(
        textureId: Int,
        vertexBuffer: FloatBuffer,
        textureBuffer: FloatBuffer,
        clearColor: Boolean,
        x: Int,
        y: Int
    ) {
        GLES30.glViewport(x, y, viewWidth, viewHeight)
        if (clearColor) {
            GLES30.glClearColor(0f ,0f, 0f, 1f)
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        }

        GLES30.glUseProgram(program)

        drawTexture(textureId, vertexBuffer, textureBuffer)

        GLES30.glBindTexture(textureType, GLES30.GL_NONE)
        GLES30.glUseProgram(GLES30.GL_NONE)
    }

    override fun drawFrameBuffer(
        textureId: Int,
        vertexBuffer: FloatBuffer,
        textureBuffer: FloatBuffer
    ): Int {
        if (frameBufferId == GLES30.GL_NONE
            || frameBufferTextureId == GLES30.GL_NONE
        ) {
            return GLES30.GL_NONE
        }
        GLES30.glViewport(0 ,0, frameWidth, frameHeight)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, frameBufferId)

        GLES30.glUseProgram(program)

        drawTexture(textureId, vertexBuffer, textureBuffer)

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, GLES30.GL_NONE)
        GLES30.glBindTexture(textureType, GLES30.GL_NONE)
        GLES30.glUseProgram(GLES30.GL_NONE)
        return frameBufferTextureId
    }

    private fun drawTexture(
        textureId: Int,
        vertexBuffer: FloatBuffer,
        textureBuffer: FloatBuffer
    ) {
        GLES30.glVertexAttribPointer(positionHandle, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer)
        GLES30.glEnableVertexAttribArray(positionHandle)

        GLES30.glVertexAttribPointer(textureCoordHandle, 2, GLES30.GL_FLOAT, false, 0, textureBuffer)
        GLES30.glEnableVertexAttribArray(textureCoordHandle)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + textureUnit)
        GLES30.glBindTexture(textureType, textureId)
        GLES30.glUniform1i(textureUnitHandle, textureUnit)

        GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        preDraw()
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, FilterConstant.VERTEX_COORDS.size / 2)
        postDraw()

        GLES30.glDisableVertexAttribArray(positionHandle)
        GLES30.glDisableVertexAttribArray(textureCoordHandle)
    }

    protected open fun preDraw() {}

    protected open fun postDraw() {}
}