package com.gtbluesky.gles.shape

import android.opengl.GLES30
import java.nio.FloatBuffer

abstract class BaseShape {

    var width = 0
    var height = 0

    protected lateinit var vertexShader: String
    protected lateinit var fragmentShader: String

    protected var vertexNum = 0
    protected lateinit var verticesBuf: FloatBuffer

    protected val modelMatrix = FloatArray(16)
    protected val viewMatrix = FloatArray(16)
    protected val projectionMatrix = FloatArray(16)

    protected var program = GLES30.GL_NONE
    protected var positionHandle = GLES30.GL_NONE
    protected var modelMatrixHandle = GLES30.GL_NONE
    protected var viewMatrixHandle = GLES30.GL_NONE
    protected var projectionMatrixHandle = GLES30.GL_NONE

    abstract fun init()

    abstract fun change(width: Int, height: Int)

    abstract fun drawFrame()

    open fun destroy() {
        if (program > 0) {
            GLES30.glDeleteProgram(program)
            program = 0
        }
    }
}