package com.gtbluesky.camera.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.HandlerThread
import android.os.IBinder

class AudioRecordService : Service() {

    private var audioThread: HandlerThread? = null
    private var audioHandler: AudioRecordHandler? = null

    private val binder: IBinder by lazy {
        AudioRecordBinder()
    }

    override fun onBind(intent: Intent?) = binder

    fun startRecord(savePath: String) {
        audioThread = HandlerThread("AudioRecordThread").apply {
            start()
            audioHandler = AudioRecordHandler(looper)
            audioHandler?.apply {
                sendMessage(obtainMessage(AudioRecordHandler.MSG_START, savePath))
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