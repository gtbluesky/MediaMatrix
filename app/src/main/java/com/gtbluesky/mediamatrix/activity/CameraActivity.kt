package com.gtbluesky.mediamatrix.activity

import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.gtbluesky.camera.engine.Camera2Engine
import com.gtbluesky.mediamatrix.R

class CameraActivity : AppCompatActivity() {

    private lateinit var cameraView: SurfaceView
    private lateinit var switchIv: ImageView
    private lateinit var flashIv: ImageView
    private var torchOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setWindowFlag()
        setContentView(R.layout.camera_layout)
        cameraView = findViewById(R.id.camera_sv)
        switchIv = findViewById(R.id.switch_iv)
        flashIv = findViewById(R.id.flash_iv)

        cameraView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder?) {
                Camera2Engine.getInstance().startPreview(this@CameraActivity, cameraView.width, cameraView.height)
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

        switchIv.setOnClickListener {
            Camera2Engine.getInstance().switchCamera(this)
        }

        flashIv.setOnClickListener {
            Camera2Engine.getInstance().toggleTorch(!torchOn.apply { torchOn = !this })
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