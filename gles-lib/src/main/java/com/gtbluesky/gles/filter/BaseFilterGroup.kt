package com.gtbluesky.gles.filter

import java.nio.FloatBuffer

abstract class BaseFilterGroup : NormalFilter() {
    val filterList = arrayListOf<BaseFilter>()

    override fun initFrameBuffer(width: Int, height: Int) {
        filterList.forEach {
            it.initFrameBuffer(width, height)
        }
    }

    override fun drawFrame(
        textureId: Int,
        vertexBuffer: FloatBuffer,
        textureBuffer: FloatBuffer,
        clearColor: Boolean
    ) {
        var outputTextureId = textureId
        filterList.forEachIndexed { index, filter ->
            if (index == filterList.size - 1) {
                filter.drawFrame(
                    outputTextureId,
                    vertexBuffer,
                    textureBuffer,
                    clearColor
                )
            } else {
                outputTextureId = filter.drawFrameBuffer(
                    outputTextureId,
                    vertexBuffer,
                    textureBuffer
                )
            }
        }
    }

    override fun drawFrameBuffer(
        textureId: Int,
        vertexBuffer: FloatBuffer,
        textureBuffer: FloatBuffer
    ): Int {
        var outputTextureId = textureId
        filterList.forEach {
            outputTextureId = it.drawFrameBuffer(
                outputTextureId,
                vertexBuffer,
                textureBuffer
            )
        }
        return outputTextureId
    }

    override fun setViewSize(width: Int, height: Int) {
        filterList.forEach {
            it.setViewSize(width, height)
        }
    }

    override fun setTextureSize(width: Int, height: Int) {
        filterList.forEach {
            it.setTextureSize(width, height)
        }
    }

    override fun destroy() {
        filterList.forEach {
            it.destroy()
        }
    }
}