package com.gtbluesky.mediamatrix.activity

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gtbluesky.camera.AspectRatioType
import com.gtbluesky.camera.MatrixCameraFragment
import com.gtbluesky.camera.ResolutionType
import com.gtbluesky.camera.listener.OnCompletionListener
import com.gtbluesky.camera.listener.OnZoomChangeListener
import com.gtbluesky.mediamatrix.R
import com.gtbluesky.mediamatrix.databinding.ActivityCameraPreviewBinding
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.Permission

class CameraPreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraPreviewBinding
    private lateinit var cameraFragment: MatrixCameraFragment

    companion object {
        private const val FRAGMENT_CAMERA = "fragment_camera"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.recordIv.drawable.level = 1
        binding.beautyIv.alpha = 0.5f
        setListener()
        openCamera()
    }

    override fun onStart() {
        super.onStart()
        setFullScreen()
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
                val bundle = Bundle().also {
                    it.putBoolean(MatrixCameraFragment.PREVIEW_NOW, true)
                }
                MatrixCameraFragment.newInstance(bundle).let {
                    cameraFragment = it
                    it.setResolution(ResolutionType.R_720, AspectRatioType.FULL)
                    it.onZoomChangeListener = object : OnZoomChangeListener {
                        override fun onZoomChange(scale: Float, completed: Boolean) {
                            binding.tvZoom.text = "${String.format("%.1f", scale)}X"
                            binding.tvZoom.visibility = if (completed) {
                                View.GONE
                            } else {
                                View.VISIBLE
                            }
                        }
                    }
                    it.onCompletionListener = object : OnCompletionListener {
                        override fun onCompletion(savePath: String) {
                            runOnUiThread {
                                Toast.makeText(this@CameraPreviewActivity, "文件路径为：$savePath", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
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
                Toast.makeText(this, "请授予权限", Toast.LENGTH_SHORT).show()
                finish()
            }.start()
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

    private fun setFullScreen() {
        // 刘海屏处理
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val lp = window.attributes
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = lp
        }
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    private fun setListener() {
        binding.switchIv.setOnClickListener {
            cameraFragment.switchCamera()
        }
        binding.flashIv.setOnClickListener {
            cameraFragment.toggleTorch()
        }
        binding.recordIv.setOnClickListener {
            if (binding.recordIv.drawable.level == 1) {
                takePicture()
            } else {
                stopRecord()
            }
        }
        binding.recordIv.setOnLongClickListener {
            if (binding.recordIv.drawable.level == 1) {
                startRecord()
            }
            true
        }
//        CameraParam.getInstance().onCameraFocusListener = object : OnCameraFocusListener {
//            override fun onCameraFocus(success: Boolean) {
//                showToast("$success")
//            }
//        }
        binding.beautyIv.setOnClickListener {
            if (it.alpha != 1f) {
                cameraFragment.setBeautyFilter(true)
                it.alpha = 1f
            } else {
                cameraFragment.setBeautyFilter(false)
                it.alpha = 0.5f
            }
        }
    }

    private fun takePicture() {
        cameraFragment.takePicture(
            Environment.getExternalStorageDirectory().absolutePath
        )
        showToast("照片已保存")
    }

    private fun startRecord() {
        showToast("开始录像")
        binding.recordIv.drawable.level = 2
        binding.chronometer.let {
            it.base = SystemClock.elapsedRealtime()
            it.start()
            it.setTextColor(Color.RED)
        }
        cameraFragment.startRecording(
            Environment.getExternalStorageDirectory().absolutePath
        )
    }

    private fun stopRecord() {
        binding.recordIv.drawable.level = 1
        binding.chronometer.let {
            it.base = SystemClock.elapsedRealtime()
            it.stop()
            it.setTextColor(Color.WHITE)
        }
        cameraFragment.stopRecording()
        showToast("视频已保存")
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

}