package com.gtbluesky.mediamatrix.player

import android.text.TextUtils
import com.gtbluesky.mediamatrix.player.bean.TimeInfoBean
import com.gtbluesky.mediamatrix.player.enums.SoundChannelEnum
import com.gtbluesky.mediamatrix.player.listener.*
import com.gtbluesky.mediamatrix.player.util.LogUtil
import com.gtbluesky.mediamatrix.player.view.VideoView
import java.io.File

class MatrixPlayer {
    companion object {
        private var duration = -1
        private var volumePercent = 100
        private var speed = 1.0f
        private var pitch = 1.0f
        private var soundChannelEnum = SoundChannelEnum.CENTER

        init {
            System.loadLibrary("matrixplayer")
        }
    }

    private var mediaSrc: String? = null
    private var playNext = false
    private var onPreparedListener: OnPreparedListener? = null
    private var onLoadListener: OnLoadListener? = null
    private var onPauseListener: OnPauseListener? = null
    private var onTimeInfoListener: OnTimeInfoListener? = null
    private var onErrorListener: OnErrorListener? = null
    private var onCompleteListener: OnCompleteListener? = null
    private var onVolumeDBListener: OnVolumeDBListener? = null
    private var timeInfoBean: TimeInfoBean? = null
    private var aacEncode: AACEncode? = null
    private var videoView: VideoView? = null
    private val viewSizeAdjust = false

    fun setSource(source: String?) {
        mediaSrc = source
    }

    fun setVideoView(videoView: VideoView?) {
        this.videoView = videoView
    }

    fun prepare() {
        if (TextUtils.isEmpty(mediaSrc)) {
            LogUtil.e("source not be empty!")
            return
        }
        Thread { nativePrepare(mediaSrc) }.start()
    }

    fun start() {
        if (TextUtils.isEmpty(mediaSrc)) {
            LogUtil.e("source is empty")
            return
        }
        Thread {
            setVolume(Companion.volumePercent)
            setSoundChannel(soundChannelEnum)
            nativeStart()
        }.start()
    }

    fun pause() {
        nativePause()
        onPauseListener?.onPause(true)
    }

    fun resume() {
        nativeResume()
        onPauseListener?.onPause(false)
    }

    fun stop() {
        timeInfoBean = null
        Companion.duration = -1
        stopRecord()
        Thread { nativeStop() }.start()
    }

    fun seek(seconds: Int) {
        nativeSeek(seconds)
    }

    fun playNext(url: String?) {
        mediaSrc = url
        playNext = true
        stop()
    }

    val duration: Int
        get() {
            if (Companion.duration < 0) {
                Companion.duration = nativeGetDuration()
            }
            return Companion.duration
        }

    fun setVolume(percent: Int) {
        if (percent in 0..100) {
            Companion.volumePercent = percent
            nativeSetVolume(percent)
        }
    }

    fun setSoundChannel(soundChannel: SoundChannelEnum) {
        soundChannelEnum = soundChannel
        nativeSetSoundChannel(soundChannel.ordinal)
    }

    val volumePercent: Int
        get() = Companion.volumePercent

    fun setPitch(p: Float) {
        pitch = p
        nativePitch(p)
    }

    fun setSpeed(s: Float) {
        speed = s
        nativeSpeed(s)
    }

    fun startRecord(filePath: String?) {
        val sampleRate = nativeGetSampleRate()
        if (aacEncode != null || sampleRate <= 0) {
            return
        }
        aacEncode = AACEncode()
        aacEncode?.initMediacodec(sampleRate, File(filePath))
        nativeControlRecord(true)
        LogUtil.d("开始录制")
    }

    fun stopRecord() {
        if (aacEncode == null) {
            return
        }
        nativeControlRecord(false)
        aacEncode?.releaseMediacodec()
        aacEncode = null
        LogUtil.d("结束录制")
    }

    fun pauseRecord() {
        nativeControlRecord(false)
        LogUtil.d("暂停录制")
    }

    fun resumeRecord() {
        nativeControlRecord(true)
        LogUtil.d("继续录制")
    }

    fun setOnPreparedListener(listener: OnPreparedListener?) {
        onPreparedListener = listener
    }

    fun setOnLoadListener(listener: OnLoadListener?) {
        onLoadListener = listener
    }

    fun setOnPauseListener(listener: OnPauseListener?) {
        onPauseListener = listener
    }

    fun setOnTimeInfoListener(listener: OnTimeInfoListener?) {
        onTimeInfoListener = listener
    }

    fun setOnErrorListener(listener: OnErrorListener?) {
        onErrorListener = listener
    }

    fun setOnCompleteListener(listener: OnCompleteListener?) {
        onCompleteListener = listener
    }

    fun setOnVolumeDBListener(listener: OnVolumeDBListener?) {
        onVolumeDBListener = listener
    }

    fun setOnRecordTimeListener(listener: OnRecordTimeListener?) {
        aacEncode?.setOnRecordTimeListener(listener)
    }

    /**
     * C++回调Java方法
     */
    fun onCallPrepared() {
        onPreparedListener?.onPrepared()
    }

    fun onCallLoad(isLoading: Boolean) {
        onLoadListener?.onLoad(isLoading)
    }

    fun onCallTimeInfo(currentTime: Int, totalTime: Int) {
        if (timeInfoBean == null) {
            timeInfoBean = TimeInfoBean()
        }
        timeInfoBean?.let {
            it.currentTime = currentTime
            it.totalTime = totalTime
            onTimeInfoListener?.onTimeInfo(it)
        }
    }

    fun onCallError(code: Int, msg: String?) {
        stop()
        onErrorListener?.onError(code, msg)
    }

    fun onCallComplete() {
        stop()
        onCompleteListener?.onComplete()
    }

    fun onCallNext() {
        if (playNext) {
            playNext = false
            prepare()
        }
    }

    fun onCallVolumeDB(value: Int) {
        onVolumeDBListener?.onDbValue(value)
    }

    fun onCallPCM2AAC(size: Int, buffer: ByteArray?) {
        aacEncode?.convertPcmToAac(size, buffer)
    }

    fun onCallRenderYUV(width: Int, height: Int, y: ByteArray?, u: ByteArray?, v: ByteArray?) {
        LogUtil.d("获取到视频的yuv数据: " + width + "x" + height)
        if (y == null || u == null || v == null) {
            return
        }
        //        if (width > 0 && height > 0 && !viewSizeAdjust) {
//            videoView.post(new Runnable() {
//                @Override
//                public void run() {
//                    viewSizeAdjust = true;
//                    ViewGroup.LayoutParams layoutParams = videoView.getLayoutParams();
//                    layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
//                    layoutParams.height = 1080 * height / width;
//                    videoView.setLayoutParams(layoutParams);
//                }
//            });
//        }
        videoView?.setYUVRenderData(width, height, y, u, v)
    }

    private external fun nativePrepare(source: String?)
    private external fun nativeStart()
    private external fun nativePause()
    private external fun nativeResume()
    private external fun nativeStop()
    private external fun nativeSeek(seconds: Int)
    private external fun nativeGetDuration(): Int
    private external fun nativeSetVolume(percent: Int)
    private external fun nativeSetSoundChannel(channel: Int)
    private external fun nativePitch(pitch: Float)
    private external fun nativeSpeed(speed: Float)
    private external fun nativeGetSampleRate(): Int
    private external fun nativeControlRecord(start: Boolean)
}