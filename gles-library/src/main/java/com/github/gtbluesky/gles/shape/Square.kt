package com.github.gtbluesky.gles.shape

import android.opengl.GLES30
import android.opengl.Matrix
import com.github.gtbluesky.gles.util.GLHelper
import java.nio.FloatBuffer

class Square : BaseShape() {

    private var colorHandle = GLES30.GL_NONE
    private lateinit var colorBuf: FloatBuffer

    override fun init() {
        vertexShader ="""
            attribute vec4 aPosition;
            attribute vec4 aColor;
            varying vec4 vColor;
            uniform mat4 uModelMatrix;
            uniform mat4 uViewMatrix;
            uniform mat4 uProjectionMatrix;
            void main() {
                gl_Position = uProjectionMatrix * uViewMatrix * uModelMatrix * aPosition;
                vColor = aColor;
            }
        """

        fragmentShader = """
            precision mediump float;
            varying vec4 vColor;
            void main() {
                gl_FragColor = vColor;
            }
        """

        program = GLHelper.createProgram(vertexShader, fragmentShader)

        positionHandle = GLES30.glGetAttribLocation(program, "aPosition")
        colorHandle = GLES30.glGetAttribLocation(program, "aColor")
        modelMatrixHandle = GLES30.glGetUniformLocation(program, "uModelMatrix")
        viewMatrixHandle = GLES30.glGetUniformLocation(program, "uViewMatrix")
        projectionMatrixHandle = GLES30.glGetUniformLocation(program, "uProjectionMatrix")

        val vertices = floatArrayOf(
            0.5f, 0.5f, 0f,
            -0.5f, 0.5f, 0f,
            0.5f, -0.5f, 0f,
            -0.5f, -0.5f, 0f
        )

        vertexNum = vertices.size / 3

        verticesBuf = GLHelper.createFloatBuffer(vertices)

        val color = floatArrayOf(
            1f, 0f, 0f, 1f
        )

        colorBuf = GLHelper.createFloatBuffer(color)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f)
    }

    override fun change(width: Int, height: Int) {
        this.width = width
        this.height = height
        GLES30.glViewport(0, 0, width, height)
        GLES30.glClearColor(0f ,0f, 0f, 0f)
        val aspect = width.toFloat() / height.toFloat()
        Matrix.orthoM(projectionMatrix, 0, -aspect, aspect, -1f, 1f, 0.5f, 1f)
    }

    override fun drawFrame() {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glUseProgram(program)

        GLES30.glVertexAttribPointer(positionHandle, 3, GLES30.GL_FLOAT, false, 0, verticesBuf)
        GLES30.glEnableVertexAttribArray(positionHandle)

        GLES30.glVertexAttrib4fv(colorHandle, colorBuf)

        GLES30.glUniformMatrix4fv(modelMatrixHandle, 1, false, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(viewMatrixHandle, 1, false, viewMatrix, 0)
        GLES30.glUniformMatrix4fv(projectionMatrixHandle, 1, false, projectionMatrix, 0)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, vertexNum)

        GLES30.glDisableVertexAttribArray(positionHandle)
        GLES30.glUseProgram(GLES30.GL_NONE)
    }
}