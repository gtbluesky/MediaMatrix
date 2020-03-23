package com.gtbluesky.gles.shape

import android.content.Context
import android.opengl.GLES30
import android.opengl.Matrix
import com.gtbluesky.gles.R
import com.gtbluesky.gles.util.GLHelper
import java.nio.ByteBuffer

/**
 * https://www.jianshu.com/p/820581046d3c
 */

class SkyBox(val context: Context) : BaseShape() {

    private val textureUnit = 0
    private var textureId = GLES30.GL_NONE
    private var textureUnitHandle = GLES30.GL_NONE

    private lateinit var indexBuf: ByteBuffer

    override fun init() {
        GLES30.glClearColor(0.2f, 0.6f, 1f, 1f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        
        vertexShader = """
            attribute vec3 aPosition;
            varying vec3 vPosition;
            uniform mat4 uModelMatrix;
            uniform mat4 uViewMatrix;
            uniform mat4 uProjectionMatrix;
            void main() {
                vPosition = aPosition;
                vPosition.z = -vPosition.z;
                gl_Position = (uProjectionMatrix * uViewMatrix * uModelMatrix * vec4(aPosition, 1.0)).xyww;
            }
        """

        fragmentShader = """
            precision mediump float;
            varying vec3 vPosition;
            uniform samplerCube uTextureUnit;
            void main() {
                gl_FragColor = textureCube(uTextureUnit, vPosition);
            }
        """

        program = GLHelper.createProgram(vertexShader, fragmentShader)

        textureId = GLHelper.createCubeTexture(
            context, intArrayOf(
            R.drawable.left, R.drawable.right,
            R.drawable.bottom, R.drawable.top,
            R.drawable.front, R.drawable.back)
        )

//        textureId = GLHelper.createCubeTexture(
//            context, intArrayOf(
//                R.drawable.bloody_heresy_lf, R.drawable.bloody_heresy_rt,
//                R.drawable.bloody_heresy_dn, R.drawable.bloody_heresy_up,
//                R.drawable.bloody_heresy_ft, R.drawable.bloody_heresy_bk)
//        )

        positionHandle = GLES30.glGetAttribLocation(program, "aPosition")
        textureUnitHandle = GLES30.glGetUniformLocation(program, "uTextureUnit")
        modelMatrixHandle = GLES30.glGetUniformLocation(program, "uModelMatrix")
        viewMatrixHandle = GLES30.glGetUniformLocation(program, "uViewMatrix")
        projectionMatrixHandle = GLES30.glGetUniformLocation(program, "uProjectionMatrix")

        val vertices = floatArrayOf(
            -1f,  1f,  1f,     // (0) Top-left near
            1f,   1f,  1f,     // (1) Top-right near
            -1f, -1f,  1f,     // (2) Bottom-left near
            1f,  -1f,  1f,     // (3) Bottom-right near
            -1f,  1f, -1f,     // (4) Top-left far
            1f,   1f, -1f,     // (5) Top-right far
            -1f, -1f, -1f,     // (6) Bottom-left far
            1f,  -1f, -1f      // (7) Bottom-right far
        )

        // GL_TRIANGLE_STRIP
        val indexes = byteArrayOf(
            // Front
            1, 3, 0,
            0, 3, 2,

            // Back
            4, 6, 5,
            5, 6, 7,

            // Left
            0, 2, 4,
            4, 2, 6,

            // Right
            5, 7, 1,
            1, 7, 3,

            // Top
            5, 1, 4,
            4, 1, 0,

            // Bottom
            6, 2, 7,
            7, 2, 3
        )

        vertexNum = vertices.size / 3
        verticesBuf = GLHelper.createFloatBuffer(vertices)

        indexBuf = GLHelper.createByteBuffer(indexes)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.rotateM(modelMatrix, 0 ,90f, 1f, 0f, 0f)
        Matrix.setLookAtM(
            viewMatrix,
            0,
            0f, 0f, 0f,
            0f, 0f, -1f,
            0f, 1f, 0f
        )
    }

    override fun change(width: Int, height: Int) {
        this.width = width
        this.height = height
        GLES30.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.toFloat()
        Matrix.perspectiveM(
            projectionMatrix,
            0,
            45f, aspect,
            1f, 300f
        )
    }

    override fun drawFrame() {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        GLES30.glUseProgram(program)

        GLES30.glVertexAttribPointer(positionHandle, 3, GLES30.GL_FLOAT, false, 0, verticesBuf)
        GLES30.glEnableVertexAttribArray(positionHandle)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + textureUnit)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_CUBE_MAP, textureId)

        GLES30.glUniform1i(textureUnitHandle, textureUnit)

        GLES30.glUniformMatrix4fv(modelMatrixHandle, 1, false, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(viewMatrixHandle, 1, false, viewMatrix, 0)
        GLES30.glUniformMatrix4fv(projectionMatrixHandle, 1, false, projectionMatrix, 0)

        GLES30.glDrawElements(GLES30.GL_TRIANGLES, 36, GLES30.GL_UNSIGNED_BYTE, indexBuf)
        GLES30.glDisableVertexAttribArray(positionHandle)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_CUBE_MAP, GLES30.GL_NONE)
        GLES30.glUseProgram(GLES30.GL_NONE)
    }

    fun setRotateMatrix(matrix: FloatArray) {
        System.arraycopy(matrix, 0, modelMatrix, 0, 16)
        Matrix.rotateM(modelMatrix, 0 ,90f, 1f, 0f, 0f)
    }
}