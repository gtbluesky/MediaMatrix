package com.gtbluesky.mediamatrix.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.*
import android.widget.TextView
import com.gtbluesky.mediamatrix.R
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tv_egl.setOnClickListener {
            intent.setClass(this, EGLTestActivity::class.java)
            startActivity(intent)
        }
        tv_sky_box.setOnClickListener {
            intent.setClass(this, SkyBoxActivity::class.java)
            startActivity(intent)
        }
        tv_sky_sphere.setOnClickListener {
            intent.setClass(this, SkySphereActivity::class.java)
            startActivity(intent)
        }
        tv_particle_effect.setOnClickListener {
            intent.setClass(this, ParticleEffectActivity::class.java)
            startActivity(intent)
        }
        tv_camera.setOnClickListener {
            intent.setClass(this, CameraPreviewActivity::class.java)
            startActivity(intent)
        }
        tv_camera_cut.setOnClickListener {
            intent.setClass(this, CameraCutActivity::class.java)
            startActivity(intent)
        }
        tv_record.setOnClickListener {
            intent.setClass(this, AudioRecordActivity::class.java)
            startActivity(intent)
        }
    }
}
