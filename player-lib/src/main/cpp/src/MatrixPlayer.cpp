//
// Created by gtbluesky on 18-9-15.
//

#include <jni.h>
#include <string>
#include "FFmpegController.h"
#include "PlayStatus.h"

#ifdef __cplusplus
extern "C" {
#endif
#include <libavformat/avformat.h>

JavaVM *javaVM = NULL;
NativeCallJava *callJava = NULL;
FFmpegController *fFmpegController = NULL;
PlayStatus *status = NULL;
pthread_t thread_start;
bool next_exit = true;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    jint result = -1;
    javaVM = vm;
    JNIEnv *env;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {

        return result;
    }
    return JNI_VERSION_1_6;

}

JNIEXPORT void JNICALL
Java_com_gtbluesky_mediamatrix_player_MatrixPlayer_nativePrepare(JNIEnv *env, jobject instance,
                                                       jstring source_) {
    jboolean isCopy;
    const char *source = env->GetStringUTFChars(source_, &isCopy);

    if (!fFmpegController) {
        if (!callJava) {
            callJava = new NativeCallJava(javaVM, env, instance);
        }
        callJava->onCallLoad(JAVA_THREAD, true);
        status = new PlayStatus();
        fFmpegController = new FFmpegController(status, callJava, source);
        fFmpegController->prepared();
    }

//    env->ReleaseStringUTFChars(source_, source);
}

void *startCallBack(void *data) {
    FFmpegController *fFmpegController = (FFmpegController *) data;
    fFmpegController->start();
    pthread_exit(&thread_start);
}

JNIEXPORT void JNICALL
Java_com_gtbluesky_mediamatrix_player_MatrixPlayer_nativeStart(JNIEnv *env, jobject instance) {
    if (!fFmpegController) {
        return;
    }
    pthread_create(&thread_start, NULL, startCallBack, fFmpegController);

}

JNIEXPORT void JNICALL
Java_com_gtbluesky_mediamatrix_player_MatrixPlayer_nativePause(JNIEnv *env, jobject instance) {
    if (!fFmpegController) {
        return;
    }
    fFmpegController->pause();
}

JNIEXPORT void JNICALL
Java_com_gtbluesky_mediamatrix_player_MatrixPlayer_nativeResume(JNIEnv *env, jobject instance) {
    if (!fFmpegController) {
        return;
    }
    fFmpegController->resume();
}

JNIEXPORT void JNICALL
Java_com_gtbluesky_mediamatrix_player_MatrixPlayer_nativeStop(JNIEnv *env, jobject instance) {
    if (!next_exit) {
        return;
    }
    jclass jclz = env->GetObjectClass(instance);
    jmethodID jmid_next = env->GetMethodID(jclz, "onCallNext", "()V");

    next_exit = false;

    if (fFmpegController) {
        delete fFmpegController;
        fFmpegController = NULL;
        if (callJava) {
            delete callJava;
            callJava = NULL;
        }
        if (status) {
            delete status;
            status = NULL;
        }
    }
    next_exit = true;
    env->CallVoidMethod(instance, jmid_next);

}

JNIEXPORT void JNICALL
Java_com_gtbluesky_mediamatrix_player_MatrixPlayer_nativeSeek(JNIEnv *env, jobject instance, jint seconds) {
    if (fFmpegController) {
        fFmpegController->seek(seconds);
    }
}

JNIEXPORT jint JNICALL
Java_com_gtbluesky_mediamatrix_player_MatrixPlayer_nativeGetDuration(JNIEnv *env, jobject instance) {
    if (fFmpegController) {
        return fFmpegController->duration;
    }
    return 0;
}

JNIEXPORT void JNICALL
Java_com_gtbluesky_mediamatrix_player_MatrixPlayer_nativeSetVolume(JNIEnv *env, jobject instance,
                                                         jint percent) {
    if (fFmpegController) {
        fFmpegController->setVolume(percent);
    }
}

JNIEXPORT void JNICALL
Java_com_gtbluesky_mediamatrix_player_MatrixPlayer_nativeSetSoundChannel(JNIEnv *env, jobject instance,
                                                               jint channel) {
    if (fFmpegController) {
        fFmpegController->setSoundChannel(channel);
    }
}

JNIEXPORT void JNICALL
Java_com_gtbluesky_mediamatrix_player_MatrixPlayer_nativePitch(JNIEnv *env, jobject instance, jfloat pitch) {
    if (fFmpegController) {
        fFmpegController->setPitch(pitch);
    }
}

JNIEXPORT void JNICALL
Java_com_gtbluesky_mediamatrix_player_MatrixPlayer_nativeSpeed(JNIEnv *env, jobject instance, jfloat speed) {
    if (fFmpegController) {
        fFmpegController->setSpeed(speed);
    }
}

JNIEXPORT jint JNICALL
Java_com_gtbluesky_mediamatrix_player_MatrixPlayer_nativeGetSampleRate(JNIEnv *env, jobject instance) {
    if (fFmpegController) {
        return fFmpegController->getSampleRate();
    }
    return 0;
}

JNIEXPORT void JNICALL
Java_com_gtbluesky_mediamatrix_player_MatrixPlayer_nativeControlRecord(JNIEnv *env, jobject instance,
                                                             jboolean start) {
    if (fFmpegController) {
        fFmpegController->controlRecord(start);
    }
}

#ifdef __cplusplus
}
#endif