package com.gtbluesky.mediamatrix.player.util

import android.content.Context
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.text.TextUtils
import javax.microedition.khronos.opengles.GL10

object GLES2Util {
    /**
     * 编译顶点着色器
     *
     * @param shaderCode
     * @return
     */
    fun compileVertexShader(shaderCode: String): Int {
        return compileShader(GLES20.GL_VERTEX_SHADER, shaderCode)
    }

    /**
     * 编译片段着色器
     *
     * @param shaderCode
     * @return
     */
    fun compleFragmentShader(shaderCode: String): Int {
        return compileShader(GLES20.GL_FRAGMENT_SHADER, shaderCode)
    }

    /**
     * 根据类型编译着色器
     *
     * @param type
     * @param shaderCode
     * @return
     */
    private fun compileShader(type: Int, shaderCode: String): Int {
        // 根据不同的类型创建着色器 ID
        val shaderObjectId = GLES20.glCreateShader(type)
        if (shaderObjectId == 0) {
            return 0
        }
        // 将着色器 ID 和着色器程序内容连接
        GLES20.glShaderSource(shaderObjectId, shaderCode)
        // 编译着色器
        GLES20.glCompileShader(shaderObjectId)
        // 以下为验证编译结果是否失败
        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shaderObjectId, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            // 失败则删除
            GLES20.glDeleteShader(shaderObjectId)
            return 0
        }
        return shaderObjectId
    }

    /**
     * 创建 OpenGL 程序和着色器链接
     *
     * @param vertexShaderId
     * @param fragmentShaderId
     * @return
     */
    fun linkProgram(vertexShaderId: Int, fragmentShaderId: Int): Int {
        // 创建 OpenGL 程序 ID
        val programObjectId = GLES20.glCreateProgram()
        if (programObjectId == 0) {
            return 0
        }
        // 链接上 顶点着色器
        GLES20.glAttachShader(programObjectId, vertexShaderId)
        // 链接上 片段着色器
        GLES20.glAttachShader(programObjectId, fragmentShaderId)
        // 链接着色器之后，链接 OpenGL 程序
        GLES20.glLinkProgram(programObjectId)
        val linkStatus = IntArray(1)
        // 验证链接结果是否失败
        GLES20.glGetProgramiv(programObjectId, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            // 失败则删除 OpenGL 程序
            GLES20.glDeleteProgram(programObjectId)
            return 0
        }
        return programObjectId
    }

    /**
     * 验证 OpenGL 程序
     *
     * @param programObjectId
     * @return
     */
    fun validateProgram(programObjectId: Int): Boolean {
        GLES20.glValidateProgram(programObjectId)
        val validateStatus = IntArray(1)
        GLES20.glGetProgramiv(programObjectId, GLES20.GL_VALIDATE_STATUS, validateStatus, 0)
        return validateStatus[0] != 0
    }

    fun createTextureID(): Int {
        val texture = IntArray(1)
        GLES20.glGenTextures(1, texture, 0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0])
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE
        )
        return texture[0]
    }

    /**
     * 创建 OpenGL 程序过程
     *
     * @param vertexShaderSource
     * @param fragmentShaderSource
     * @return
     */
    fun buildProgramFromResource(
        context: Context,
        vertexShaderSource: Int,
        fragmentShaderSource: Int
    ): Int {
        val program: Int
        val vertexShader = compileVertexShader(
            TextResourceReader.readTextFileFromResource(context, vertexShaderSource)
        )
        val fragmentShader = compleFragmentShader(
            TextResourceReader.readTextFileFromResource(context, fragmentShaderSource)
        )
        program = linkProgram(vertexShader, fragmentShader)
        validateProgram(program)
        return program
    }

    fun buildProgramFromAsset(
        context: Context,
        vertexShaderSource: String,
        fragmentShaderSource: String
    ): Int {
        val vertexShader = TextResourceReader.readTextFileFromAsset(context, vertexShaderSource)?.let {
            compileVertexShader(it)
        }
        val fragmentShader = TextResourceReader.readTextFileFromAsset(context, fragmentShaderSource)?.let {
            compleFragmentShader(it)
        }
        if (vertexShader == null || fragmentShader == null) {
            LogUtil.e("shader is null")
            return 0
        }
        val program = linkProgram(vertexShader, fragmentShader)
        validateProgram(program)
        return program
    }

    fun buildProgram(vertexShaderSource: String, fragmentShaderSource: String): Int {
        val program: Int
        val vertexShader = compileVertexShader(vertexShaderSource)
        val fragmentShader = compleFragmentShader(fragmentShaderSource)
        program = linkProgram(vertexShader, fragmentShader)
        validateProgram(program)
        return program
    }

    fun useTexParameter() {
        //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
        GLES20.glTexParameterf(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST.toFloat()
        )
        //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
        GLES20.glTexParameterf(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat()
        )
        //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameterf(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE.toFloat()
        )
        //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameterf(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE.toFloat()
        )
    }

    fun genTexturesWithParameter(
        size: Int, textures: IntArray, start: Int,
        glFormat: Int, width: Int, height: Int
    ) {
        GLES20.glGenTextures(size, textures, start)
        for (i in 0 until size) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[i])
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D, 0, glFormat, width, height,
                0, glFormat, GLES20.GL_UNSIGNED_BYTE, null
            )
            useTexParameter()
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    fun bindFrameTexture(frameBufferId: Int, textureId: Int) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBufferId)
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D, textureId, 0
        )
    }

    fun unBindFrameBuffer() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }
}