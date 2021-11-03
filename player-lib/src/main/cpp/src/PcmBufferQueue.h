//
// Created by gtbluesky on 18-10-5.
//

#ifndef MUSICPLAYER_PCMBUFFERQUEUE_H
#define MUSICPLAYER_PCMBUFFERQUEUE_H

#include <deque>
#include "PlayStatus.h"
#include "PcmBean.h"
#include <pthread.h>
#include "LogUtil.h"

extern "C" {
#include <libavcodec/avcodec.h>
};

using namespace std;

class PcmBufferQueue {
public:
    deque<PcmBean *> queue_buffer;
    pthread_mutex_t mutex_buffer;
    pthread_cond_t cond_buffer;
    PlayStatus *status = NULL;

public:
    PcmBufferQueue(PlayStatus *status);

    ~PcmBufferQueue();

    int putBuffer(SAMPLETYPE *buffer, int size);

    int getBuffer(PcmBean **pcmBean);

    int clearBuffer();

    void release();

    int getBufferSize();

    int noticeThread();
};


#endif //MUSICPLAYER_PCMBUFFERQUEUE_H
