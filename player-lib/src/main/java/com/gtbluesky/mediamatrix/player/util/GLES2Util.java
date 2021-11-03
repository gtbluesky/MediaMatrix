package com.gtbluesky.mediamatrix.player.util;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import javax.microedition.khronos.opengles.GL10;

public class GLES2Util {
    /**
     * 编译顶点着色器
     *
     * @param shaderCode
     * @return
     */
    public static int compileVertexShader(String shaderCode) {
        return compileShader(GLES20.GL_VERTEX_SHADER, shaderCode);
    }

    /**
     * 编译片段着色器
     *
     * @param shaderCode
     * @return
     */
    public static int compleFragmentShader(String shaderCode) {
        return compileShader(GLES20.GL_FRAGMENT_SHADER, shaderCode);
    }

    /**
     * 根据类型编译着色器
     *
     * @param type
     * @param shaderCode
     * @return
     */
    private static int compileShader(int type, String shaderCode) {
        // 根据不同的类型创建着色器 ID
        int shaderObjectId = GLES20.glCreateShader(type);
        if (shaderObjectId == 0) {
            return 0;
        }
        // 将着色器 ID 和着色器程序内容连接
        GLES20.glShaderSource(shaderObjectId, shaderCode);
        // 编译着色器
        GLES20.glCompileShader(shaderObjectId);
        // 以下为验证编译结果是否失败
        final int[] compileStatsu = new int[1];
        GLES20.glGetShaderiv(shaderObjectId, GLES20.GL_COMPILE_STATUS, compileStatsu, 0);
        if ((compileStatsu[0] == 0)) {
            // 失败则删除
            GLES20.glDeleteShader(shaderObjectId);
            return 0;
        }
        return shaderObjectId;
    }

    /**
     * 创建 OpenGL 程序和着色器链接
     *
     * @param vertexShaderId
     * @param fragmentShaderId
     * @return
     */
    public static int linkProgram(int vertexShaderId, int fragmentShaderId) {
        // 创建 OpenGL 程序 ID
        final int programObjectId = GLES20.glCreateProgram();
        if (programObjectId == 0) {
            return 0;
        }
        // 链接上 顶点着色器
        GLES20.glAttachShader(programObjectId, vertexShaderId);
        // 链接上 片段着色器
        GLES20.glAttachShader(programObjectId, fragmentShaderId);
        // 链接着色器之后，链接 OpenGL 程序
        GLES20.glLinkProgram(programObjectId);
        final int[] linkStatus = new int[1];
        // 验证链接结果是否失败
        GLES20.glGetProgramiv(programObjectId, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            // 失败则删除 OpenGL 程序
            GLES20.glDeleteProgram(programObjectId);
            return 0;
        }
        return programObjectId;
    }

    /**
     * 验证 OpenGL 程序
     *
     * @param programObjectId
     * @return
     */
    public static boolean validateProgram(int programObjectId) {
        GLES20.glValidateProgram(programObjectId);
        final int[] validateStatus = new int[1];
        GLES20.glGetProgramiv(programObjectId, GLES20.GL_VALIDATE_STATUS, validateStatus, 0);
        return validateStatus[0] != 0;
    }

    public static int createTextureID() {
        int[] texture = new int[1];
        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        return texture[0];
    }

    /**
     * 创建 OpenGL 程序过程
     *
     * @param vertexShaderSource
     * @param fragmentShaderSource
     * @return
     */
    public static int buildProgramFromResource(Context context, int vertexShaderSource, int fragmentShaderSource) {
        int program;
        int vertexShader = compileVertexShader(
                TextResourceReader.readTextFileFromResource(context, vertexShaderSource));
        int fragmentShader = compleFragmentShader(
                TextResourceReader.readTextFileFromResource(context, fragmentShaderSource));
        program = linkProgram(vertexShader, fragmentShader);
        validateProgram(program);
        return program;
    }

    public static int buildProgramFromAsset(Context context, String vertexShaderSource, String fragmentShaderSource) {
        int program;
        int vertexShader = compileVertexShader(
                TextResourceReader.readTextFileFromAsset(context, vertexShaderSource));
        int fragmentShader = compleFragmentShader(
                TextResourceReader.readTextFileFromAsset(context, fragmentShaderSource));
        program = linkProgram(vertexShader, fragmentShader);
        validateProgram(program);
        return program;
    }

    public static int buildProgram(String vertexShaderSource, String fragmentShaderSource) {
        int program;
        int vertexShader = compileVertexShader(vertexShaderSource);
        int fragmentShader = compleFragmentShader(fragmentShaderSource);
        program = linkProgram(vertexShader, fragmentShader);
        validateProgram(program);
        return program;
    }

    public static void useTexParameter() {
        //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }

    public static void genTexturesWithParameter(int size, int[] textures, int start,
                                                int gl_format, int width, int height) {
        GLES20.glGenTextures(size, textures, start);
        for (int i = 0; i < size; i++) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[i]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, gl_format, width, height,
                    0, gl_format, GLES20.GL_UNSIGNED_BYTE, null);
            useTexParameter();
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    public static void bindFrameTexture(int frameBufferId, int textureId) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBufferId);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, textureId, 0);
    }

    public static void unBindFrameBuffer() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

}
