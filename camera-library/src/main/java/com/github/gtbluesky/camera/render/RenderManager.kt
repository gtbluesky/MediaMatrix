package com.github.gtbluesky.camera.render

import android.content.Context
import android.opengl.GLES30
import com.github.gtbluesky.camera.R
import com.github.gtbluesky.gles.FilterType
import com.github.gtbluesky.gles.constant.FilterConstant
import com.github.gtbluesky.gles.filter.*
import com.github.gtbluesky.gles.util.GLHelper
import java.nio.FloatBuffer

class RenderManager(context: Context) {

    private val vertexBuffer: FloatBuffer by lazy {
        GLHelper.createFloatBuffer(FilterConstant.VERTEX_COORDS)
    }
    private val textureBuffer: FloatBuffer by lazy {
        GLHelper.createFloatBuffer(FilterConstant.TEXTURE_COORDS)
    }
    private val filterMap = HashMap<FilterType, BaseFilter>()
    private var outputTextureId = GLES30.GL_NONE

    init {
        initFilter(context)
    }

    private fun initFilter(context: Context) {
        filterMap[FilterType.OESInputFilter] = OESInputFilter()
        filterMap[FilterType.NormalFilter] = NormalFilter()
        filterMap[FilterType.WatermarkFilter] = WatermarkFilter().apply {
            setResource(context, R.drawable.wm, 100, 100, 200, 200)
        }
        filterMap[FilterType.StickerFilter] = StickerFilter().apply {
            setResource(context, R.drawable.wm, 100, 300, 200, 200)
        }
    }

    fun setViewSize(width: Int, height: Int) {
        filterMap.forEach {
            it.value.let { filter ->
                filter.setViewSize(width, height)
                filter.initFrameBuffer(width, height)
            }
        }
    }

    fun setTextureSize(width: Int, height: Int) {
        filterMap.forEach {
            it.value.setTextureSize(width, height)
        }
    }

    fun drawFrame(textureId: Int, matrix: FloatArray): Int {
        (filterMap[FilterType.OESInputFilter] as? OESInputFilter)?.let {
            it.transformMatrix = matrix
            outputTextureId = it.drawFrameBuffer(textureId, vertexBuffer, textureBuffer)
        }
        (filterMap[FilterType.WatermarkFilter] as? WatermarkFilter)?.let {
            outputTextureId = it.drawFrameBuffer(outputTextureId, vertexBuffer, textureBuffer)
        }
        (filterMap[FilterType.StickerFilter] as? StickerFilter)?.let {
            outputTextureId = it.drawFrameBuffer(outputTextureId, vertexBuffer, textureBuffer)
        }
        (filterMap[FilterType.NormalFilter] as? NormalFilter)?.let {
            it.drawFrame(outputTextureId, vertexBuffer, textureBuffer)
        }
        return outputTextureId
    }

    fun release() {
        releaseBuffers()
        releaseFilters()
    }

    private fun releaseBuffers() {
        vertexBuffer.clear()
        textureBuffer.clear()
    }

    private fun releaseFilters() {
        filterMap.forEach {
            it.value.destroy()
        }
    }
}