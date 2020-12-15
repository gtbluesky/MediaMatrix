package com.gtbluesky.gles.filter

import android.content.Context
import android.opengl.GLES30
import android.opengl.Matrix
import android.util.Log
import com.gtbluesky.gles.constant.FilterConstant
import com.gtbluesky.gles.util.GLHelper
import java.nio.FloatBuffer

open class NormalFilter(
    protected val context: Context? = null,
    protected val vertexShader: String = VERTEX_SHADER,
    protected val fragmentShader: String = FRAGMENT_SHADER
) : BaseFilter() {

    companion object {
        private const val VERTEX_SHADER = """
            attribute vec4 aPosition;
            attribute vec4 aTextureCoord;
            varying vec2 vTextureCoord;
            uniform mat4 uMVPMatrix;
            void main() {
                gl_Position = uMVPMatrix * aPosition;
                vTextureCoord = aTextureCoord.xy;
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D uTextureUnit;
            varying vec2 vTextureCoord;
            void main() {
                gl_FragColor = texture2D(uTextureUnit, vTextureCoord);
            }
        """
    }

    init {
        initProgram()
    }

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
        viewWidth = (width * scale).toInt()
        viewHeight = (height * scale).toInt()
    }

    override fun setTextureSize(width: Int, height: Int) {
        textureWidth = (width * scale).toInt()
        textureHeight = (height * scale).toInt()
    }

    override fun drawFrame(
        textureId: Int,
        vertexBuffer: FloatBuffer,
        textureBuffer: FloatBuffer,
        clearColor: Boolean
    ) {
        GLES30.glViewport(0, 0, viewWidth, viewHeight)
        if (clearColor) {
            GLES30.glClearColor(0f, 0f, 0f, 1f)
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
        GLES30.glViewport(0, 0, frameWidth, frameHeight)
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

        GLES30.glVertexAttribPointer(
            textureCoordHandle,
            2,
            GLES30.GL_FLOAT,
            false,
            0,
            textureBuffer
        )
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