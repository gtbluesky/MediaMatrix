package com.gtbluesky.mediamatrix.player;

import android.text.TextUtils;

import com.gtbluesky.mediamatrix.player.bean.TimeInfoBean;
import com.gtbluesky.mediamatrix.player.enums.SoundChannelEnum;
import com.gtbluesky.mediamatrix.player.listener.OnCompleteListener;
import com.gtbluesky.mediamatrix.player.listener.OnErrorListener;
import com.gtbluesky.mediamatrix.player.listener.OnLoadListener;
import com.gtbluesky.mediamatrix.player.listener.OnPauseListener;
import com.gtbluesky.mediamatrix.player.listener.OnPreparedListener;
import com.gtbluesky.mediamatrix.player.listener.OnRecordTimeListener;
import com.gtbluesky.mediamatrix.player.listener.OnTimeInfoListener;
import com.gtbluesky.mediamatrix.player.listener.OnVolumeDBListener;
import com.gtbluesky.mediamatrix.player.util.LogUtil;
import com.gtbluesky.mediamatrix.player.view.VideoView;

import java.io.File;

public class MatrixPlayer {
    static {
        System.loadLibrary("matrixplayer");
    }

    private String mediaSrc;
    private boolean playNext;
    private OnPreparedListener onPreparedListener;
    private OnLoadListener onLoadListener;
    private OnPauseListener onPauseListener;
    private OnTimeInfoListener onTimeInfoListener;
    private OnErrorListener onErrorListener;
    private OnCompleteListener onCompleteListener;
    private OnVolumeDBListener onVolumeDBListener;
    private TimeInfoBean timeInfoBean;
    private static int duration = -1;
    private static int volumePercent = 100;
    private static float speed = 1.0f;
    private static float pitch = 1.0f;
    private static SoundChannelEnum soundChannelEnum = SoundChannelEnum.CENTER;
    private AACEncode aacEncode;
    private VideoView videoView;
    private boolean viewSizeAdjust = false;

    public void setSource(String source) {
        mediaSrc = source;
    }

    public void setVideoView(VideoView videoView) {
        this.videoView = videoView;
    }

