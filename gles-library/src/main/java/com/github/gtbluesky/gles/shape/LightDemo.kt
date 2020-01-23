package com.github.gtbluesky.gles.shape

import android.opengl.GLES30
import android.opengl.Matrix
import com.github.gtbluesky.gles.util.GLHelper

class LightDemo : BaseShape() {
    private var lightPosHandle = GLES30.GL_NONE
    private var viewPosHandle = GLES30.GL_NONE
    private var lightColorHandle = GLES30.GL_NONE
    private var objectColorHandle = GLES30.GL_NONE
    private var normalHandle = GLES30.GL_NONE

    private val vao = IntArray(1)
    private val vbo = IntArray(1)

    override fun init() {
        GLES30.glClearColor(0.2f, 0.6f, 1f, 1f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)

        vertexShader = """
            attribute vec3 aPosition;
            attribute vec3 aNormal;
            
            varying vec3 vPosition;
            varying vec3 vNormal;
            
            uniform mat4 uModelMatrix;
            uniform mat4 uViewMatrix;
            uniform mat4 uProjectionMatrix;
            
            void main() {
                vPosition = vec3(uModelMatrix * vec4(aPosition, 1.0));
                //vNormal = mat3(transpose(inverse(uModelMatrix))) * aNormal;
                vNormal = aNormal;
                gl_Position = uProjectionMatrix * uViewMatrix * vec4(vPosition, 1.0);
            }
        """

        fragmentShader = """
            precision mediump float;
            
            varying vec3 vPosition;
            varying vec3 vNormal;
            
            // world space
            uniform vec3 uLightPosition;
            uniform vec3 uViewPosition;
            uniform vec3 uLightColor;
            uniform vec3 uObjectColor;
            
            void main() {
                // ambient
                float ambientStrength = 0.1;
                vec3 ambient = ambientStrength * uLightColor;
                
                // diffuse
                vec3 normal = normalize(vNormal);
                vec3 lightDirection = normalize(uLightPosition - vPosition);
                float diff = max(dot(normal, lightDirection), 0.0);
                vec3 diffuse = diff * uLightColor;
                
                // specular
                float specularStrength = 0.5;
                vec3 viewDirection = normalize(uViewPosition - vPosition);
                vec3 reflectDirection = reflect(-lightDirection, normal);
                float spec = pow(max(dot(viewDirection, reflectDirection), 0.0), 32.0);
                vec3 specular = specularStrength * spec * uLightColor;
                        
                vec3 result = (ambient + diffuse + specular) * uObjectColor;
                gl_FragColor = vec4(result, 1.0);
            }
        """

        program = GLHelper.createProgram(vertexShader, fragmentShader)

        lightPosHandle = GLES30.glGetUniformLocation(program, "uLightPosition")
        viewPosHandle = GLES30.glGetUniformLocation(program, "uViewPosition")
        lightColorHandle = GLES30.glGetUniformLocation(program, "uLightColor")
        objectColorHandle = GLES30.glGetUniformLocation(program, "uObjectColor")
        positionHandle = GLES30.glGetAttribLocation(program, "aPosition")
        modelMatrixHandle = GLES30.glGetUniformLocation(program, "uModelMatrix")
        viewMatrixHandle = GLES30.glGetUniformLocation(program, "uViewMatrix")
        projectionMatrixHandle = GLES30.glGetUniformLocation(program, "uProjectionMatrix")
        normalHandle = GLES30.glGetAttribLocation(program, "aNormal")

        val vertices = floatArrayOf(
            //左三为顶点坐标，右三为该面的法向量
            -0.5f, -0.5f, -0.5f,  0.0f,  0.0f, -1.0f,
            0.5f, -0.5f, -0.5f,  0.0f,  0.0f, -1.0f,
            0.5f,  0.5f, -0.5f,  0.0f,  0.0f, -1.0f,
            0.5f,  0.5f, -0.5f,  0.0f,  0.0f, -1.0f,
            -0.5f,  0.5f, -0.5f,  0.0f,  0.0f, -1.0f,
            -0.5f, -0.5f, -0.5f,  0.0f,  0.0f, -1.0f,

            -0.5f, -0.5f,  0.5f,  0.0f,  0.0f,  1.0f,
            0.5f, -0.5f,  0.5f,  0.0f,  0.0f,  1.0f,
            0.5f,  0.5f,  0.5f,  0.0f,  0.0f,  1.0f,
            0.5f,  0.5f,  0.5f,  0.0f,  0.0f,  1.0f,
            -0.5f,  0.5f,  0.5f,  0.0f,  0.0f,  1.0f,
            -0.5f, -0.5f,  0.5f,  0.0f,  0.0f,  1.0f,

            -0.5f,  0.5f,  0.5f, -1.0f,  0.0f,  0.0f,
            -0.5f,  0.5f, -0.5f, -1.0f,  0.0f,  0.0f,
            -0.5f, -0.5f, -0.5f, -1.0f,  0.0f,  0.0f,
            -0.5f, -0.5f, -0.5f, -1.0f,  0.0f,  0.0f,
            -0.5f, -0.5f,  0.5f, -1.0f,  0.0f,  0.0f,
            -0.5f,  0.5f,  0.5f, -1.0f,  0.0f,  0.0f,

            0.5f,  0.5f,  0.5f,  1.0f,  0.0f,  0.0f,
            0.5f,  0.5f, -0.5f,  1.0f,  0.0f,  0.0f,
            0.5f, -0.5f, -0.5f,  1.0f,  0.0f,  0.0f,
            0.5f, -0.5f, -0.5f,  1.0f,  0.0f,  0.0f,
            0.5f, -0.5f,  0.5f,  1.0f,  0.0f,  0.0f,
            0.5f,  0.5f,  0.5f,  1.0f,  0.0f,  0.0f,

            -0.5f, -0.5f, -0.5f,  0.0f, -1.0f,  0.0f,
            0.5f, -0.5f, -0.5f,  0.0f, -1.0f,  0.0f,
            0.5f, -0.5f,  0.5f,  0.0f, -1.0f,  0.0f,
            0.5f, -0.5f,  0.5f,  0.0f, -1.0f,  0.0f,
            -0.5f, -0.5f,  0.5f,  0.0f, -1.0f,  0.0f,
            -0.5f, -0.5f, -0.5f,  0.0f, -1.0f,  0.0f,

            -0.5f,  0.5f, -0.5f,  0.0f,  1.0f,  0.0f,
            0.5f,  0.5f, -0.5f,  0.0f,  1.0f,  0.0f,
            0.5f,  0.5f,  0.5f,  0.0f,  1.0f,  0.0f,
            0.5f,  0.5f,  0.5f,  0.0f,  1.0f,  0.0f,
            -0.5f,  0.5f,  0.5f,  0.0f,  1.0f,  0.0f,
            -0.5f,  0.5f, -0.5f,  0.0f,  1.0f,  0.0f
        )

        vertexNum = vertices.size / 3

        verticesBuf = GLHelper.createFloatBuffer(vertices)

        GLES30.glGenVertexArrays(1, vao, 0)
        GLES30.glGenBuffers(1, vbo, 0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertexNum * 3 * 4, verticesBuf, GLES30.GL_STATIC_DRAW)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.setLookAtM(viewMatrix, 0, 3f, 3f, 3f, 0f, 0f, 0f, 0f, 1f, 0f)
    }

