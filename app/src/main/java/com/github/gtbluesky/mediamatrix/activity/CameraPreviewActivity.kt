package com.github.gtbluesky.mediamatrix.activity

import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.gtbluesky.mediamatrix.R
import com.github.gtbluesky.camera.CameraPreviewFragment
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.Permission

class CameraPreviewActivity :
    AppCompatActivity(), View.OnClickListener {

    private lateinit var switchIv: ImageView
    private lateinit var flashIv: ImageView
    private lateinit var recordIv: ImageView
    private lateinit var cameraPreviewFragment: CameraPreviewFragment

    companion object {
        private const val FRAGMENT_CAMERA = "fragment_camera"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setWindowFlag()
        setContentView(R.layout.activity_camera_preview)
        initView()
        setListener()
        startPreview()
    }

    private fun startPreview() {
        AndPermission
            .with(this)
            .runtime()
            .permission(
                Permission.Group.CAMERA,
                Permission.Group.MICROPHONE,
                Permission.Group.STORAGE
            ).onGranted {
                CameraPreviewFragment.newInstance(previewNow = true).let {
                    cameraPreviewFragment = it
                    supportFragmentManager
                        .beginTransaction()
                        .replace(
                            R.id.fl_container,
                            it,
                            FRAGMENT_CAMERA
                        ).addToBackStack(FRAGMENT_CAMERA)
                        .commit()
                }
            }.onDenied {
                Toast.makeText(this, "请设置权限", Toast.LENGTH_SHORT).show()
                finish()
            }.start()
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

    override fun onBackPressed() {
        supportFragmentManager.backStackEntryCount.let {
            when {
                it > 1 -> supportFragmentManager.popBackStack()
                it == 1 -> {
                    finish()
                    overridePendingTransition(0, R.anim.anim_slide_down)
                }
                else -> super.onBackPressed()
            }
        }

    }

    private fun initView() {
        switchIv = findViewById(R.id.switch_iv)
        flashIv = findViewById(R.id.flash_iv)
        recordIv = findViewById(R.id.record_iv)
    }

    private fun setListener() {
        switchIv.setOnClickListener(this)
        flashIv.setOnClickListener(this)
        recordIv.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.switch_iv -> cameraPreviewFragment.switchCamera()
            R.id.flash_iv -> cameraPreviewFragment.toggleTorch()
            R.id.record_iv -> cameraPreviewFragment.takePicture(
                "${Environment.getExternalStorageDirectory().absolutePath}/pic_${System.currentTimeMillis()}.jpg"
            )
        }
    }

}