//
// Created by gtbluesky on 18-9-15.
//

#ifndef MUSICPLAYER_FFMPEG_H
#define MUSICPLAYER_FFMPEG_H

#include "NativeCallJava.h"
#include <pthread.h>
#include "AudioInfo.h"
#include "VideoInfo.h"
#include "PlayStatus.h"

#ifdef __cplusplus
extern "C" {
#endif
#include "libavformat/avformat.h"
#include "libavutil/time.h"

class FFmpegController {

public:
    PlayStatus *status = NULL;
    NativeCallJava *callJava = NULL;
    const char *url = NULL;
    pthread_t decodeThread;
    AVFormatContext *pFormatCtx = NULL;
    AudioInfo *audioInfo = NULL;
    VideoInfo *videoInfo = NULL;
    pthread_mutex_t init_mutex;
    bool decode_exit = false;
    int duration = 0;
    pthread_mutex_t seek_mutex;

public:
    FFmpegController(PlayStatus *playStatus, NativeCallJava *callJava, const char *url);

    ~FFmpegController();

    void prepared();

    void decodeFFmpegThread();

    void start();

    void pause();

    void resume();

    void release();

    void seek(int64_t milliseconds);

    void setVolume(int percent);

    void setSoundChannel(int sound_channle);

    void setPitch(float pitch);

    void setSpeed(float speed);

    int getSampleRate();

    void controlRecord(bool start);

    int getCodecContext(AVCodecParameters *params, AVCodecContext **ctx);

};

#ifdef __cplusplus
}
#endif

#endif //MUSICPLAYER_FFMPEG_H
