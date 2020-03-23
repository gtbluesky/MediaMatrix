package com.gtbluesky.gles.shape

import android.opengl.GLES30
import android.opengl.Matrix
import com.gtbluesky.gles.util.GLHelper
import java.nio.FloatBuffer

class BlendShape : BaseShape() {

    private val textureUnit = 0
    var textureId = GLES30.GL_NONE
    private var textureCoordinateHandle = GLES30.GL_NONE
    private var textureUnitHandle = GLES30.GL_NONE

    private lateinit var textureCoordsBuf: FloatBuffer

    override fun init() {

        vertexShader = """
            attribute vec4 aPosition;
            attribute vec2 aTextureCoord;
            varying vec2 vTextureCoord;
            uniform mat4 uModelMatrix;
            uniform mat4 uViewMatrix;
            uniform mat4 uProjectionMatrix;
            void main() {
                gl_Position = uProjectionMatrix * uViewMatrix * uModelMatrix * aPosition;
                vTextureCoord = aTextureCoord;
            }
        """

        fragmentShader = """
            precision mediump float;
            uniform sampler2D uTextureUnit;
            varying vec2 vTextureCoord;
            void main() {
                gl_FragColor = texture2D(uTextureUnit, vTextureCoord);
            }
        """

        program = GLHelper.createProgram(vertexShader, fragmentShader)

        positionHandle = GLES30.glGetAttribLocation(program, "aPosition")
        textureCoordinateHandle = GLES30.glGetAttribLocation(program, "aTextureCoord")
        textureUnitHandle = GLES30.glGetUniformLocation(program, "uTextureUnit")
        modelMatrixHandle = GLES30.glGetUniformLocation(program, "uModelMatrix")
        viewMatrixHandle = GLES30.glGetUniformLocation(program, "uViewMatrix")
        projectionMatrixHandle = GLES30.glGetUniformLocation(program, "uProjectionMatrix")

        val vertices = floatArrayOf(
            -1.0f,  1.0f,
            -1.0f, -1.0f,
            1.0f, 1.0f,
            1.0f,  -1.0f
        )

        val textureCoords = floatArrayOf(
            0.0f, 0.0f,
            0.0f,  1.0f,
            1.0f,  0.0f,
            1.0f, 1.0f
        )

        vertexNum = vertices.size / 2

        verticesBuf = GLHelper.createFloatBuffer(vertices)

        textureCoordsBuf = GLHelper.createFloatBuffer(textureCoords)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f)
    }

    override fun change(width: Int, height: Int) {
        this.width = width
        this.height = height
        val aspect = width.toFloat() / height.toFloat()
        Matrix.orthoM(projectionMatrix, 0, -aspect, aspect, -1f, 1f, 0.5f, 1f)
    }

    override fun drawFrame() {
        GLES30.glUseProgram(program)

        GLES30.glVertexAttribPointer(positionHandle, 2, GLES30.GL_FLOAT, false, 0, verticesBuf)
        GLES30.glEnableVertexAttribArray(positionHandle)

        GLES30.glVertexAttribPointer(textureCoordinateHandle, 2, GLES30.GL_FLOAT, false, 0, textureCoordsBuf)
        GLES30.glEnableVertexAttribArray(textureCoordinateHandle)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + textureUnit)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        // 为当前绑定的纹理设置环绕、过滤方式
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        // 为当前绑定的纹理自动生成所有需要的多级渐远纹理
//        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)

        GLES30.glUniform1i(textureUnitHandle, textureUnit)

        GLES30.glUniformMatrix4fv(modelMatrixHandle, 1, false, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(viewMatrixHandle, 1, false, viewMatrix, 0)
        GLES30.glUniformMatrix4fv(projectionMatrixHandle, 1, false, projectionMatrix, 0)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, vertexNum)

        GLES30.glDisableVertexAttribArray(positionHandle)
        GLES30.glDisableVertexAttribArray(textureCoordinateHandle)
        GLES30.glUseProgram(GLES30.GL_NONE)
    }
}