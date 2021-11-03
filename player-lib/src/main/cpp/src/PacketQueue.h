//
// Created by gtbluesky on 18-9-18.
//

#ifndef MUSICPLAYER_PACKETQUEUE_H
#define MUSICPLAYER_PACKETQUEUE_H

#include <queue>
#include <pthread.h>
#include "LogUtil.h"
#include "PlayStatus.h"

#ifdef __cplusplus
extern "C" {
#endif
#include "libavcodec/avcodec.h"


class PacketQueue {

public:
    std::queue<AVPacket *> queuePacket;
    pthread_mutex_t mutexPacket;
    pthread_cond_t condPacket;
    PlayStatus *playStatus = NULL;

public:
    PacketQueue(PlayStatus *playStatus);

    ~PacketQueue();

    int putAVPacket(AVPacket *packet);

    int getAVPacket(AVPacket *packet);

    int getQueueSize();

    void clearAvpacket();
};

#ifdef __cplusplus
}
#endif

#endif //MUSICPLAYER_PACKETQUEUE_H
