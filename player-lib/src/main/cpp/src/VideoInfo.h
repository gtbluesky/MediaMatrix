//
// Created by gtbluesky on 18-10-6.
//

#ifndef MUSICPLAYER_VIDEOINFO_H
#define MUSICPLAYER_VIDEOINFO_H

#include "PacketQueue.h"
#include "NativeCallJava.h"
#include "AudioInfo.h"

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavutil/time.h>
#include <libswscale/swscale.h>
#include <libavutil/imgutils.h>
};

class VideoInfo {

public:
    int stream_index = -1;
    AVCodecContext *avCodecContext = NULL;
    AVCodecParameters *codecpar = NULL;
    PacketQueue *queue = NULL;
    PlayStatus *status = NULL;
    NativeCallJava *callJava = NULL;
    AVRational time_base;
    pthread_t thread_play;
    AudioInfo *audioInfo = NULL;
    double clock = 0;
    double delayTime = 0;
    double defaultDelayTime = 0;

public:
    VideoInfo(PlayStatus *status, NativeCallJava *nativeCallJava);

    ~VideoInfo();

    void play();

    void release();

    double getFrameDiffTime(AVFrame *avFrame);

    double getDelayTime(double diff);};


#endif //MUSICPLAYER_VIDEOINFO_H
