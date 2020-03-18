package com.github.gtbluesky.camera.render

import android.opengl.GLES30
import android.util.SparseArray
import com.github.gtbluesky.gles.constant.FilterConstant
import com.github.gtbluesky.gles.filter.BaseFilter
import com.github.gtbluesky.gles.filter.NormalFilter
import com.github.gtbluesky.gles.filter.OESInputFilter
import com.github.gtbluesky.gles.util.GLHelper
import java.nio.FloatBuffer

class RenderManager {

    private var vertexBuffer: FloatBuffer? = null
    private var textureBuffer: FloatBuffer? = null
    private val filterArray = SparseArray<BaseFilter>()
    private var outputTextureId = GLES30.GL_NONE

    fun init() {
        initBuffers()
        initFilter()
    }

    private fun initBuffers() {
        releaseBuffers()
        vertexBuffer = GLHelper.createFloatBuffer(FilterConstant.VERTEX_COORDS)
        textureBuffer = GLHelper.createFloatBuffer(FilterConstant.TEXTURE_COORDS)
    }

    private fun initFilter() {
        filterArray.put(0, OESInputFilter().apply { init() })
        filterArray.put(1, NormalFilter().apply { init() })
    }

    fun setViewSize(width: Int, height: Int) {
        repeat(filterArray.size()) {
            filterArray.valueAt(it).let { filter ->
                filter.setViewSize(width, height)
                filter.initFrameBuffer(width, height)
            }
        }
    }

    fun setTextureSize(width: Int, height: Int) {
        repeat(filterArray.size()) {
            filterArray.valueAt(it).let { filter ->
                filter.setTextureSize(width, height)
            }
        }
    }

    fun drawFrame(textureId: Int, matrix: FloatArray): Int {
        (filterArray[0] as? OESInputFilter)?.let {
            it.transformMatrix = matrix
            outputTextureId = it.drawFrameBuffer(textureId, vertexBuffer!!, textureBuffer!!)
        }
        filterArray[1]?.let {
            it.drawFrame(outputTextureId, vertexBuffer!!, textureBuffer!!)
        }
//        else -> {
//            outputTextureId = filter.drawFrameBuffer(outputTextureId, vertexBuffer!!, textureBuffer!!)
//        }
        return outputTextureId
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
        repeat(filterArray.size()) {
            filterArray.valueAt(it).destroy()
        }
    }
}