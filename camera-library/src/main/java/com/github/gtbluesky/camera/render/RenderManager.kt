package com.github.gtbluesky.camera.render

import com.github.gtbluesky.gles.constant.FilterConstant
import com.github.gtbluesky.gles.filter.BaseFilter
import com.github.gtbluesky.gles.filter.OESInputFilter
import com.github.gtbluesky.gles.util.GLHelper
import java.nio.FloatBuffer

class RenderManager {

    private var filter: BaseFilter? = null
    private var vertexBuffer: FloatBuffer? = null
    private var textureBuffer: FloatBuffer? = null

    fun init() {
        initBuffers()
        initFilter()
    }

    private fun initBuffers() {
        releaseBuffers()
        vertexBuffer = GLHelper.createFloatBuffer(FilterConstant.VERTEX_COORDS)
        textureBuffer = GLHelper.createFloatBuffer(FilterConstant.OES_TEXTURE_COORDS)
    }

    private fun initFilter() {
        filter = OESInputFilter()
        filter?.init()
    }

    fun setDisplaySize(width: Int, height: Int) {
        filter?.change(width, height)
    }

    fun drawFrame(textureId: Int, matrix: FloatArray) {
        if (filter is OESInputFilter) {
            (filter as OESInputFilter).transformMatrix = matrix
        }
        filter?.drawFrame(textureId, vertexBuffer!!, textureBuffer!!)
    }

    fun release() {
        releaseBuffers()
        releaseFilters()
    }

    private fun releaseBuffers() {
        vertexBuffer?.clear()
        vertexBuffer = null
        textureBuffer?.clear()
        textureBuffer = null
    }

    private fun releaseFilters() {
        filter?.destroy()
    }
}