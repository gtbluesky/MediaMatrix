package com.github.gtbluesky.gles.shape

import android.opengl.GLES30
import android.opengl.Matrix
import com.github.gtbluesky.gles.util.GLHelper
import java.nio.ByteBuffer
import java.nio.FloatBuffer

class Cube : BaseShape() {

    private var colorHandle = GLES30.GL_NONE
    private lateinit var indexes: ByteArray
    private lateinit var topIndex: ByteArray
    private lateinit var bottomIndex: ByteArray
    private lateinit var colorBuf: FloatBuffer
    private lateinit var indexBuf: ByteBuffer
    private lateinit var topIndexBuf: ByteBuffer
    private lateinit var bottomIndexBuf: ByteBuffer

    override fun init() {
        GLES30.glClearColor(0.2f, 0.6f, 1f, 1f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        
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

        val vertices = floatArrayOf(
            -1.0f,1.0f,1.0f,    //正面左上0
            -1.0f,-1.0f,1.0f,   //正面左下1
            1.0f,-1.0f,1.0f,    //正面右下2
            1.0f,1.0f,1.0f,     //正面右上3
            -1.0f,1.0f,-1.0f,    //反面左上4
            -1.0f,-1.0f,-1.0f,   //反面左下5
            1.0f,-1.0f,-1.0f,    //反面右下6
            1.0f,1.0f,-1.0f     //反面右上7
        )

        // GL_TRIANGLES
//        indexes = byteArrayOf(
//            6,7,4,6,4,5,    //后面
//            6,3,7,6,2,3,    //右面
//            6,5,1,6,1,2,    //下面
//            0,3,2,0,2,1,    //正面
//            0,1,5,0,5,4,    //左面
//            0,7,3,0,4,7    //上面
//        )

        // GL_TRIANGLE_STRIP
        indexes = byteArrayOf(
            0, 1, 3, 2, //前
            7, 6, //右
            4, 5, //后
            0, 1 //左
        )
        topIndex = byteArrayOf(0, 3, 4, 7)
        bottomIndex = byteArrayOf(1, 2, 5, 6)

        vertexNum = vertices.size / 3
        verticesBuf = GLHelper.createFloatBuffer(vertices)

        val color = floatArrayOf(
            0f,1f,0f,1f,
            0f,1f,0f,1f,
            0f,1f,0f,1f,
            0f,1f,0f,1f,
            1f,0f,0f,1f,
            1f,0f,0f,1f,
            1f,0f,0f,1f,
            1f,0f,0f,1f
        )

        colorBuf = GLHelper.createFloatBuffer(color)
        indexBuf = GLHelper.createByteBuffer(indexes)
        topIndexBuf = GLHelper.createByteBuffer(topIndex)
        bottomIndexBuf = GLHelper.createByteBuffer(bottomIndex)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.setLookAtM(viewMatrix, 0, 5f, 5f, 10f, 0f, 0f, 0f, 0f, 1f, 0f)
    }

    override fun change(width: Int, height: Int) {
        this.width = width
        this.height = height
        GLES30.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.toFloat()
        Matrix.frustumM(projectionMatrix, 0, -aspect, aspect, -1f, 1f, 3f, 20f)
    }

    override fun drawFrame() {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        GLES30.glUseProgram(program)

        GLES30.glVertexAttribPointer(positionHandle, 3, GLES30.GL_FLOAT, false, 0, verticesBuf)
        GLES30.glEnableVertexAttribArray(positionHandle)

        GLES30.glVertexAttribPointer(colorHandle, 4, GLES30.GL_FLOAT, false, 0, colorBuf)
        GLES30.glEnableVertexAttribArray(colorHandle)

        GLES30.glUniformMatrix4fv(modelMatrixHandle, 1, false, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(viewMatrixHandle, 1, false, viewMatrix, 0)
        GLES30.glUniformMatrix4fv(projectionMatrixHandle, 1, false, projectionMatrix, 0)

        GLES30.glEnable(GLES30.GL_SCISSOR_TEST)
        GLES30.glScissor(100, 100, 300, 400)
        GLES30.glClearColor(1.0f, 0.0f, 0.0f, 1.0f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        GLES30.glDisable(GLES30.GL_SCISSOR_TEST)

//        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexes.size, GLES30.GL_UNSIGNED_BYTE, indexBuf)
        GLES30.glDrawElements(GLES30.GL_TRIANGLE_STRIP, indexes.size, GLES30.GL_UNSIGNED_BYTE, indexBuf)
        GLES30.glDrawElements(GLES30.GL_TRIANGLE_STRIP, topIndex.size, GLES30.GL_UNSIGNED_BYTE, topIndexBuf)
        GLES30.glDrawElements(GLES30.GL_TRIANGLE_STRIP, bottomIndex.size, GLES30.GL_UNSIGNED_BYTE, bottomIndexBuf)

        GLES30.glDisableVertexAttribArray(positionHandle)
        GLES30.glDisableVertexAttribArray(colorHandle)
        GLES30.glUseProgram(GLES30.GL_NONE)
    }
}