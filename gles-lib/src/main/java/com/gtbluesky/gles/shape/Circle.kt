package com.gtbluesky.gles.shape

import android.opengl.GLES30
import android.opengl.Matrix
import com.gtbluesky.gles.util.GLHelper
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.sin

class Circle : BaseShape() {

    private var colorHandle = GLES30.GL_NONE
    private lateinit var colorBuf: FloatBuffer

    companion object {
        private const val VERTEX_NUM = 360
    }

    override fun init() {
        GLES30.glClearColor(0.2f, 0.6f, 1f, 0f)

        vertexShader = """
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

        initVetices()

        val color = floatArrayOf(0f, 1f, 0f, 1f)
        colorBuf = GLHelper.createFloatBuffer(color)
    }
    
    private fun initVetices() {
        val vertices = FloatArray((VERTEX_NUM + 2) * 2)
        vertexNum = VERTEX_NUM

        val radian = 2 * Math.PI / VERTEX_NUM

        val radius = 0.5f

        // 中心点
        vertices[0] = 0f
        vertices[1] = 0f

        for (i in 0 until VERTEX_NUM) {
            vertices[2 * (i + 1)] = (radius * cos(radian * i)).toFloat()
            vertices[2 * (i + 1) + 1] = (radius * sin(radian * i)).toFloat()
        }

        // 尾点
        vertices[(VERTEX_NUM + 1) * 2] = (radius * cos(radian)).toFloat()
        vertices[(VERTEX_NUM + 1) * 2 + 1] = (radius * sin(radian)).toFloat()

        verticesBuf = GLHelper.createFloatBuffer(vertices)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f)
    }

    override fun change(width: Int, height: Int) {
        this.width = width
        this.height = height
        GLES30.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.toFloat()
        Matrix.orthoM(projectionMatrix, 0, -aspect, aspect, -1f, 1f, 0f, 2f)
    }

    override fun drawFrame() {

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glUseProgram(program)

        GLES30.glVertexAttribPointer(positionHandle, 2, GLES30.GL_FLOAT, false, 0, verticesBuf)
        GLES30.glEnableVertexAttribArray(positionHandle)

        GLES30.glVertexAttrib4fv(colorHandle, colorBuf)

        GLES30.glUniformMatrix4fv(modelMatrixHandle, 1, false, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(viewMatrixHandle, 1, false, viewMatrix, 0)
        GLES30.glUniformMatrix4fv(projectionMatrixHandle, 1, false, projectionMatrix, 0)

        // 圆饼
//        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, vertices.size / 2)
        // 圆环
        GLES30.glDrawArrays(GLES30.GL_LINE_LOOP, 1, vertexNum)

        GLES30.glDisableVertexAttribArray(positionHandle)
        GLES30.glUseProgram(GLES30.GL_NONE)
    }
}