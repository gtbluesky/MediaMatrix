package com.gtbluesky.mediamatrix.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.*
import android.widget.TextView
import com.gtbluesky.mediamatrix.R
import com.gtbluesky.mediamatrix.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.tvEgl.setOnClickListener {
            intent.setClass(this, EGLTestActivity::class.java)
            startActivity(intent)
        }
        binding.tvSkyBox.setOnClickListener {
            intent.setClass(this, SkyBoxActivity::class.java)
            startActivity(intent)
        }
        binding.tvSkySphere.setOnClickListener {
            intent.setClass(this, SkySphereActivity::class.java)
            startActivity(intent)
        }
        binding.tvParticleEffect.setOnClickListener {
            intent.setClass(this, ParticleEffectActivity::class.java)
            startActivity(intent)
        }
        binding.tvCamera.setOnClickListener {
            intent.setClass(this, CameraPreviewActivity::class.java)
            startActivity(intent)
        }
        binding.tvCameraCut.setOnClickListener {
            intent.setClass(this, CameraCutActivity::class.java)
            startActivity(intent)
        }
        binding.tvRecord.setOnClickListener {
            intent.setClass(this, AudioRecordActivity::class.java)
            startActivity(intent)
        }
    }
}
