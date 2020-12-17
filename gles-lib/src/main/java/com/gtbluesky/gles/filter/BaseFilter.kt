package com.gtbluesky.gles.filter

import android.opengl.GLES30
import com.gtbluesky.gles.util.GLHelper
import java.nio.FloatBuffer

abstract class BaseFilter {
    // 纹理单元
    protected val textureUnit = 0
    protected var textureType = GLES30.GL_TEXTURE_2D

    protected var positionHandle = GLES30.GL_NONE
    protected var textureCoordHandle = GLES30.GL_NONE
    protected var mvpMatrixHandle = GLES30.GL_NONE
    protected var textureUnitHandle = GLES30.GL_NONE

    val mvpMatrix = FloatArray(GL_MATRIX_SIZE)

    // 输入的纹理宽高，与相机预览宽高一致
    var textureWidth = 0
    var textureHeight = 0

    // 视图宽高，用于设置glViewport宽高
    protected var viewWidth = 0
    protected var viewHeight = 0
    protected var program = GLES30.GL_NONE

    // FBO宽高，与纹理宽高一致
    protected var frameWidth = 0
    protected var frameHeight = 0

    // FBO
    protected var frameBufferId = GLES30.GL_NONE
    protected var frameBufferTextureId = GLES30.GL_NONE

    var scale = 1f

    companion object {
        const val GL_MATRIX_SIZE = 16
    }

    protected abstract fun initProgram()

    abstract fun setViewSize(width: Int, height: Int)

    abstract fun setTextureSize(width: Int, height: Int)

    open fun adjustMvpMatrix() {}

    abstract fun drawFrame(
        textureId: Int,
        vertexBuffer: FloatBuffer,
        textureBuffer: FloatBuffer,
        clearColor: Boolean = true
    )

    abstract fun drawFrameBuffer(
        textureId: Int,
        vertexBuffer: FloatBuffer,
        textureBuffer: FloatBuffer
    ): Int

    open fun initFrameBuffer(width: Int, height: Int) {
        if (frameBufferId != GLES30.GL_NONE
            && (frameWidth != width || frameHeight != height)
        ) {
            destroyFrameBuffer()
        }
        frameWidth = (width * scale).toInt()
        frameHeight = (height * scale).toInt()
        val frameBuffers = IntArray(1)
        val frameBufferTextures = IntArray(1)
        GLHelper.createFrameBuffer(frameBuffers, frameBufferTextures, frameWidth, frameHeight)
        frameBufferId = frameBuffers[0]
        frameBufferTextureId = frameBufferTextures[0]
    }

    private fun destroyFrameBuffer() {
        if (frameBufferTextureId != GLES30.GL_NONE) {
            GLES30.glDeleteTextures(1, intArrayOf(frameBufferTextureId), 0)
            frameBufferTextureId = GLES30.GL_NONE
        }
        if (frameBufferId != GLES30.GL_NONE) {
            GLES30.glDeleteFramebuffers(1, intArrayOf(frameBufferId), 0)
            frameBufferId = GLES30.GL_NONE
        }
        frameWidth = 0
        frameHeight = 0
    }

    open fun destroy() {
        if (program > 0) {
            GLES30.glDeleteProgram(program)
            program = 0
        }
        destroyFrameBuffer()
    }
}