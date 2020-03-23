package com.gtbluesky.gles.shape

import android.content.Context
import android.graphics.Color
import android.opengl.GLES30
import android.opengl.Matrix
import com.gtbluesky.gles.R
import com.gtbluesky.gles.util.GLHelper
import kotlin.random.Random

/**
 * https://www.jianshu.com/p/0831b40a0bc9
 */
class ParticleEffect(val context: Context) : BaseShape() {

    private val textureUnit = GLES30.GL_NONE
    private var textureId = GLES30.GL_NONE
    private var textureUnitHandle = GLES30.GL_NONE
    private var currentTimeHandle = GLES30.GL_NONE
    private var colorHandle = GLES30.GL_NONE
    private var directionVectorHandle = GLES30.GL_NONE
    private var startTimeHandle = GLES30.GL_NONE

    private val initialTime = System.nanoTime()

    private var particles = FloatArray(PARTICLE_MAX_NUM * TOTAL_COMPONENT_COUNT)
    private var color = Color.rgb(255, 255, 255)
    private var nextParticleIndex = 0

    private var angleVariance = 5f
    private var speedVariance = 1f
    private val rotateMatrix = FloatArray(16)
    private val directionVector = floatArrayOf(0f, 0.5f, 0f, 0f)
    private val resultVector = FloatArray(4)


    companion object {
        // 最大粒子数
        private const val PARTICLE_MAX_NUM = 1000
        // 单个粒子起始位置顶点数
        private const val POSITION_COMPONENT_COUNT = 3
        // 单个粒子颜色值
        private const val COLOR_COMPONENT_COUNT = 3
        // 单个粒子终点位置顶点数
        private const val VECTOR_COMPONENT_COUNT = 3
        private const val PARTICLE_START_TIME_COMPONENT_COUNT = 1

        private const val TOTAL_COMPONENT_COUNT = (
                POSITION_COMPONENT_COUNT
                        + COLOR_COMPONENT_COUNT
                        + VECTOR_COMPONENT_COUNT
                        + PARTICLE_START_TIME_COMPONENT_COUNT
                )

        private const val STRIDE = TOTAL_COMPONENT_COUNT * 4
    }

    override fun init() {
        GLES30.glClearColor(0.2f, 0.6f, 1f, 1f)
//        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
//        GLES30.glEnable(GLES30.GL_CULL_FACE)

        vertexShader = """
            uniform float uCurrentTime;
            uniform mat4 uModelMatrix;
            uniform mat4 uViewMatrix;
            uniform mat4 uProjectionMatrix;
            attribute vec3 aPosition;
            attribute vec3 aColor;
            attribute vec3 aDirectionVector;
            attribute float aStartTime;
            varying vec3 vColor;
            varying float vElapsedTime;
            
            void main() {
                // 颜色
                vColor = aColor;
                // 经历的时间
                vElapsedTime = uCurrentTime - aStartTime;
                // 重力加速度
                float gravityFactor = pow(vElapsedTime, 2.0) / 8.0;
                // 当前位置
                vec3 currentPosition = aPosition + (aDirectionVector * vElapsedTime);
                currentPosition.y -= gravityFactor;
                gl_Position = uProjectionMatrix * uViewMatrix * uModelMatrix * vec4(currentPosition, 1.0);
                gl_PointSize = 25.0;
            }
        """

        fragmentShader = """
            precision mediump float;
            uniform sampler2D uTextureUnit;
            varying vec3 vColor;
            varying float vElapsedTime;
            
            void main() {
                gl_FragColor = vec4(vColor / vElapsedTime, 1.0) * texture2D(uTextureUnit, gl_PointCoord);
            }
        """

        program = GLHelper.createProgram(vertexShader, fragmentShader)

        textureId = GLHelper.createTexture(GLES30.GL_TEXTURE_2D, context, R.drawable.particle_texture)

        positionHandle = GLES30.glGetAttribLocation(program, "aPosition")
        colorHandle = GLES30.glGetAttribLocation(program, "aColor")
        directionVectorHandle = GLES30.glGetAttribLocation(program, "aDirectionVector")
        startTimeHandle = GLES30.glGetAttribLocation(program, "aStartTime")
        textureUnitHandle = GLES30.glGetUniformLocation(program, "uTextureUnit")
        modelMatrixHandle = GLES30.glGetUniformLocation(program, "uModelMatrix")
        viewMatrixHandle = GLES30.glGetUniformLocation(program, "uViewMatrix")
        projectionMatrixHandle = GLES30.glGetUniformLocation(program, "uProjectionMatrix")
        currentTimeHandle = GLES30.glGetUniformLocation(program, "uCurrentTime")

        verticesBuf = GLHelper.createFloatBuffer(particles)

        Matrix.setIdentityM(modelMatrix, 0)
//        Matrix.setLookAtM(
//            viewMatrix,
//            0,
//            0f, 0f, 0f,
//            0f, 0f, -1f,
//            0f, 1f, 0f
//        )
        Matrix.setIdentityM(viewMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0f, 0f, -5f)
    }

