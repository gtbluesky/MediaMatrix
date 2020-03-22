//
// Created by gtbluesky on 2019-07-17.
//

#include "Looper.h"
#include "android_log.h"

#include <jni.h>
#include <pthread.h>
#include <cstdio>
#include <cstring>
#include <unistd.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <semaphore.h>

//typedef struct LooperMessage LooperMessage;


/**
 * 线程执行句柄
 * @param p
 * @return
 */
void* Looper::trampoline(void* p) {
    ((Looper*)p)->loop();
    return nullptr;
}


Looper::Looper() {
    sem_init(&headdataavailable, 0, 0);
    sem_init(&headwriteprotect, 0, 1);
    pthread_attr_t attr;
    pthread_attr_init(&attr);

    pthread_create(&worker, &attr, trampoline, this);
    running = true;
}

Looper::~Looper() {
    if (running) {
        LOGD("Looper deleted while still running. Some messages will not be processed");
        quit();
    }
}

/**
 * 入队消息
 * @param what
 * @param flush
 */
void Looper::postMessage(int what, bool flush) {
    postMessage(what, 0, 0, nullptr, flush);
}

/**
 * 入队消息
 * @param what
 * @param obj
 * @param flush
 */
void Looper::postMessage(int what, void *obj, bool flush) {
    postMessage(what, 0, 0, obj, flush);
}

/**
 * 入队消息
 * @param what
 * @param arg1
 * @param arg2
 * @param flush
 */
void Looper::postMessage(int what, int arg1, int arg2, bool flush) {
    postMessage(what, arg1, arg2, nullptr, flush);
}

/**
 *
 * @param what
 * @param arg1
 * @param arg2
 * @param obj
 * @param flush
 */
void Looper::postMessage(int what, int arg1, int arg2, void *obj, bool flush) {
    auto msg = new LooperMessage();
    msg->what = what;
    msg->obj = obj;
    msg->arg1 = arg1;
    msg->arg2 = arg2;
    msg->next = nullptr;
    msg->quit = false;
    addMessage(msg, flush);
}

/**
 * 添加消息
 * @param msg
 * @param flush
 */
void Looper::addMessage(LooperMessage *msg, bool flush) {
    sem_wait(&headwriteprotect);
    LooperMessage *h = head;

    if (flush) {
        while(h) {
            LooperMessage *next = h->next;
            delete h;
            h = next;
        }
        h = nullptr;
    }
    if (h) {
        while (h->next) {
            h = h->next;
        }
        h->next = msg;
    } else {
        head = msg;
    }
    LOGD("post msg %d", msg->what);
    sem_post(&headwriteprotect);
    sem_post(&headdataavailable);
}

/**
 * 循环体
 */
void Looper::loop() {
    while(true) {
        // wait for available message
        sem_wait(&headdataavailable);

        // get next available message
        sem_wait(&headwriteprotect);
        LooperMessage *msg = head;
        if (msg == nullptr) {
            LOGD("no msg");
            sem_post(&headwriteprotect);
            continue;
        }
        head = msg->next;
        sem_post(&headwriteprotect);

        if (msg->quit) {
            LOGD("quitting");
            delete msg;
            return;
        }
        LOGD("processing msg %d", msg->what);
        handleMessage(msg);
        delete msg;
    }
}

/**
 * 退出Looper循环
 */
void Looper::quit() {
    LOGD("quit");
    auto msg = new LooperMessage();
    msg->what = 0;
    msg->obj = nullptr;
    msg->next = nullptr;
    msg->quit = true;
    addMessage(msg, false);
    void *retval;
    pthread_join(worker, &retval);
    sem_destroy(&headdataavailable);
    sem_destroy(&headwriteprotect);
    running = false;
}

/**
 * 处理消息
 * @param what
 * @param data
 */
void Looper::handleMessage(LooperMessage *msg) {
    LOGD("dropping msg %d %p", msg->what, msg->obj);
}