package com.gtbluesky.gles.shape

import android.opengl.GLES30
import android.opengl.Matrix
import com.gtbluesky.gles.util.GLHelper
import kotlin.math.cos
import kotlin.math.sin

/**
 * https://www.jianshu.com/p/c127387cd504
 */

class Sphere : BaseShape() {

    private var radius = 1f
    private var angleSpan = Math.PI / 90

    override fun init() {
        GLES30.glClearColor(0.2f, 0.6f, 1f, 1f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_CULL_FACE)

        vertexShader = """
            attribute vec4 aPosition;
            varying vec4 vPosition;
            uniform mat4 uModelMatrix;
            uniform mat4 uViewMatrix;
            uniform mat4 uProjectionMatrix;
            
            void main() {
                gl_Position = uProjectionMatrix * uViewMatrix * uModelMatrix * aPosition;
                vPosition = aPosition;
            }
        """

        fragmentShader = """
            precision mediump float;
            varying vec4 vPosition;
            
            void main() {
                //球的半径
                float uR = 0.6;
                vec4 color;
                //分为n层n列n行
                float n = 8.0;
                //正方形长度
                float span = 2.0 * uR / n;
                //计算行列层数
                //行数
                int i = int((vPosition.x + uR) / span);
                //层数
                int j = int((vPosition.y + uR) / span);
                //列数
                int k = int((vPosition.z + uR) / span);
                int colorType = int(mod(float(i + j + k), 2.0));
                if(colorType == 1) {
                    //奇数时为绿色
                    color = vec4(0.2, 1.0, 0.129, 0);
                } else {
                    //偶数时为白色
                    color = vec4(1.0, 1.0, 1.0, 0);
                }
                //将计算出的颜色给此片元
                gl_FragColor = color;
            }
        """

        program = GLHelper.createProgram(vertexShader, fragmentShader)

        positionHandle = GLES30.glGetAttribLocation(program, "aPosition")
        modelMatrixHandle = GLES30.glGetUniformLocation(program, "uModelMatrix")
        viewMatrixHandle = GLES30.glGetUniformLocation(program, "uViewMatrix")
        projectionMatrixHandle = GLES30.glGetUniformLocation(program, "uProjectionMatrix")

        initSphereVertex()

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.setLookAtM(
            viewMatrix,
            0,
            0f, 0f, 50f,
            0f, 0f, 0f,
            0f, 1f, 0f
        )

    }

    private fun initSphereVertex() {
        val vertex = ArrayList<Float>()

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

                vertex.add(x1)
                vertex.add(y1)
                vertex.add(z1)

                vertex.add(x3)
                vertex.add(y3)
                vertex.add(z3)

                vertex.add(x0)
                vertex.add(y0)
                vertex.add(z0)

                vertex.add(x1)
                vertex.add(y1)
                vertex.add(z1)

                vertex.add(x2)
                vertex.add(y2)
                vertex.add(z2)

                vertex.add(x3)
                vertex.add(y3)
                vertex.add(z3)

                hAngle += angleSpan
            }
            vAngle += angleSpan
        }

        val vertices = vertex.toFloatArray()
        vertexNum = vertices.size / 3
        verticesBuf = GLHelper.createFloatBuffer(vertices)
    }

    override fun change(width: Int, height: Int) {
        this.width = width
        this.height = height
        GLES30.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.toFloat()
        Matrix.frustumM(
            projectionMatrix,
            0,
            -aspect, aspect,
            -1f, 1f,
            20f, 100f
        )
    }

    override fun drawFrame() {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        GLES30.glUseProgram(program)

        GLES30.glVertexAttribPointer(positionHandle, 3, GLES30.GL_FLOAT, false, 0, verticesBuf)
        GLES30.glEnableVertexAttribArray(positionHandle)

        GLES30.glUniformMatrix4fv(modelMatrixHandle, 1, false, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(viewMatrixHandle, 1, false, viewMatrix, 0)
        GLES30.glUniformMatrix4fv(projectionMatrixHandle, 1, false, projectionMatrix, 0)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, vertexNum)

        GLES30.glDisableVertexAttribArray(positionHandle)
        GLES30.glUseProgram(GLES30.GL_NONE)
    }

    fun rotate(rotate: Float, x: Float, y: Float, z: Float) {
        Matrix.rotateM(modelMatrix, 0, rotate, x, y, z)
    }
}