    public void prepare() {
        if (TextUtils.isEmpty(mediaSrc)) {
            LogUtil.d("source not be empty!");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                nativePrepare(mediaSrc);
            }
        }).start();
    }

    public void start() {
        if (TextUtils.isEmpty(mediaSrc)) {
            LogUtil.e("source is empty");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                setVolume(volumePercent);
                setSoundChannel(soundChannelEnum);

                nativeStart();
            }
        }).start();
    }

    public void pause() {
        nativePause();
        if (onPauseListener != null) {
            onPauseListener.onPause(true);
        }
    }

    public void resume() {
        nativeResume();
        if (onPauseListener != null) {
            onPauseListener.onPause(false);
        }
    }

    public void stop() {
        timeInfoBean = null;
        duration = -1;
        stopRecord();
        new Thread(new Runnable() {
            @Override
            public void run() {
                nativeStop();
            }
        }).start();
    }

    public void seek(int seconds) {
        nativeSeek(seconds);
    }

    public void playNext(String url) {
        mediaSrc = url;
        playNext = true;
        stop();
    }

    public int getDuration() {
        if (duration < 0) {
            duration = nativeGetDuration();
        }
        return duration;
    }

    public void setVolume(int percent) {
        if (percent >= 0 && percent <= 100) {
            volumePercent = percent;
            nativeSetVolume(percent);
        }
    }

    public void setSoundChannel(SoundChannelEnum soundChannel) {
        soundChannelEnum = soundChannel;
        nativeSetSoundChannel(soundChannel.ordinal());
    }

    public int getVolumePercent() {
        return volumePercent;
    }

    public void setPitch(float p) {
        pitch = p;
        nativePitch(p);
    }

    public void setSpeed(float s) {
        speed = s;
        nativeSpeed(s);
    }

    public void startRecord(String filePath) {
        int sampleRate = nativeGetSamplerate();
        if (aacEncode != null || sampleRate <= 0) {
            return;
        }
        aacEncode = new AACEncode();
        aacEncode.initMediacodec(sampleRate, new File(filePath));
        nativeControlRecord(true);
        LogUtil.d("开始录制");
    }

    public void stopRecord() {
        if (aacEncode == null) {
            return;
        }
        nativeControlRecord(false);
        aacEncode.releaseMediacodec();
        aacEncode = null;
        LogUtil.d("结束录制");
    }

    public void pauseRecord() {
        nativeControlRecord(false);
        LogUtil.d("暂停录制");
    }

    public void resumeRecord() {
        nativeControlRecord(true);
        LogUtil.d("继续录制");
    }

    public void setOnPreparedListener(OnPreparedListener listener) {
        this.onPreparedListener = listener;
    }

    public void setOnLoadListener(OnLoadListener listener) {
        this.onLoadListener = listener;
    }

    public void setOnPauseListener(OnPauseListener listener) {
        this.onPauseListener = listener;
    }

    public void setOnTimeInfoListener(OnTimeInfoListener listener) {
        this.onTimeInfoListener = listener;
    }

    public void setOnErrorListener(OnErrorListener listener) {
        this.onErrorListener = listener;
    }

    public void setOnCompleteListener(OnCompleteListener listener) {
        this.onCompleteListener = listener;
    }

    public void setOnVolumeDBListener(OnVolumeDBListener listener) {
        this.onVolumeDBListener = listener;
    }

    public void setOnRecordTimeListener(OnRecordTimeListener listener) {
        if (aacEncode == null) {
            return;
        }
        aacEncode.setOnRecordTimeListener(listener);
    }

    /**
     * C++回调Java方法
     */
    public void onCallPrepared() {
        if (onPreparedListener == null) {
            return;
        }
        onPreparedListener.onPrepared();
    }

    public void onCallLoad(boolean isLoading) {
        if (onLoadListener == null) {
            return;
        }
        onLoadListener.onLoad(isLoading);
    }

    public void onCallTimeInfo(int currentTime, int totalTime) {
        if (onTimeInfoListener == null) {
            return;
        }
        if (timeInfoBean == null) {
            timeInfoBean = new TimeInfoBean();
        }
        timeInfoBean.setCurrentTime(currentTime);
        timeInfoBean.setTotalTime(totalTime);
        onTimeInfoListener.onTimeInfo(timeInfoBean);
    }

    public void onCallError(int code, String msg) {
        stop();
        if (onErrorListener == null) {
            return;
        }
        onErrorListener.onError(code, msg);
    }

    public void onCallComplete() {
        stop();
        if (onCompleteListener == null) {
            return;
        }
        onCompleteListener.onComplete();
    }

    public void onCallNext() {
        if (playNext) {
            playNext = false;
            prepare();
        }
    }

    public void onCallVolumeDB(int value) {
        if (onVolumeDBListener == null) {
            return;
        }
        onVolumeDBListener.onDbValue(value);
    }

    public void onCallPCM2AAC(int size, byte[] buffer) {
        if (aacEncode == null) {
            return;
        }
        aacEncode.convertPcmToAac(size, buffer);
    }

    public void onCallRenderYUV(final int width, final int height, byte[] y, byte[] u, byte[] v) {
        LogUtil.d("获取到视频的yuv数据: " + width + "x" + height);
        if (videoView == null) {
            return;
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
        videoView.setYUVRenderData(width, height, y, u, v);
    }

    private native void nativePrepare(String source);

    private native void nativeStart();

    private native void nativePause();

    private native void nativeResume();

    private native void nativeStop();

    private native void nativeSeek(int seconds);

    private native int nativeGetDuration();

    private native void nativeSetVolume(int percent);

    private native void nativeSetSoundChannel(int channel);

    private native void nativePitch(float pitch);

    private native void nativeSpeed(float speed);

    private native int nativeGetSamplerate();

    private native void nativeControlRecord(boolean start);

}
