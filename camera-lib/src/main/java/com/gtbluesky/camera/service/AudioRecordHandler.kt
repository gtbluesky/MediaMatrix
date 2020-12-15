package com.gtbluesky.camera.service

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import com.gtbluesky.codec.CodecParam
import com.gtbluesky.codec.CodecUtil
import java.io.File
import java.io.FileOutputStream

class AudioRecordHandler(looper: Looper) : Handler(looper) {
    private val codecParam = CodecParam.getInstance()
    private val minBufferSize: Int
    private var audioRecord: AudioRecord? = null
    private var fos: FileOutputStream? = null
    private var savePath: String? = null
    var presentationTimeUs = 0L
        private set
    private var totalReadedBytes = 0
    var state = RecordState.PENDING

    companion object {
        const val MSG_START = 0x1
        const val MSG_RECORDING = 0x2
        const val MSG_STOP = 0x3
        const val MSG_QUIT = 0x4
        const val MSG_PAUSE = 0x5
        const val MSG_RESUME = 0x6

        private const val SAMPLE_RATE = 44100
        private val TAG = this::class.simpleName
    }

    init {
        minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize
        )
    }

    override fun handleMessage(msg: Message) {
        when (msg.what) {
            MSG_START -> {
                handleStart(msg.obj as String)
            }
            MSG_RECORDING -> {
                handleAudioData(false)
            }
            MSG_STOP -> {
                handleAudioData(true)
            }
            MSG_PAUSE -> {
                handlePause()
            }
            MSG_RESUME -> {
                handleResume()
            }
            MSG_QUIT -> {
                looper.quit()
            }
        }
    }

    private fun handleStart(path: String) {
        if (state != RecordState.PENDING) {
            return
        }
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) {
            Log.e(TAG, "start error caused by using a incorrect directory path")
            return
        }
        audioRecord?.startRecording()
        state = RecordState.ONGOING
        savePath = dir.absolutePath + File.separator + "audio_${System.currentTimeMillis()}"
        fos = FileOutputStream(File("$savePath.pcm"))
        sendMessage(obtainMessage(MSG_RECORDING))
    }

    private fun handleAudioData(endOfStream: Boolean) {
        if (state != RecordState.ONGOING) {
            return
        }
        if (endOfStream) {
            removeMessages(MSG_RECORDING)
            audioRecord?.stop()
            while (!drainAudioData()) {
            }
            audioRecord?.release()
            audioRecord = null
            fos?.close()
            fos = null
            presentationTimeUs = 0
            state = RecordState.COMPLETED
            CodecUtil.pcm2Wav(
                "$savePath.pcm",
                "$savePath.wav",
                codecParam.sampleRate,
                codecParam.channelCount,
                minBufferSize
            )
            File("$savePath.pcm").delete()
            sendMessage(obtainMessage(MSG_QUIT))
        } else {
            drainAudioData()
            sendMessage(obtainMessage(MSG_RECORDING))
        }
    }

    private fun drainAudioData(): Boolean {
        val pcmData = ByteArray(minBufferSize)
        val length = audioRecord?.read(pcmData, 0, minBufferSize) ?: 0
        if (length > 0) {
            totalReadedBytes += length
            presentationTimeUs =
                1000000L * totalReadedBytes / (codecParam.channelCount * 2 * codecParam.sampleRate)
            fos?.write(pcmData)
            return false
        }
        return true
    }

    private fun handlePause() {
        if (state != RecordState.ONGOING) {
            return
        }
        state = RecordState.SUSPENDED
        removeMessages(MSG_RECORDING)
        audioRecord?.stop()
    }

    private fun handleResume() {
        if (state != RecordState.SUSPENDED) {
            return
        }
        audioRecord?.startRecording()
        state = RecordState.ONGOING
        sendMessage(obtainMessage(MSG_RECORDING))
    }
}