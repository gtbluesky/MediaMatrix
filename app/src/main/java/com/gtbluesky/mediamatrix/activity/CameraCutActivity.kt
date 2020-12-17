package com.gtbluesky.mediamatrix.activity

import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.view.View
import android.view.WindowManager
import android.widget.Chronometer
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gtbluesky.camera.AspectRatioType
import com.gtbluesky.camera.MatrixCameraFragment
import com.gtbluesky.camera.ResolutionType
import com.gtbluesky.camera.listener.OnZoomChangeListener
import com.gtbluesky.mediamatrix.R
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.Permission
import kotlinx.android.synthetic.main.activity_camera_cut.*

class CameraCutActivity : AppCompatActivity() {

    private lateinit var switchIv: ImageView
    private lateinit var flashIv: ImageView
    private lateinit var recordIv: ImageView
    private lateinit var zoomTv: TextView
    private lateinit var matrixCameraFragment: MatrixCameraFragment

    companion object {
        private const val FRAGMENT_CAMERA = "fragment_camera"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_cut)
        initView()
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
                MatrixCameraFragment.newInstance(previewNow = true).let {
                    matrixCameraFragment = it
                    it.setResolution(ResolutionType.R_720, AspectRatioType.FULL)
                    it.onZoomChangeListener = object : OnZoomChangeListener {
                        override fun onZoomChange(scale: Float, completed: Boolean) {
                            zoomTv.text = "${String.format("%.1f", scale)}X"
                            zoomTv.visibility = if (completed) {
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

    private fun initView() {
        switchIv = findViewById(R.id.switch_iv)
        flashIv = findViewById(R.id.flash_iv)
        recordIv = findViewById(R.id.record_iv)
        recordIv.drawable.level = 1
        zoomTv = findViewById(R.id.tv_zoom)
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
        switchIv.setOnClickListener {
            matrixCameraFragment.switchCamera()
        }
        flashIv.setOnClickListener {
            matrixCameraFragment.toggleTorch()
        }
        recordIv.setOnClickListener {
            takePicture()
        }
    }

    private fun takePicture() {
        matrixCameraFragment.takePicture(
            "${Environment.getExternalStorageDirectory().absolutePath}/pic_${System.currentTimeMillis()}.jpg",
            Rect(view_mask.left, view_mask.top, view_mask.right, view_mask.bottom)
        )
        showToast("照片已保存, ${view_mask.left}, ${view_mask.top},${view_mask.right}, ${view_mask.bottom}")
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

}