package com.github.gtbluesky.mediamatrix.activity

import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.view.View
import android.view.WindowManager
import android.widget.Chronometer
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.postDelayed
import com.github.gtbluesky.camera.AspectRatioType
import com.github.gtbluesky.camera.MatrixCameraFragment
import com.github.gtbluesky.camera.ResolutionType
import com.github.gtbluesky.mediamatrix.R
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.Permission

class CameraPreviewActivity :
    AppCompatActivity(), View.OnClickListener {

    private lateinit var chronometer: Chronometer
    private lateinit var switchIv: ImageView
    private lateinit var flashIv: ImageView
    private lateinit var recordIv: ImageView
    private lateinit var matrixCameraFragment: MatrixCameraFragment

    companion object {
        private const val FRAGMENT_CAMERA = "fragment_camera"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setWindowFlag()
        setContentView(R.layout.activity_camera_preview)
        initView()
        setListener()
        openCamera()
    }

    private fun openCamera() {
        AndPermission
            .with(this)
            .runtime()
            .permission(
                Permission.Group.CAMERA,
                Permission.Group.MICROPHONE,
                Permission.Group.STORAGE
            ).onGranted {
                MatrixCameraFragment.newInstance(previewNow = true).let {
                    matrixCameraFragment = it
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
        chronometer = findViewById(R.id.chronometer)
        switchIv = findViewById(R.id.switch_iv)
        flashIv = findViewById(R.id.flash_iv)
        recordIv = findViewById(R.id.record_iv)
        recordIv.drawable.level = 1
    }

    private fun setListener() {
        switchIv.setOnClickListener(this)
        flashIv.setOnClickListener(this)
        recordIv.setOnClickListener(this)
        recordIv.setOnLongClickListener {
            if (recordIv.drawable.level == 1) {
                startRecord()
            }
            true
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.switch_iv -> matrixCameraFragment.switchCamera()
            R.id.flash_iv -> matrixCameraFragment.toggleTorch()
            R.id.record_iv -> {
                if (recordIv.drawable.level == 1) {
                    takePicture()
                } else {
                    stopRecord()
                }
            }
        }
    }

    private fun takePicture() {
        matrixCameraFragment.takePicture(
            "${Environment.getExternalStorageDirectory().absolutePath}/pic_${System.currentTimeMillis()}.jpg"
        )
        showToast("照片已保存")
    }

    private fun startRecord() {
        showToast("开始录像")
        recordIv.drawable.level = 2
        chronometer.let {
            it.base = SystemClock.elapsedRealtime()
            it.start()
            it.setTextColor(Color.RED)
        }
        matrixCameraFragment.startRecording("${Environment.getExternalStorageDirectory().absolutePath}/vod_${System.currentTimeMillis()}.mp4")
    }

    private fun stopRecord() {
        recordIv.drawable.level = 1
        chronometer.let {
            it.base = SystemClock.elapsedRealtime()
            it.stop()
            it.setTextColor(Color.WHITE)
        }
        matrixCameraFragment.stopRecording()
        showToast("视频已保存")
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

}