//
// Created by gtbluesky on 18-9-15.
//

#ifndef MUSICPLAYER_NATIVECALLJAVA_H
#define MUSICPLAYER_NATIVECALLJAVA_H

#include <jni.h>
#include "LogUtil.h"
#include <stdint.h>

#define JAVA_THREAD 0
#define NATIVE_THREAD 1

class NativeCallJava {

public:
    JavaVM *javaVm = NULL;
    JNIEnv *jniEnv = NULL;
    jobject jobj;
    jmethodID jmid_prepare;
    jmethodID jmid_load;
    jmethodID jmid_timeinfo;
    jmethodID jmid_error;
    jmethodID jmid_complete;
    jmethodID jmid_volumedb;
    jmethodID jmid_pcm2aac;
    jmethodID jmid_renderyuv;

public:
    NativeCallJava(JavaVM *javaVM, JNIEnv *env, jobject obj);

    ~NativeCallJava();

    void onCallPrepared(int thread_type);

    void onCallLoad(int thread_type, bool is_loading);

    void onCallTimeInfo(int thread_type, int cur, int total);

    void onCallError(int thread_type, int code, char *msg);

    void onCallComplete(int thread_type);

    void onCallVolumeDB(int thread_type, int db);

    void onCallPCM2AAC(int thread_type, int size, void *buffer);

    void onCallRenderYUV(int width, int height, uint8_t *y, uint8_t *u, uint8_t *v);
};


#endif //MUSICPLAYER_NATIVECALLJAVA_H
