package com.gtbluesky.mediamatrix.player.view

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import com.gtbluesky.mediamatrix.player.util.GLES2Util.buildProgramFromResource
import com.gtbluesky.player.R
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class VideoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(
    context, attrs
), GLSurfaceView.Renderer {
    private val vertexData = floatArrayOf(
        -1f, -1f,
        1f, -1f,
        -1f, 1f,
        1f, 1f
    )
    private val textureData = floatArrayOf(
        0f, 1f,
        1f, 1f,
        0f, 0f,
        1f, 0f
    )
    private val vertexBuffer: FloatBuffer
    private val textureBuffer: FloatBuffer
    private var programYuv = 0
    private var avPositionYuv = 0
    private var afPositionYuv = 0
    private var samplerY = 0
    private var samplerU = 0
    private var samplerV = 0
    private var textureIdYuv = IntArray(3)
    private var widthYuv = 0
    private var heightYuv = 0
    private var y: ByteBuffer? = null
    private var u: ByteBuffer? = null
    private var v: ByteBuffer? = null
    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        initRenderYUV()
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        renderYUV()
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }

    private fun initRenderYUV() {
        programYuv = buildProgramFromResource(context, R.raw.vertex_shader, R.raw.fragment_shader)
        avPositionYuv = GLES20.glGetAttribLocation(programYuv, "av_Position")
        afPositionYuv = GLES20.glGetAttribLocation(programYuv, "af_Position")
        samplerY = GLES20.glGetUniformLocation(programYuv, "sampler_y")
        samplerU = GLES20.glGetUniformLocation(programYuv, "sampler_u")
        samplerV = GLES20.glGetUniformLocation(programYuv, "sampler_v")
        GLES20.glGenTextures(3, textureIdYuv, 0)
        for (i in 0..2) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIdYuv[i])
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR
            )
        }
    }

    fun setYUVRenderData(width: Int, height: Int, y: ByteArray, u: ByteArray, v: ByteArray) {
        widthYuv = width
        heightYuv = height
        this.y = ByteBuffer.wrap(y)
        this.u = ByteBuffer.wrap(u)
        this.v = ByteBuffer.wrap(v)
        requestRender()
    }

    private fun renderYUV() {
        if (widthYuv > 0 && heightYuv > 0 && y != null && u != null && v != null) {
            GLES20.glUseProgram(programYuv)
            GLES20.glEnableVertexAttribArray(avPositionYuv)
            GLES20.glVertexAttribPointer(avPositionYuv, 2, GLES20.GL_FLOAT, false, 8, vertexBuffer)
            GLES20.glEnableVertexAttribArray(afPositionYuv)
            GLES20.glVertexAttribPointer(
                afPositionYuv,
                2,
                GLES20.GL_FLOAT,
                false,
                8,
                textureBuffer
            )
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIdYuv[0])
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                GLES20.GL_LUMINANCE,
                widthYuv,
                heightYuv,
                0,
                GLES20.GL_LUMINANCE,
                GLES20.GL_UNSIGNED_BYTE,
                y
            )
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIdYuv[1])
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                GLES20.GL_LUMINANCE,
                widthYuv / 2,
                heightYuv / 2,
                0,
                GLES20.GL_LUMINANCE,
                GLES20.GL_UNSIGNED_BYTE,
                u
            )
            GLES20.glActiveTexture(GLES20.GL_TEXTURE2)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIdYuv[2])
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                GLES20.GL_LUMINANCE,
                widthYuv / 2,
                heightYuv / 2,
                0,
                GLES20.GL_LUMINANCE,
                GLES20.GL_UNSIGNED_BYTE,
                v
            )
            GLES20.glUniform1i(samplerY, 0)
            GLES20.glUniform1i(samplerU, 1)
            GLES20.glUniform1i(samplerV, 2)
            y?.clear()
            u?.clear()
            v?.clear()
            y = null
            u = null
            v = null
        }
    }

    init {
        setEGLContextClientVersion(2)
        setRenderer(this)
        renderMode = RENDERMODE_WHEN_DIRTY
        vertexBuffer = ByteBuffer.allocateDirect(vertexData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertexData)
        vertexBuffer.position(0)
        textureBuffer = ByteBuffer.allocateDirect(textureData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(textureData)
        textureBuffer.position(0)
    }
}