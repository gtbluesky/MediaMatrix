package com.github.gtbluesky.gles.filter

import android.opengl.GLES30
import com.github.gtbluesky.gles.util.GLHelper
import java.nio.FloatBuffer

abstract class BaseFilter {
    protected var vertexShader = VERTEX_SHADER
    protected var fragmentShader = FRAGMENT_SHADER
    // 纹理单元
    protected val textureUnit = 0
    protected var textureType = GLES30.GL_TEXTURE_2D

    protected var positionHandle = GLES30.GL_NONE
    protected var textureCoordHandle = GLES30.GL_NONE
    protected var mvpMatrixHandle = GLES30.GL_NONE
    protected var textureUnitHandle = GLES30.GL_NONE

    protected val mvpMatrix = FloatArray(GL_MATRIX_SIZE)
    // 纹理宽高
    var width = 0
    var height = 0
    protected var program = GLES30.GL_NONE
    // FBO宽高
    protected var frameWidth = 0
    protected var frameHeight = 0
    // FBO
    protected var frameBufferId = GLES30.GL_NONE
    protected var frameBufferTextureId = GLES30.GL_NONE

    companion object {
        const val GL_MATRIX_SIZE = 16
        const val VERTEX_SHADER = """
            attribute vec4 aPosition;
            attribute vec4 aTextureCoord;
            varying vec2 vTextureCoord;
            uniform mat4 uMVPMatrix;
            void main() {
                gl_Position = uMVPMatrix * aPosition;
                vTextureCoord = aTextureCoord.xy;
            }
        """

        const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D uTextureUnit;
            varying vec2 vTextureCoord;
            void main() {
                gl_FragColor = texture2D(uTextureUnit, vTextureCoord);
            }
        """
    }

    constructor() {
        vertexShader = VERTEX_SHADER
        fragmentShader = FRAGMENT_SHADER
    }

    constructor(vertexShader: String, fragmentShader: String) {
        this.vertexShader = vertexShader
        this.fragmentShader = fragmentShader
    }

    abstract fun init()

    abstract fun change(width: Int, height: Int)

    abstract fun drawFrame(
        textureId: Int,
        vertexBuffer: FloatBuffer,
        textureBuffer: FloatBuffer
    )

    abstract fun drawFrameBuffer(
        textureId: Int,
        vertexBuffer: FloatBuffer,
        textureBuffer: FloatBuffer
    ): Int

    fun initFrameBuffer(width: Int, height: Int) {
        if (frameBufferId != GLES30.GL_NONE
            && (frameWidth != width || frameHeight != height)
        ) {
            destroyFrameBuffer()
        }
        frameWidth = width
        frameHeight = height
        val frameBuffers = IntArray(1)
        val frameBufferTextures = IntArray(1)
        GLHelper.createFrameBuffer(frameBuffers, frameBufferTextures, width, height)
        frameBufferId = frameBuffers[0]
        frameBufferTextureId = frameBufferTextures[0]
    }

    private fun destroyFrameBuffer() {
        if (frameBufferTextureId != GLES30.GL_NONE) {
            GLES30.glDeleteTextures(1, intArrayOf(frameBufferTextureId), 0)
            frameBufferTextureId = GLES30.GL_NONE
        }
        if (frameBufferId != GLES30.GL_NONE) {
            GLES30.glDeleteFramebuffers(0, intArrayOf(frameBufferId), 0)
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