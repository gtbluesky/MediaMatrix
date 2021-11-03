package com.gtbluesky.mediamatrix.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.gtbluesky.mediamatrix.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val intent = Intent()
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
        binding.tvPlayer.setOnClickListener {
            intent.setClass(this, PlayerActivity::class.java)
            startActivity(intent)
        }
    }
}
