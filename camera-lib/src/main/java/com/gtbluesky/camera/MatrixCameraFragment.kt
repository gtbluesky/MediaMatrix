package com.gtbluesky.camera

import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.view.*
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.gtbluesky.camera.controller.OrientationController
import com.gtbluesky.camera.controller.SensorController
import com.gtbluesky.camera.engine.CameraEngine
import com.gtbluesky.camera.entity.SnapInfoEntity
import com.gtbluesky.camera.listener.OnCompletionListener
import com.gtbluesky.camera.listener.OnRotationChangeListener
import com.gtbluesky.camera.listener.OnZoomChangeListener
import com.gtbluesky.camera.listener.StartFocusCallback
import com.gtbluesky.camera.render.PreviewRenderer
import java.io.File

class MatrixCameraFragment : Fragment() {

    private lateinit var contentView: RelativeLayout
    private var previewRenderer: PreviewRenderer? = null

    /**
     * false时，需主动调用startPreview()方法
     */
    private var previewNow = false
    private val scaleGestureDetector: ScaleGestureDetector by lazy {
        ScaleGestureDetector(context, ZoomScaleGestureDetector())
    }
    private var minSpan = 0f
    var onZoomChangeListener: OnZoomChangeListener? = null
    var onCompletionListener: OnCompletionListener? = null

    var torchOn = false
        private set

    private var rotation = 0
    private var resolutionType = ResolutionType.R_720
    private var aspectRatioType = AspectRatioType.FULL

    private val orientationController: OrientationController by lazy {
        OrientationController(context).also {
            it.onRotationChangeListener = object : OnRotationChangeListener {
                override fun onRotationChange(rotation: Int) {
                    this@MatrixCameraFragment.rotation = rotation
                }
            }
        }
    }
    private val sensorController: SensorController by lazy {
        SensorController(context).also {
            it.startFocusCallback = object : StartFocusCallback {
                override fun onStart() {
                    CameraEngine.getInstance().setAutoFocus()
                }
            }
        }
    }

    companion object {
        const val PREVIEW_NOW = "PREVIEW_NOW"

        @JvmStatic
        @JvmOverloads
        fun newInstance(bundle: Bundle? = null): MatrixCameraFragment {
            val fragment = MatrixCameraFragment()
            bundle?.let {
                fragment.arguments = it
            }
            return fragment
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        arguments?.let {
            previewNow = it.getBoolean(PREVIEW_NOW)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return RelativeLayout(context).also {
            it.keepScreenOn = true
            contentView = it
            if (previewNow) {
                initCameraView()
            }
        }
    }

    private fun initCameraView() {
        context?.let {
            previewRenderer = PreviewRenderer(it)
            previewRenderer?.setOnCompletionListener(onCompletionListener)
            TextureView(it).also {
                it.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(
                        surface: SurfaceTexture,
                        width: Int,
                        height: Int
                    ) {
                        initCameraParams(width, height)
                        previewRenderer?.apply {
                            bindSurface(surface)
                            changePreviewSize()
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(
                        surface: SurfaceTexture,
                        width: Int,
                        height: Int
                    ) {
                        initCameraParams(width, height)
                        previewRenderer?.changePreviewSize()
                    }

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        stopRecording()
                        previewRenderer?.unBindSurface()
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                }
                contentView.addView(it)
                contentView.setOnTouchListener { _, event ->
                    if (event.pointerCount == 1) {
                        //focus
                        if (event.action == MotionEvent.ACTION_UP
                            && event.eventTime - event.downTime < 500
                        ) {
                            CameraEngine.getInstance()
                                .setAutoFocus(Point(event.x.toInt(), event.y.toInt()))
                        }
                        true
                    } else {
                        //zoom
                        scaleGestureDetector.onTouchEvent(event)
                    }
                }
            }
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

    private fun initCameraParams(viewWidth: Int, viewHeight: Int) {
        var isLandscape = false
        context?.let {
            if (it.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                isLandscape = true
            }
        }
        CameraParam.getInstance().initParams(
            resolutionType,
            aspectRatioType,
            viewWidth, viewHeight,
            isLandscape
        )
    }

    override fun onResume() {
        super.onResume()
        orientationController.let {
            if (it.canDetectOrientation()) {
                it.enable()
            }
        }
        sensorController.register()
    }

    override fun onPause() {
        super.onPause()
        orientationController.disable()
        sensorController.unregister()
    }

    fun startPreview() {
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

    fun switchCamera() {
        previewRenderer?.switchCamera()
    }

    /**
     * 只在竖屏模式下可设置useRotation=true
     */
    @JvmOverloads
    fun takePicture(dirPath: String, fileName: String = "pic_${System.currentTimeMillis()}.jpg", clipRect: Rect? = null, useRotation: Boolean = false) {
        val dir = if (dirPath.endsWith(File.separator)) dirPath.substring(0, dirPath.length - 1) else dirPath
        val name = if (fileName.startsWith(File.separator)) fileName.substring(1) else fileName
        File(dir).let {
            if (!it.exists() || !it.isDirectory) {
                return
            }
        }
        val filePath = "${dir}${File.separator}${name}"
        previewRenderer?.takePicture(
            SnapInfoEntity(
                filePath, if (useRotation) {
                    rotation
                } else {
                    OrientationController.ROTATION_0
                },
                clipRect
            )
        )
    }

    /**
     * 只在竖屏模式下可设置useRotation=true
     */
    @JvmOverloads
    fun startRecording(dirPath: String, fileName: String = "vod_${System.currentTimeMillis()}.mp4", useRotation: Boolean = false) {
        val dir = if (dirPath.endsWith(File.separator)) dirPath.substring(0, dirPath.length - 1) else dirPath
        val name = if (fileName.startsWith(File.separator)) fileName.substring(1) else fileName
        File(dir).let {
            if (!it.exists() || !it.isDirectory) {
                return
            }
        }
        val filePath = "${dir}${File.separator}${name}"
        previewRenderer?.startRecording(
            filePath, if (useRotation) {
                rotation
            } else {
                OrientationController.ROTATION_0
            }
        )
    }

    fun stopRecording() = previewRenderer?.stopRecording()

    fun toggleTorch() {
        (!torchOn).let {
            torchOn = it
            previewRenderer?.toggleTorch(it)
        }
    }

    fun setResolution(
        resolutionType: ResolutionType,
        aspectRatioType: AspectRatioType
    ) {
        this.resolutionType = resolutionType
        this.aspectRatioType = aspectRatioType
    }

    fun changeResolution(
        resolutionType: ResolutionType,
        aspectRatioType: AspectRatioType
    ) {
        this.resolutionType = resolutionType
        this.aspectRatioType = aspectRatioType
        previewRenderer?.changeResolution(
            resolutionType,
            aspectRatioType
        )
    }

    fun setBeautyFilter(enable: Boolean) {
        previewRenderer?.setBeautyFilter(enable)
    }

    private inner class ZoomScaleGestureDetector :
        ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scale = detector.currentSpan / minSpan
            val zoomScale = CameraEngine.getInstance().changeZoom(scale)
            onZoomChangeListener?.onZoomChange(zoomScale, false)
            return super.onScale(detector)
        }

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            minSpan = detector.currentSpan / CameraEngine.getInstance().getCurrentZoomScale()
            return super.onScaleBegin(detector)
        }

        override fun onScaleEnd(detector: ScaleGestureDetector?) {
            onZoomChangeListener?.onZoomChange(
                CameraEngine.getInstance().getCurrentZoomScale(),
                true
            )
            super.onScaleEnd(detector)
        }
    }
}