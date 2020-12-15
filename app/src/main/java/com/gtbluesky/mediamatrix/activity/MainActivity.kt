package com.gtbluesky.mediamatrix.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.*
import android.widget.TextView
import com.gtbluesky.mediamatrix.R
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var eglTv: TextView
    private lateinit var skyBoxTv: TextView
    private lateinit var skySphereTv: TextView
    private lateinit var particleEffectTv: TextView
    private lateinit var cameraTv: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        eglTv = findViewById(R.id.tv_egl)
        skyBoxTv = findViewById(R.id.tv_sky_box)
        skySphereTv = findViewById(R.id.tv_sky_sphere)
        particleEffectTv = findViewById(R.id.tv_particle_effect)
        cameraTv = findViewById(R.id.tv_camera)
        eglTv.setOnClickListener(this)
        skyBoxTv.setOnClickListener(this)
        skySphereTv.setOnClickListener(this)
        particleEffectTv.setOnClickListener(this)
        cameraTv.setOnClickListener(this)
        tv_record.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        val intent = Intent()
        when (v?.id) {
            R.id.tv_egl -> {
                intent.setClass(this, EGLTestActivity::class.java)
                startActivity(intent)
            }
            R.id.tv_sky_box -> {
                intent.setClass(this, SkyBoxActivity::class.java)
                startActivity(intent)
            }
            R.id.tv_sky_sphere -> {
                intent.setClass(this, SkySphereActivity::class.java)
                startActivity(intent)
            }
            R.id.tv_particle_effect -> {
                intent.setClass(this, ParticleEffectActivity::class.java)
                startActivity(intent)
            }
            R.id.tv_camera -> {
                intent.setClass(this, CameraPreviewActivity::class.java)
                startActivity(intent)
            }
            R.id.tv_record -> {
                intent.setClass(this, AudioRecordActivity::class.java)
                startActivity(intent)
            }
        }
    }

}
