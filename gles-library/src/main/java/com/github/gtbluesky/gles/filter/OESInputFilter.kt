package com.github.gtbluesky.gles.filter

import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.Matrix

class OESInputFilter : NormalFilter() {

    private var transformMatrixHandle = 0
    var transformMatrix: FloatArray? = null

    companion object {
        private val TAG = OESInputFilter::class.java.simpleName
    }

    init {
        vertexShader = """
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
        fragmentShader = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            uniform samplerExternalOES uTextureUnit;
            varying vec2 vTextureCoord;
            void main() {
                gl_FragColor = texture2D(uTextureUnit, vTextureCoord);
            }
        """
    }

    override fun init() {
        super.init()
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


}