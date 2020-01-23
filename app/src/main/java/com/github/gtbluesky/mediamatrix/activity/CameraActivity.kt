package com.github.gtbluesky.mediamatrix.activity

import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.github.gtbluesky.camera.engine.Camera2Engine
import com.github.gtbluesky.mediamatrix.R

class CameraActivity : AppCompatActivity() {

    private lateinit var mCameraView: SurfaceView
    private lateinit var mSwitchIv: ImageView
    private lateinit var mFlashIv: ImageView
    private var mTorchOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setWindowFlag()
        setContentView(R.layout.camera_layout)
        mCameraView = findViewById(R.id.camera_sv)
        mSwitchIv = findViewById(R.id.switch_iv)
        mFlashIv = findViewById(R.id.flash_iv)

        mCameraView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder?) {
                Camera2Engine.getInstance().startPreview(this@CameraActivity, mCameraView.width, mCameraView.height)
            }

            override fun surfaceChanged(
                holder: SurfaceHolder?,
                format: Int,
                width: Int,
                height: Int
            ) {

            }

            override fun surfaceDestroyed(holder: SurfaceHolder?) {
                Camera2Engine.getInstance().stopPreview()
            }
        })

        mSwitchIv.setOnClickListener {
            Camera2Engine.getInstance().switchCamera(this)
        }

        mFlashIv.setOnClickListener {
            Camera2Engine.getInstance().toggleTorch(!mTorchOn.apply { mTorchOn = !this })
        }
    }

    private fun setWindowFlag() {
        window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            attributes = attributes.apply {
                systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE
            }
        }

    }

//    override fun onResume() {
//        super.onResume()
//    }
//
//    override fun onPause() {
//        super.onPause()
//        mCameraController.stopPreview()
//    }

    override fun onDestroy() {
        super.onDestroy()
        Camera2Engine.getInstance().destroy()
    }


}