    override fun change(width: Int, height: Int) {
        this.width = width
        this.height = height
        GLES30.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.toFloat()
        Matrix.frustumM(projectionMatrix, 0, -aspect, aspect, -1f, 1f, 2f, 10f)
    }

    override fun drawFrame() {

        GLES30.glUseProgram(program)

        GLES30.glBindVertexArray(vao[0])

        GLES30.glVertexAttribPointer(positionHandle, 3, GLES30.GL_FLOAT, false, 6 * 4, 0)
        GLES30.glEnableVertexAttribArray(positionHandle)

        GLES30.glVertexAttribPointer(normalHandle, 3, GLES30.GL_FLOAT, false, 6 * 4, 3 * 4)
        GLES30.glEnableVertexAttribArray(normalHandle)

        GLES30.glBindVertexArray(GLES30.GL_NONE)

        // render loop start

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        GLES30.glUniform3fv(lightPosHandle, 1, floatArrayOf(0f, 10f, -3f), 0)
        GLES30.glUniform3fv(viewPosHandle, 1, floatArrayOf(3f, 3f, 3f), 0)
        GLES30.glUniform3fv(lightColorHandle, 1, floatArrayOf(1f, 1f, 1f), 0)
        GLES30.glUniform3fv(objectColorHandle, 1, floatArrayOf(1f, 0.5f, 0.31f), 0)

        GLES30.glUniformMatrix4fv(modelMatrixHandle, 1, false, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(viewMatrixHandle, 1, false, viewMatrix, 0)
        GLES30.glUniformMatrix4fv(projectionMatrixHandle, 1, false, projectionMatrix, 0)

        // 可见使用了 vao 可以简化每次对顶点数据传值
        GLES30.glBindVertexArray(vao[0])
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 36)
        GLES30.glBindVertexArray(GLES30.GL_NONE)

        // render loop end
        GLES30.glUseProgram(GLES30.GL_NONE)
        GLES30.glDeleteVertexArrays(1, vao, 0)
        GLES30.glDeleteBuffers(1, vbo, 0)

    }
}