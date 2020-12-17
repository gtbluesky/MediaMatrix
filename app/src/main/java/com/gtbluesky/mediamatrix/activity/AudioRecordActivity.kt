package com.gtbluesky.mediamatrix.activity

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gtbluesky.camera.MatrixCameraFragment
import com.gtbluesky.camera.listener.OnZoomChangeListener
import com.gtbluesky.camera.service.AudioRecordService
import com.gtbluesky.mediamatrix.R
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.Permission
import kotlinx.android.synthetic.main.activity_audio_record.*

class AudioRecordActivity : AppCompatActivity() {
    private var audioService: AudioRecordService? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            audioService = (service as? AudioRecordService.AudioRecordBinder)?.getService()?.also {
                if (it.getDuration() != 0L) {
                    tv_time.text = "状态：${it.getState()}，时长：${it.getDuration() / 1000f}ms"
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {

        }

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_record)
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
        tv_start.setOnClickListener {
            if (audioService?.getDuration() == 0L) {
                audioService?.startRecord(Environment.getExternalStorageDirectory().absolutePath)
            }
            tv_start.text = "进行中"
        }
        tv_stop.setOnClickListener {
            audioService?.stopRecord()
            Toast.makeText(this, "已停止", Toast.LENGTH_SHORT).show()
        }
        tv_time.setOnClickListener {
            audioService?.let {
                tv_time.text = "状态：${it.getState()}，时长：${it.getDuration() / 1000f}ms"
            }
        }
        tv_pause.setOnClickListener {
            audioService?.pauseRecord()
        }
        tv_resume.setOnClickListener {
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