package com.gtbluesky.gles.shape

import android.content.Context
import android.opengl.GLES30

class BlendDemo(val context: Context) : BaseShape() {

    private val srcShape = BlendShape()
    private val dstShape = BlendShape()

    override fun init() {
//        val srcBitmap = BitmapFactory.decodeResource(context.resources, R.drawable)
//        val dstBitmap = BitmapFactory.decodeResource(context.resources, R.mipmap.bg)

        GLES30.glClearColor(0.2f, 0.6f, 1f, 0f)

        srcShape.init()
        dstShape.init()

        val textures = IntArray(2)
        GLES30.glGenTextures(2, textures, 0)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[0])
//        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, srcBitmap, 0)
        srcShape.textureId = textures[0]

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[1])
//        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, dstBitmap, 0)
        dstShape.textureId = textures[1]
    }

    override fun change(width: Int, height: Int) {
        srcShape.change(width, height)
        dstShape.change(width, height)
        GLES30.glViewport(0, 0, srcShape.width, srcShape.height)
    }

    override fun drawFrame() {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

        dstShape.drawFrame()

        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_COLOR, GLES30.GL_ONE_MINUS_SRC_COLOR)
        GLES30.glBlendEquation(GLES30.GL_FUNC_ADD)

        srcShape.drawFrame()

        GLES30.glDisable(GLES30.GL_BLEND)
    }
}