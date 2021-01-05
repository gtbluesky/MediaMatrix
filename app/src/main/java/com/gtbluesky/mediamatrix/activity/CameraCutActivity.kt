package com.gtbluesky.mediamatrix.activity

import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gtbluesky.camera.AspectRatioType
import com.gtbluesky.camera.MatrixCameraFragment
import com.gtbluesky.camera.ResolutionType
import com.gtbluesky.camera.listener.OnZoomChangeListener
import com.gtbluesky.mediamatrix.R
import com.gtbluesky.mediamatrix.databinding.ActivityCameraCutBinding
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.Permission

class CameraCutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraCutBinding
    private lateinit var cameraFragment: MatrixCameraFragment

    companion object {
        private const val FRAGMENT_CAMERA = "fragment_camera"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraCutBinding.inflate(layoutInflater)
        setContentView(binding.root)
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
            lp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
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
            takePicture()
        }
    }

    private fun takePicture() {
        val rect = Rect()
        binding.viewMask.let {
            rect.left = it.left
            rect.top = it.top
            rect.right = it.right
            rect.bottom = it.bottom
        }
        cameraFragment.takePicture(
            Environment.getExternalStorageDirectory().absolutePath,
            "pic_${System.currentTimeMillis()}.jpg",
            Rect(rect)
        )
        binding.viewMask.let {
            showToast("照片已保存, ${it.left}, ${it.top},${it.right}, ${it.bottom}")
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

}