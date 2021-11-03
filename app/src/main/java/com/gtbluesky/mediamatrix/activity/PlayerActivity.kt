package com.gtbluesky.mediamatrix.activity

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.gtbluesky.mediamatrix.databinding.ActivityPlayerBinding
import com.gtbluesky.mediamatrix.player.MatrixPlayer
import com.gtbluesky.mediamatrix.player.bean.TimeInfoBean
import com.gtbluesky.mediamatrix.player.enums.SoundChannelEnum
import com.gtbluesky.mediamatrix.player.listener.*
import com.gtbluesky.mediamatrix.player.util.LogUtil
import com.gtbluesky.mediamatrix.player.util.TimeUtil

class PlayerActivity : AppCompatActivity() {

    private var mediaPlayer: MatrixPlayer? = null
    private lateinit var viewBinding: ActivityPlayerBinding
    private var position = 0
    private var isSeekBar = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        mediaPlayer = MatrixPlayer().apply {
            setVideoView(viewBinding.videoView)
            setVolume(80)
            setPitch(1.0f)
            setSpeed(1.0f)
            setSoundChannel(SoundChannelEnum.CENTER)
        }
        viewBinding.tvVolume.text = "音量：${mediaPlayer?.volumePercent ?: 0}%"
        viewBinding.seekbarVolume.progress = mediaPlayer?.volumePercent ?: 0
        setListener()
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer?.stop()
    }

    private fun setListener() {
        mediaPlayer?.setOnPreparedListener(object : OnPreparedListener {
            override fun onPrepared() {
                LogUtil.d("onPrepared")
                mediaPlayer?.start()
            }
        })
        mediaPlayer?.setOnLoadListener(object : OnLoadListener {
            override fun onLoad(isLoading: Boolean) {
                if (isLoading) {
                    LogUtil.d("加载中...")
                } else {
                    LogUtil.d("播放中...")
                }
            }
        })
        mediaPlayer?.setOnPauseListener(object : OnPauseListener {
            override fun onPause(isPaused: Boolean) {
                if (isPaused) {
                    LogUtil.d("暂停中...")
                } else {
                    LogUtil.d("播放中...")
                }
            }
        })
        mediaPlayer?.setOnTimeInfoListener(object : OnTimeInfoListener {
            override fun onTimeInfo(bean: TimeInfoBean) {
                if (!isSeekBar) {
                    runOnUiThread {
                        viewBinding.tvTime.text = (TimeUtil.getFormatTime(bean.currentTime)
                                + "/"
                                + TimeUtil.getFormatTime(bean.totalTime))
                        if (bean.totalTime == 0) {
                            viewBinding.seekbarSeek.progress = 0
                        } else {
                            viewBinding.seekbarSeek.progress = bean.currentTime * 100 / bean.totalTime
                        }
                    }
                }
            }
        })
        mediaPlayer?.setOnErrorListener(object : OnErrorListener {
            override fun onError(code: Int, msg: String?) {
                LogUtil.d("code: $code, msg: $msg")
            }
        })
        mediaPlayer?.setOnCompleteListener(object : OnCompleteListener{
            override fun onComplete() {
                LogUtil.d("播放完成")
            }
        })
        viewBinding.seekbarSeek.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (mediaPlayer?.duration ?: 0 > 0 && isSeekBar) {
                    position = (mediaPlayer?.duration ?: 0) * progress / 100
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                isSeekBar = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                mediaPlayer?.seek(position)
                isSeekBar = false
            }
        })
        viewBinding.seekbarVolume.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                mediaPlayer?.setVolume(progress)
                viewBinding.tvTime.text = "音量：${mediaPlayer?.volumePercent ?: 0}%"
                LogUtil.d("progress is $progress")
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        mediaPlayer?.setOnVolumeDBListener(object : OnVolumeDBListener {
            override fun onDbValue(value: Int) {
                runOnUiThread { viewBinding.tvDb.text = "${value.toString()}db" }
            }
        })
        mediaPlayer?.setOnRecordTimeListener(object : OnRecordTimeListener{
            override fun onRecordTime(recordTime: Int) {
                LogUtil.d("录音时长(ms): $recordTime")
            }
        })
    }

    fun begin(view: View?) {
//        mediaPlayer?.setSource("file:///sdcard/movie.mp4");
//        mediaPlayer.setDataSource("http://ivi.bupt.edu.cn/hls/cctv1hd.m3u8");
//        mediaPlayer.setDataSource("http://mpge.5nd.com/2015/2015-11-26/69708/1.mp3");
        mediaPlayer?.setSource("https://www.w3school.com.cn/i/movie.mp4")
//        mediaPlayer?.setSource("http://vjs.zencdn.net/v/oceans.mp4")
        //        audioPlayer.setSource("http://ngcdn004.cnr.cn/live/dszs/index.m3u8");
        mediaPlayer?.prepare()
    }

    fun pause(view: View?) {
        mediaPlayer?.pause()
    }

    fun resume(view: View?) {
        mediaPlayer?.resume()
    }

    fun stop(view: View?) {
        mediaPlayer?.stop()
    }

    fun next(view: View?) {
        mediaPlayer?.playNext("http://ngcdn004.cnr.cn/live/dszs/index.m3u8")
    }

    fun left(view: View?) {
        mediaPlayer?.setSoundChannel(SoundChannelEnum.LEFT)
    }

    fun right(view: View?) {
        mediaPlayer?.setSoundChannel(SoundChannelEnum.RIGHT)
    }

    fun center(view: View?) {
        mediaPlayer?.setSoundChannel(SoundChannelEnum.CENTER)
    }

    fun speed(view: View?) {
        mediaPlayer?.setSpeed(1.5f)
        mediaPlayer?.setPitch(1.0f)
    }

    fun pitch(view: View?) {
        mediaPlayer?.setPitch(1.5f)
        mediaPlayer?.setSpeed(1.0f)
    }

    fun speedpitch(view: View?) {
        mediaPlayer?.setSpeed(1.5f)
        mediaPlayer?.setPitch(1.5f)
    }

    fun normalspeedpitch(view: View?) {
        mediaPlayer?.apply {
            setSpeed(1.0f)
            setPitch(1.0f)
        }
    }
}