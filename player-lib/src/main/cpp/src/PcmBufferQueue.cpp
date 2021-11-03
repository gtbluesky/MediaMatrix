//
// Created by gtbluesky on 18-10-5.
//

#include "PcmBufferQueue.h"

PcmBufferQueue::PcmBufferQueue(PlayStatus *status) {
    this->status = status;
    pthread_mutex_init(&mutex_buffer, NULL);
    pthread_cond_init(&cond_buffer, NULL);
}

PcmBufferQueue::~PcmBufferQueue() {
    release();
    status = NULL;
    pthread_mutex_destroy(&mutex_buffer);
    pthread_cond_destroy(&cond_buffer);
    LogUtil::e("PcmBufferQueue 释放完成");
}

int PcmBufferQueue::putBuffer(SAMPLETYPE *buffer, int size) {
    pthread_mutex_lock(&mutex_buffer);
    PcmBean *pcmBean = new PcmBean(buffer, size);
    queue_buffer.push_back(pcmBean);
    noticeThread();
    pthread_mutex_unlock(&mutex_buffer);
    return 0;
}

int PcmBufferQueue::getBuffer(PcmBean **pcmBean) {
    pthread_mutex_lock(&mutex_buffer);
    while (status && !status->is_exited) {
        if (queue_buffer.size() > 0) {
            *pcmBean = queue_buffer.front();
            queue_buffer.pop_front();
            break;
        } else {
            if (!status->is_exited) {
                pthread_cond_wait(&cond_buffer, &mutex_buffer);
            }
        }
    }
    pthread_mutex_unlock(&mutex_buffer);
    return 0;
}

int PcmBufferQueue::clearBuffer() {
    noticeThread();
    pthread_mutex_lock(&mutex_buffer);
    while (!queue_buffer.empty()) {
        PcmBean *pcmBean = queue_buffer.front();
        queue_buffer.pop_front();
        delete pcmBean;
    }
    pthread_mutex_unlock(&mutex_buffer);
    return 0;
}

void PcmBufferQueue::release() {
    noticeThread();
    clearBuffer();
}

int PcmBufferQueue::getBufferSize() {
    int size = 0;
    pthread_mutex_lock(&mutex_buffer);
    size = queue_buffer.size();
    pthread_mutex_unlock(&mutex_buffer);
    return size;
}

int PcmBufferQueue::noticeThread() {
    pthread_cond_signal(&cond_buffer);
    return 0;
}
