package com.gtbluesky.gles.shape

import android.content.Context
import android.opengl.GLES30
import android.opengl.Matrix
import com.gtbluesky.gles.R
import com.gtbluesky.gles.util.GLHelper
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.sin

/**
 * https://blog.csdn.net/junzia/article/details/72803124
 */

class SkySphere(val context: Context) : BaseShape() {

    private var radius = 2f
    private var angleSpan = Math.PI / 90
    private val textureUnit = 0
    private var textureId = GLES30.GL_NONE
    private var textureUnitHandle = GLES30.GL_NONE

    private var coordinateHandle = GLES30.GL_NONE

    private lateinit var coordBuf: FloatBuffer

    override fun init() {
        GLES30.glClearColor(0.2f, 0.6f, 1f, 1f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glCullFace(GLES30.GL_FRONT)
        
        vertexShader = """
            attribute vec4 aPosition;
            attribute vec2 aTextureCoord;
            varying vec2 vTextureCoord;
            uniform mat4 uModelMatrix;
            uniform mat4 uViewMatrix;
            uniform mat4 uProjectionMatrix;
            void main() {
                vTextureCoord = aTextureCoord;
                gl_Position = uProjectionMatrix * uViewMatrix * uModelMatrix * aPosition;
            }
        """

        fragmentShader = """
            precision highp float;
            varying vec2 vTextureCoord;
            uniform sampler2D uTextureUnit;
            void main() {
                gl_FragColor = texture2D(uTextureUnit, vTextureCoord);
            }
        """

        program = GLHelper.createProgram(vertexShader, fragmentShader)

        textureId = GLHelper.createTexture(GLES30.GL_TEXTURE_2D, context, R.drawable.vr2)

        positionHandle = GLES30.glGetAttribLocation(program, "aPosition")
        coordinateHandle = GLES30.glGetAttribLocation(program, "aTextureCoord")
        textureUnitHandle = GLES30.glGetUniformLocation(program, "uTextureUnit")
        modelMatrixHandle = GLES30.glGetUniformLocation(program, "uModelMatrix")
        viewMatrixHandle = GLES30.glGetUniformLocation(program, "uViewMatrix")
        projectionMatrixHandle = GLES30.glGetUniformLocation(program, "uProjectionMatrix")

        initSphereVertex()

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.setLookAtM(
            viewMatrix,
            0,
            0f, 0f, 0f,
            0f, 0f, -1f,
            0f, 1f, 0f
        )
    }

    private fun initSphereVertex() {
        val vertexList = ArrayList<Float>()
        val textureVertexList = ArrayList<Float>()

        var vAngle = 0.0
        while (vAngle < Math.PI) {
            // vertical
            var hAngle = 0.0
            while (hAngle < 2 * Math.PI) {
                // horizontal
                val x0 = (radius * sin(vAngle) * cos(hAngle)).toFloat()
                val y0 = (radius * sin(vAngle) * sin(hAngle)).toFloat()
                val z0 = (radius * cos(vAngle)).toFloat()

                val x1 = (radius * sin(vAngle) * cos(hAngle + angleSpan)).toFloat()
                val y1 = (radius * sin(vAngle) * sin(hAngle + angleSpan)).toFloat()
                val z1 = (radius * cos(vAngle)).toFloat()

                val x2 = (radius * sin(vAngle + angleSpan) * cos(hAngle + angleSpan)).toFloat()
                val y2 = (radius * sin(vAngle + angleSpan) * sin(hAngle + angleSpan)).toFloat()
                val z2 = (radius * cos(vAngle + angleSpan)).toFloat()

                val x3 = (radius * sin(vAngle + angleSpan) * cos(hAngle)).toFloat()
                val y3 = (radius * sin(vAngle + angleSpan) * sin(hAngle)).toFloat()
                val z3 = (radius * cos(vAngle + angleSpan)).toFloat()

                val s0 = (hAngle / Math.PI / 2).toFloat()
                val s1 = ((hAngle + angleSpan) / Math.PI / 2).toFloat()
                val t0 = (vAngle / Math.PI).toFloat()
                val t1 = ((vAngle + angleSpan) / Math.PI).toFloat()

                vertexList.add(x1)
                vertexList.add(y1)
                vertexList.add(z1)

                vertexList.add(x0)
                vertexList.add(y0)
                vertexList.add(z0)

                vertexList.add(x3)
                vertexList.add(y3)
                vertexList.add(z3)

                vertexList.add(x1)
                vertexList.add(y1)
                vertexList.add(z1)

                vertexList.add(x3)
                vertexList.add(y3)
                vertexList.add(z3)

                vertexList.add(x2)
                vertexList.add(y2)
                vertexList.add(z2)

                textureVertexList.add(s1)// x1 y1对应纹理坐标
                textureVertexList.add(t0)
                textureVertexList.add(s0)// x0 y0对应纹理坐标
                textureVertexList.add(t0)
                textureVertexList.add(s0)// x3 y3对应纹理坐标
                textureVertexList.add(t1)

                textureVertexList.add(s1)// x1 y1对应纹理坐标
                textureVertexList.add(t0)
                textureVertexList.add(s0)// x3 y3对应纹理坐标
                textureVertexList.add(t1)
                textureVertexList.add(s1)// x2 y3对应纹理坐标
                textureVertexList.add(t1)

                hAngle += angleSpan
            }
            vAngle += angleSpan
        }

        val vertices = vertexList.toFloatArray()
        val coords = textureVertexList.toFloatArray()
        vertexNum = vertices.size / 3
        verticesBuf = GLHelper.createFloatBuffer(vertices)
        coordBuf = GLHelper.createFloatBuffer(coords)
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

        GLES30.glVertexAttribPointer(coordinateHandle, 2, GLES30.GL_FLOAT, false, 0, coordBuf)
        GLES30.glEnableVertexAttribArray(coordinateHandle)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + textureUnit)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)

        GLES30.glUniform1i(textureUnitHandle, textureUnit)

        GLES30.glUniformMatrix4fv(modelMatrixHandle, 1, false, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(viewMatrixHandle, 1, false, viewMatrix, 0)
        GLES30.glUniformMatrix4fv(projectionMatrixHandle, 1, false, projectionMatrix, 0)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, vertexNum)
        GLES30.glDisableVertexAttribArray(positionHandle)
        GLES30.glDisableVertexAttribArray(coordinateHandle)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, GLES30.GL_NONE)
        GLES30.glUseProgram(GLES30.GL_NONE)
    }

    fun setRotateMatrix(matrix: FloatArray) {
        System.arraycopy(matrix, 0, modelMatrix, 0, 16)
    }
}