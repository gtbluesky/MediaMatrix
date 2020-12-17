package com.gtbluesky.gles.filter

import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.Matrix

class OESInputFilter :
    NormalFilter(
        vertexShader = VERTEX_SHADER,
        fragmentShader = FRAGMENT_SHADER
    ) {

    private var transformMatrixHandle = 0
    var transformMatrix: FloatArray? = null

    companion object {
        private const val VERTEX_SHADER = """
            attribute vec4 aPosition;
            attribute vec4 aTextureCoord;
            varying vec2 vTextureCoord;
            uniform mat4 uMVPMatrix;
            uniform mat4 uTransformMatrix;
            void main() {
                gl_Position = uMVPMatrix * aPosition;
                vTextureCoord = (uTransformMatrix * aTextureCoord).xy;
            }
        """
        private const val FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            uniform samplerExternalOES uTextureUnit;
            varying vec2 vTextureCoord;
            void main() {
                gl_FragColor = texture2D(uTextureUnit, vTextureCoord);
            }
        """
    }

    override fun initProgram() {
        super.initProgram()
        textureType = GLES11Ext.GL_TEXTURE_EXTERNAL_OES
        transformMatrixHandle = GLES30.glGetUniformLocation(program, "uTransformMatrix")
    }

    override fun preDraw() {
        if (transformMatrix == null) {
            transformMatrix = FloatArray(GL_MATRIX_SIZE)
            Matrix.setIdentityM(transformMatrix, 0)
        }
        GLES30.glUniformMatrix4fv(transformMatrixHandle, 1, false, transformMatrix, 0)
    }

    override fun adjustMvpMatrix() {
        if (textureWidth.toFloat() / textureHeight > frameWidth.toFloat() / frameHeight) {
            val heightScale = textureHeight.toFloat() / frameHeight
            Matrix.orthoM(
                mvpMatrix,
                0,
                -frameWidth * heightScale / textureWidth,
                frameWidth * heightScale / textureWidth,
                -1f, 1f,
                0f, 1f
            )
        } else {
            val widthScale = textureWidth.toFloat() / frameWidth
            Matrix.orthoM(
                mvpMatrix,
                0,
                -1f, 1f,
                -frameHeight * widthScale / textureHeight,
                frameHeight * widthScale / textureHeight,
                0f, 1f
            )
        }
    }
}