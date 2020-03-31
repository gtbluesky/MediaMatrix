package com.gtbluesky.camera.render

import android.content.Context
import android.opengl.GLES30
import com.gtbluesky.camera.R
import com.gtbluesky.gles.FilterType
import com.gtbluesky.gles.constant.FilterConstant
import com.gtbluesky.gles.filter.*
import com.gtbluesky.gles.util.GLHelper
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
//        filterMap[FilterType.BlurFilter] = GaussianBlurFilter(context, 1f)
//        filterMap[FilterType.SplitScreenFilter] = SplitScreenFilter(context, 9)
//        filterMap[FilterType.MirrorScreenFilter] = MirrorScreenFilter(context, isMirrorX = false)
//        filterMap[FilterType.MosaicFilter] = MosaicCircleFilter(context)
        filterMap[FilterType.WatermarkFilter] = WatermarkFilter().apply {
            setResource(context, R.drawable.wm, 0, 0, 100, 200)
        }
//        filterMap[FilterType.StickerFilter] = StickerFilter().apply {
//            setResource(context, R.drawable.wm, 100, 300, 200, 200)
//        }
//        filterMap[FilterType.BeautyFilter] = BeautyFilter(context)
        filterMap[FilterType.ToneCurveFilter] = ToneCurveFilter(context, rawId = R.raw.tone_cuver_sample)
//        filterMap[FilterType.LookupTableFilter] = LookupTableFilter(context)
    }

    fun setViewSize(width: Int, height: Int) {
        filterMap.forEach {
            it.value.let { filter ->
                filter.setViewSize(width, height)
            }
        }
    }

    fun setTextureSize(width: Int, height: Int) {
        filterMap.forEach {
            it.value.let { filter ->
                filter.initFrameBuffer(width, height)
                filter.setTextureSize(width, height)
            }
        }
    }

    fun drawFrame(textureId: Int, matrix: FloatArray): Int {
        (filterMap[FilterType.OESInputFilter] as? OESInputFilter)?.let {
            it.transformMatrix = matrix
            outputTextureId = it.drawFrameBuffer(textureId, vertexBuffer, textureBuffer)
        }
        (filterMap[FilterType.BeautyFilter])?.let {
            outputTextureId = it.drawFrameBuffer(outputTextureId, vertexBuffer, textureBuffer)
        }
        (filterMap[FilterType.BlurFilter])?.let {
            outputTextureId = it.drawFrameBuffer(outputTextureId, vertexBuffer, textureBuffer)
        }
        (filterMap[FilterType.ToneCurveFilter])?.let {
            outputTextureId = it.drawFrameBuffer(outputTextureId, vertexBuffer, textureBuffer)
        }
        (filterMap[FilterType.LookupTableFilter])?.let {
            outputTextureId = it.drawFrameBuffer(outputTextureId, vertexBuffer, textureBuffer)
        }
        (filterMap[FilterType.MosaicFilter])?.let {
            outputTextureId = it.drawFrameBuffer(outputTextureId, vertexBuffer, textureBuffer)
        }
        (filterMap[FilterType.SplitScreenFilter])?.let {
            outputTextureId = it.drawFrameBuffer(outputTextureId, vertexBuffer, textureBuffer)
        }
        (filterMap[FilterType.MirrorScreenFilter])?.let {
            outputTextureId = it.drawFrameBuffer(outputTextureId, vertexBuffer, textureBuffer)
        }
        (filterMap[FilterType.WatermarkFilter])?.let {
            outputTextureId = it.drawFrameBuffer(outputTextureId, vertexBuffer, textureBuffer)
        }
        (filterMap[FilterType.StickerFilter])?.let {
            outputTextureId = it.drawFrameBuffer(outputTextureId, vertexBuffer, textureBuffer)
        }
        (filterMap[FilterType.NormalFilter])?.let {
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