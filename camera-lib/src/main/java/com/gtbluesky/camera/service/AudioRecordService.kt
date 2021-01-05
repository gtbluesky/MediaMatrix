package com.gtbluesky.camera.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.HandlerThread
import android.os.IBinder
import com.gtbluesky.camera.listener.OnCompletionListener
import java.io.File

class AudioRecordService : Service() {

    private var audioThread: HandlerThread? = null
    private var audioHandler: AudioRecordHandler? = null
    var onCompletionListener: OnCompletionListener? = null

    private val binder: IBinder by lazy {
        AudioRecordBinder()
    }

    override fun onBind(intent: Intent?) = binder

    @JvmOverloads
    fun startRecord(dirPath: String, fileName: String = "audio_${System.currentTimeMillis()}") {
        audioThread = HandlerThread("AudioRecordThread").apply {
            start()
            audioHandler = AudioRecordHandler(looper)
            val dir = if (dirPath.endsWith(File.separator)) dirPath.substring(0, dirPath.length - 1) else dirPath
            val name = if (fileName.startsWith(File.separator)) fileName.substring(1) else fileName
            File(dir).let { 
                if (!it.exists() || !it.isDirectory) {
                    return
                }
            }
            val path = "${dir}${File.separator}${name}"
            audioHandler?.let {
                it.sendMessage(it.obtainMessage(AudioRecordHandler.MSG_START, path))
                it.onCompletionListener = onCompletionListener
            }
        }
    }

    fun stopRecord() {
        audioHandler?.apply {
            sendMessage(obtainMessage(AudioRecordHandler.MSG_STOP))
        }
        audioHandler = null
    }

    fun pauseRecord() {
        audioHandler?.apply {
            sendMessage(obtainMessage(AudioRecordHandler.MSG_PAUSE))
        }
    }

    fun resumeRecord() {
        audioHandler?.apply {
            sendMessage(obtainMessage(AudioRecordHandler.MSG_RESUME))
        }
    }

    fun getDuration() = audioHandler?.presentationTimeUs ?: 0L

    fun getState() = audioHandler?.state

    inner class AudioRecordBinder : Binder() {
        fun getService() = this@AudioRecordService
    }
}