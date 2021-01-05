package com.gtbluesky.mediamatrix.activity

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gtbluesky.camera.listener.OnCompletionListener
import com.gtbluesky.camera.service.AudioRecordService
import com.gtbluesky.mediamatrix.databinding.ActivityAudioRecordBinding
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.Permission

class AudioRecordActivity : AppCompatActivity() {
    private var audioService: AudioRecordService? = null
    private lateinit var binding: ActivityAudioRecordBinding
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            audioService = (service as? AudioRecordService.AudioRecordBinder)?.getService()?.also {
                if (it.getDuration() != 0L) {
                    binding.tvTime.text = "状态：${it.getState()}，时长：${it.getDuration() / 1000f}ms"
                }
                it.onCompletionListener = object : OnCompletionListener {
                    override fun onCompletion(savePath: String) {
                        runOnUiThread {
                            Toast.makeText(
                                this@AudioRecordActivity,
                                "文件路径为：$savePath",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {

        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAudioRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setListener()
        AndPermission
            .with(this)
            .runtime()
            .permission(
                Permission.Group.MICROPHONE,
                Permission.Group.STORAGE
            ).onDenied {
                Toast.makeText(this, "请授予权限", Toast.LENGTH_SHORT).show()
                finish()
            }.start()
    }

    private fun setListener() {
        binding.tvStart.setOnClickListener {
            if (audioService?.getDuration() == 0L) {
                audioService?.startRecord(Environment.getExternalStorageDirectory().absolutePath)
            }
            binding.tvStart.text = "进行中"
        }
        binding.tvStop.setOnClickListener {
            audioService?.stopRecord()
            Toast.makeText(this, "已停止", Toast.LENGTH_SHORT).show()
        }
        binding.tvTime.setOnClickListener {
            audioService?.let {
                binding.tvTime.text = "状态：${it.getState()}，时长：${it.getDuration() / 1000f}ms"
            }
        }
        binding.tvPause.setOnClickListener {
            audioService?.pauseRecord()
        }
        binding.tvResume.setOnClickListener {
            audioService?.resumeRecord()
        }
    }

    override fun onResume() {
        super.onResume()
        val intent = Intent(this, AudioRecordService::class.java)
        startService(intent)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }

    override fun onPause() {
        super.onPause()
        unbindService(serviceConnection)
    }
}