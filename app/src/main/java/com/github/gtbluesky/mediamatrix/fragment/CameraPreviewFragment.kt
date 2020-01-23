package com.github.gtbluesky.mediamatrix.fragment

import android.app.Activity
import android.content.Context
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.github.gtbluesky.camera.render.PreviewRenderer
import com.github.gtbluesky.mediamatrix.R
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.Permission

class CameraPreviewFragment : Fragment(), View.OnClickListener {

    private var mActivity: Activity? = null
    private lateinit var mContext: Context
    private lateinit var mContentView: ViewGroup
    private lateinit var mSurfaceView: SurfaceView
    private lateinit var mTextureView: TextureView
//    private lateinit var mCameraView: GLSurfaceView
    private lateinit var mPreviewRenderer: PreviewRenderer
    private lateinit var mSwitchIv: ImageView
    private lateinit var mFlashIv: ImageView
    private lateinit var mRecordIv: ImageView
    private var mTorchOn = false

    companion object {
        private val TAG = CameraPreviewFragment::class.java.simpleName

        fun newInstance(bundle: Bundle? = null): Fragment {
            val fragment = CameraPreviewFragment()
            bundle?.let {
                fragment.arguments = it
            }
            return fragment
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = activity
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mContentView = inflater.inflate(
            R.layout.camera_filter_layout,
            container,
            false
        ) as ViewGroup
        return mContentView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        AndPermission
            .with(this)
            .runtime().permission(
                Permission.Group.CAMERA,
                Permission.Group.MICROPHONE,
                Permission.Group.STORAGE
            ).onGranted {
                initView()
            }.onDenied {
                Toast.makeText(mContext, "请设置权限", Toast.LENGTH_SHORT).show()
                mActivity?.finish()
            }.start()
    }

    private fun initView() {
        mPreviewRenderer = PreviewRenderer(mContext)
        mTextureView = TextureView(mContext)
//        mCameraView = GLSurfaceView(mContext)
//        mCameraView.setEGLContextClientVersion(2)
        mContentView.addView(mTextureView, 0)
        mSwitchIv = mContentView.findViewById(R.id.switch_iv)
        mFlashIv = mContentView.findViewById(R.id.flash_iv)
        mRecordIv = mContentView.findViewById(R.id.record_iv)

        mTextureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                mPreviewRenderer.bindSurface(surface)
                mPreviewRenderer.changePreviewSize(width, height)
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture?,
                width: Int,
                height: Int
            ) {
                mPreviewRenderer.changePreviewSize(width, height)
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
                mPreviewRenderer.unBindSurface()
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
//                mGLLooper.postMessage(GLLooper.MSG_CHANGE, width, height)

            }
        }

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

        setListener()
    }

    private fun setListener() {
        mSwitchIv.setOnClickListener(this)
        mFlashIv.setOnClickListener(this)
        mRecordIv.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.switch_iv -> mPreviewRenderer.switchCamera()
//            R.id.flash_iv -> mPreviewRenderer.toggleTorch(!mTorchOn.apply { mTorchOn = !this })
            R.id.record_iv -> mPreviewRenderer.takePicture()
        }
    }
}