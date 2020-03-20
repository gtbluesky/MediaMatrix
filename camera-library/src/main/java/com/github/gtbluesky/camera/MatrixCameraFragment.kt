package com.github.gtbluesky.camera

import android.graphics.SurfaceTexture
import android.os.Bundle
import android.view.*
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.github.gtbluesky.camera.engine.CameraEngine
import com.github.gtbluesky.camera.render.PreviewRenderer

class MatrixCameraFragment : Fragment() {

    private lateinit var contentView: RelativeLayout
    private var previewRenderer: PreviewRenderer? = null
    private var previewNow = false
    var torchOn = false
        private set

    companion object {
        private val TAG = MatrixCameraFragment::class.java.simpleName

        @JvmStatic
        fun newInstance(bundle: Bundle? = null, previewNow: Boolean = false): MatrixCameraFragment {
            val fragment = MatrixCameraFragment().also {
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
        previewRenderer = PreviewRenderer(context!!)
        TextureView(context).also {
            it.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
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
            contentView.addView(it)
        }

//        SurfaceView(context).also {
//            it.holder.addCallback(object : SurfaceHolder.Callback {
//                override fun surfaceCreated(holder: SurfaceHolder) {
//
//                }
//
//                override fun surfaceChanged(
//                    holder: SurfaceHolder,
//                    format: Int,
//                    width: Int,
//                    height: Int
//                ) {
//                    CameraParam.getInstance().let {
//                        it.viewWidth = width
//                        it.viewHeight = height
//                    }
//                    previewRenderer?.let {
//                        it.bindSurface(holder.surface)
//                        it.changePreviewSize()
//                    }
//                }
//
//                override fun surfaceDestroyed(holder: SurfaceHolder?) {
//
//                }
//            })
//            contentView.addView(it)
//        }
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

    fun takePicture(filePath: String, rotation: Int = 0) {
        previewRenderer?.takePicture(filePath, rotation)
    }

    fun startRecording(filePath: String, rotation: Int = 0) {
        previewRenderer?.startRecording(filePath, rotation)
    }

    fun stopRecording() = previewRenderer?.stopRecording()

    fun toggleTorch() {
        (!torchOn).let {
            torchOn = it
            previewRenderer?.toggleTorch(it)
        }
    }

    fun changeResolution(
        resolutionType: ResolutionType,
        aspectRatioType: AspectRatioType
    ) {
        previewRenderer?.changeResolution(
            resolutionType,
            aspectRatioType
        )
    }
}