    override fun change(width: Int, height: Int) {
        this.width = width
        this.height = height
        GLES30.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.toFloat()
        Matrix.perspectiveM(
            projectionMatrix,
            0,
            45f,
            aspect,
            1f,
            300f
        )
    }

    override fun drawFrame() {
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        val currentTime = (System.nanoTime() - initialTime) / 1000000000f

//        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glUseProgram(program)
        GLES30.glUniform1f(currentTimeHandle, currentTime)
        GLES30.glUniformMatrix4fv(modelMatrixHandle, 1, false, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(viewMatrixHandle, 1, false, viewMatrix, 0)
        GLES30.glUniformMatrix4fv(projectionMatrixHandle, 1, false, projectionMatrix, 0)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + textureUnit)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)

        GLES30.glUniform1i(textureUnitHandle, textureUnit)

        genParticlesPerFrame(currentTime, 1)
        bindParticlesData()

        GLES30.glDrawArrays(GLES30.GL_POINTS, 0, vertexNum)

        GLES30.glDisableVertexAttribArray(positionHandle)
        GLES30.glDisableVertexAttribArray(colorHandle)
        GLES30.glDisableVertexAttribArray(directionVectorHandle)
        GLES30.glDisableVertexAttribArray(startTimeHandle)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, GLES30.GL_NONE)
        GLES30.glUseProgram(GLES30.GL_NONE)
        GLES30.glDisable(GLES30.GL_BLEND)
    }

    /**
     * 每帧创建的粒子数
     */
    private fun genParticlesPerFrame(time: Float, count: Int) {
        for (i in 0 until count) {
            Matrix.setRotateEulerM(
                rotateMatrix, 0,
                (Random.nextFloat() - 0.5f) * angleVariance,
                (Random.nextFloat() - 0.5f) * angleVariance,
                (Random.nextFloat() - 0.5f) * angleVariance
            )
            Matrix.multiplyMV(
                resultVector, 0,
                rotateMatrix, 0,
                directionVector, 0
            )
            val speedAdjustment = 1f + Random.nextFloat() * speedVariance
            genParticle(
                Random.nextFloat() * 2 - 1,
                Random.nextFloat() * 2 - 1,
                color,
                resultVector[0] * speedAdjustment,
                resultVector[1] * speedAdjustment,
                resultVector[2] * speedAdjustment,
                time
            )
        }
    }

    private fun genParticle(
        positionX: Float,
        positionY: Float,
        color: Int,
        directionX: Float,
        directionY: Float,
        directionZ: Float,
        startTime: Float
    ) {
        val offset = nextParticleIndex++ * TOTAL_COMPONENT_COUNT
        var currentOffset = offset

        if (vertexNum < PARTICLE_MAX_NUM) {
            vertexNum++
        }
        if (nextParticleIndex == PARTICLE_MAX_NUM) {
            nextParticleIndex = 0
        }
        // position
        particles[currentOffset++] = positionX
        particles[currentOffset++] = positionY
        particles[currentOffset++] = 0f
        // color
        particles[currentOffset++] = Color.red(color) / 255f
        particles[currentOffset++] = Color.green(color) / 255f
        particles[currentOffset++] = Color.blue(color) / 255f
        // direction
        particles[currentOffset++] = directionX
        particles[currentOffset++] = directionY
        particles[currentOffset++] = directionZ
        // startTime
        particles[currentOffset] = startTime

        verticesBuf.position(offset)
        verticesBuf.put(particles, offset, TOTAL_COMPONENT_COUNT)
        verticesBuf.position(0)
    }

    private fun bindParticlesData() {
        var offset = 0
        // position
        verticesBuf.position(offset)
        GLES30.glVertexAttribPointer(positionHandle, POSITION_COMPONENT_COUNT, GLES30.GL_FLOAT, false, STRIDE, verticesBuf)
        GLES30.glEnableVertexAttribArray(positionHandle)
        verticesBuf.position(0)
        offset += POSITION_COMPONENT_COUNT
        // color
        verticesBuf.position(offset)
        GLES30.glVertexAttribPointer(colorHandle, COLOR_COMPONENT_COUNT, GLES30.GL_FLOAT, false, STRIDE, verticesBuf)
        GLES30.glEnableVertexAttribArray(colorHandle)
        verticesBuf.position(0)
        offset += COLOR_COMPONENT_COUNT
        // direction
        verticesBuf.position(offset)
        GLES30.glVertexAttribPointer(directionVectorHandle, VECTOR_COMPONENT_COUNT, GLES30.GL_FLOAT, false, STRIDE, verticesBuf)
        GLES30.glEnableVertexAttribArray(directionVectorHandle)
        verticesBuf.position(0)
        offset += VECTOR_COMPONENT_COUNT
        // startTime
        verticesBuf.position(offset)
        GLES30.glVertexAttribPointer(startTimeHandle, PARTICLE_START_TIME_COMPONENT_COUNT, GLES30.GL_FLOAT, false, STRIDE, verticesBuf)
        GLES30.glEnableVertexAttribArray(startTimeHandle)
        verticesBuf.position(0)
    }
}