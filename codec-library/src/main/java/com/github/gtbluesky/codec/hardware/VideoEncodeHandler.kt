package com.github.gtbluesky.codec.hardware

import android.opengl.EGLContext
import android.opengl.GLES30
import android.os.Handler
import android.os.Looper
import android.os.Message
import com.github.gtbluesky.gles.constant.FilterConstant
import com.github.gtbluesky.gles.egl.EglCore
import com.github.gtbluesky.gles.egl.WindowSurface
import com.github.gtbluesky.gles.filter.NormalFilter
import com.github.gtbluesky.gles.util.GLHelper
import java.nio.FloatBuffer

class VideoEncodeHandler(looper: Looper, private val encoder: Encoder) : Handler(looper) {

    private var eglCore: EglCore? = null
    private var windowSurface: WindowSurface? = null
    private val recordFilter = NormalFilter()
    private val vertexBuffer: FloatBuffer by lazy {
        GLHelper.createFloatBuffer(FilterConstant.VERTEX_COORDS)
    }
    private val textureBuffer: FloatBuffer by lazy {
        GLHelper.createFloatBuffer(FilterConstant.TEXTURE_COORDS)
    }
    private var isEncoding = false

    companion object {
        private val TAG = VideoEncodeHandler::class.java.simpleName
        // 渲染
        const val MSG_RENDER = 0x04
        // 开始编码
        const val MSG_START_ENCODING = 0x05
        // 停止编码
        const val MSG_STOP_ENCODING = 0x06

        const val MSG_QUIT = 0x0a
    }

    override fun handleMessage(msg: Message) {
        when (msg.what) {
            MSG_START_ENCODING -> {
                startEncoding(msg.arg1, msg.arg2, msg.obj as EGLContext)
            }
            MSG_RENDER -> {
                drawFrame(msg.arg1, msg.obj as Long)
            }
            MSG_STOP_ENCODING -> {
                stopEncoding()
            }
            MSG_QUIT -> {
                looper.quit()
            }
        }
    }

    private fun destroy() {
        recordFilter.destroy()
        windowSurface?.release()
        windowSurface = null
        eglCore?.release()
        eglCore = null
    }

    private fun drawFrame(textureId: Int, timeStamp: Long) {
        windowSurface?.makeCurrent()
        recordFilter.drawFrame(textureId, vertexBuffer, textureBuffer)
        windowSurface?.setPresentationTime(timeStamp)
        windowSurface?.swapBuffers()
    }

    private fun startEncoding(textureWidth: Int, textureHeight: Int, eglContext: EGLContext) {
        windowSurface?.apply {
            releaseEglSurface()
        }
        eglCore?.apply {
            release()
        }
        eglCore = EglCore(eglContext, EglCore.FLAG_RECORDABLE)
        if (windowSurface == null) {
            windowSurface = WindowSurface(eglCore!!, encoder.inputSurface!!, true)
        } else {
            windowSurface?.recreate(eglCore!!)
        }
        windowSurface?.makeCurrent()

        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        GLES30.glDisable(GLES30.GL_CULL_FACE)

        recordFilter.init()
        recordFilter.change(textureWidth, textureHeight)
        isEncoding = true
    }

    private fun stopEncoding() {
        isEncoding = false

    }

}