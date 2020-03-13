package com.github.gtbluesky.camera

import android.graphics.SurfaceTexture
import android.os.Bundle
import android.view.*
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.github.gtbluesky.camera.engine.CameraParam
import com.github.gtbluesky.camera.render.PreviewRenderer

class CameraPreviewFragment : Fragment() {

//    private lateinit var surfaceView: SurfaceView
    private lateinit var contentView: RelativeLayout
    private lateinit var textureView: TextureView
    private var previewRenderer: PreviewRenderer? = null
    private var previewNow = false
    var torchOn = false
        private set

    companion object {
        private val TAG = CameraPreviewFragment::class.java.simpleName

        @JvmStatic
        fun newInstance(bundle: Bundle? = null, previewNow: Boolean = false): CameraPreviewFragment {
            val fragment = CameraPreviewFragment().also {
                it.previewNow = previewNow
            }
            bundle?.let {
                fragment.arguments = it
            }
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return RelativeLayout(context).also {
            contentView = it
            if (previewNow) {
                initCameraView()
            }
        }
    }

    private fun initCameraView() {
        textureView = TextureView(context)
        previewRenderer = PreviewRenderer(context!!)
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                CameraParam.getInstance().let {
                    it.viewWidth = width
                    it.viewHeight = height
                }
                previewRenderer?.let {
                    it.bindSurface(surface)
                    it.changePreviewSize()
                }
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture?,
                width: Int,
                height: Int
            ) {
                CameraParam.getInstance().let {
                    it.viewWidth = width
                    it.viewHeight = height
                }
                previewRenderer?.changePreviewSize()
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
                previewRenderer?.unBindSurface()
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {}
        }
        contentView.addView(textureView)

//        mCameraView.setRenderer(object : GLSurfaceView.Renderer {
//            override fun onDrawFrame(gl: GL10?) {
//                CameraEngine.instance.surfaceTexture?.apply {
//                    updateTexImage()
//                    getTransformMatrix(CameraEngine.instance.mCameraRenderer.transformMatrix)
//                }
//                CameraEngine.instance.mCameraRenderer.apply {
//                    drawFrame()
//                }
//            }
//
//            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
//                CameraEngine.instance.mCameraRenderer.change(width, height)
//            }
//
//            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
//                CameraEngine.instance.mCameraRenderer.init()
//                CameraEngine.instance.startPreview(mContext, mCameraView.width, mCameraView.height)
//                CameraEngine.instance.surfaceTexture?.setOnFrameAvailableListener {
//                    mCameraView.requestRender()
//                }
//            }
//        })
//        mCameraView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
    }

    fun startPrewiew() {
        if (context == null) {
            Toast.makeText(
                context,
                "Please add CameraPreviewFragment in Activity",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (contentView.childCount > 0) {
            Toast.makeText(
                context,
                "Do not call startPrewiew() again",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        initCameraView()
    }

    fun switchCamera() = previewRenderer?.switchCamera()

    fun takePicture(filePath: String) = previewRenderer?.takePicture(filePath)

    fun toggleTorch() {
        (!torchOn).let {
            torchOn = it
            previewRenderer?.toggleTorch(it)
        }
    }
}