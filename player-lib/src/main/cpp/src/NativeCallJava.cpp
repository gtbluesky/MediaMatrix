//
// Created by gtbluesky on 18-9-15.
//

#include "NativeCallJava.h"

NativeCallJava::NativeCallJava(JavaVM *javaVM, JNIEnv *env, jobject obj) {

    this->javaVm = javaVM;
    this->jniEnv = env;
    this->jobj = env->NewGlobalRef(obj);

    jclass jclz = jniEnv->GetObjectClass(jobj);
    if (!jclz) {
        LogUtil::e("get jclass error");
        return;
    }

    jmid_prepare = env->GetMethodID(jclz, "onCallPrepared", "()V");
    jmid_load = env->GetMethodID(jclz, "onCallLoad", "(Z)V");
    jmid_timeinfo = env->GetMethodID(jclz, "onCallTimeInfo", "(II)V");
    jmid_error = env->GetMethodID(jclz, "onCallError", "(ILjava/lang/String;)V");
    jmid_complete = env->GetMethodID(jclz, "onCallComplete", "()V");
    jmid_volumedb = env->GetMethodID(jclz, "onCallVolumeDB", "(I)V");
    jmid_pcm2aac = env->GetMethodID(jclz, "onCallPCM2AAC", "(I[B)V");
    jmid_renderyuv = env->GetMethodID(jclz, "onCallRenderYUV", "(II[B[B[B)V");
}

NativeCallJava::~NativeCallJava() {

}

void NativeCallJava::onCallPrepared(int thread_type) {
    if (thread_type == JAVA_THREAD) {
        jniEnv->CallVoidMethod(jobj, jmid_prepare);
    } else if (thread_type == NATIVE_THREAD) {
        JNIEnv *jniEnv;
        if (javaVm->AttachCurrentThread(&jniEnv, 0) != JNI_OK) {
            LogUtil::e("get child thread jnienv error");
            return;
        }
        jniEnv->CallVoidMethod(jobj, jmid_prepare);
        javaVm->DetachCurrentThread();
    }
}

void NativeCallJava::onCallLoad(int thread_type, bool is_loading) {
    if (thread_type == JAVA_THREAD) {
        jniEnv->CallVoidMethod(jobj, jmid_load, is_loading);
    } else if (thread_type == NATIVE_THREAD) {
        JNIEnv *jniEnv;
        if (javaVm->AttachCurrentThread(&jniEnv, 0) != JNI_OK) {
            LogUtil::e("get child thread jnienv error");
            return;
        }
        jniEnv->CallVoidMethod(jobj, jmid_load, is_loading);
        javaVm->DetachCurrentThread();
    }
}

void NativeCallJava::onCallTimeInfo(int thread_type, int cur, int total) {
    if (thread_type == JAVA_THREAD) {
        jniEnv->CallVoidMethod(jobj, jmid_timeinfo, cur, total);
    } else if (thread_type == NATIVE_THREAD) {
        JNIEnv *jniEnv;
        if (javaVm->AttachCurrentThread(&jniEnv, 0) != JNI_OK) {
            LogUtil::e("get child thread jnienv error");
            return;
        }
        jniEnv->CallVoidMethod(jobj, jmid_timeinfo, cur, total);
        javaVm->DetachCurrentThread();
    }
}

void NativeCallJava::onCallError(int thread_type, int code, char *msg) {
    if (thread_type == JAVA_THREAD) {
        jstring jmsg = jniEnv->NewStringUTF(msg);
        jniEnv->CallVoidMethod(jobj, jmid_error, code, jmsg);
        jniEnv->DeleteLocalRef(jmsg);
    } else if (thread_type == NATIVE_THREAD) {
        JNIEnv *jniEnv;
        if (javaVm->AttachCurrentThread(&jniEnv, 0) != JNI_OK) {
            LogUtil::e("get child thread jnienv error");
            return;
        }
        jstring jmsg = jniEnv->NewStringUTF(msg);
        jniEnv->CallVoidMethod(jobj, jmid_error, code, jmsg);
        jniEnv->DeleteLocalRef(jmsg);
        javaVm->DetachCurrentThread();
    }
}

void NativeCallJava::onCallComplete(int thread_type) {
    if (thread_type == JAVA_THREAD) {
        jniEnv->CallVoidMethod(jobj, jmid_complete);
    } else if (thread_type == NATIVE_THREAD) {
        JNIEnv *jniEnv;
        if (javaVm->AttachCurrentThread(&jniEnv, 0) != JNI_OK) {
            LogUtil::e("get child thread jnienv error");
            return;
        }
        jniEnv->CallVoidMethod(jobj, jmid_complete);
        javaVm->DetachCurrentThread();
    }
}

void NativeCallJava::onCallVolumeDB(int thread_type, int db) {
    if (thread_type == JAVA_THREAD) {
        jniEnv->CallVoidMethod(jobj, jmid_volumedb, db);
    } else if (thread_type == NATIVE_THREAD) {
        JNIEnv *jniEnv;
        if (javaVm->AttachCurrentThread(&jniEnv, 0) != JNI_OK) {
            LogUtil::e("get child thread jnienv error");
            return;
        }
        jniEnv->CallVoidMethod(jobj, jmid_volumedb, db);
        javaVm->DetachCurrentThread();
    }
}

void NativeCallJava::onCallPCM2AAC(int thread_type, int size, void *buffer) {
    if (thread_type == JAVA_THREAD) {
        jbyteArray jbuffer = jniEnv->NewByteArray(size);
        jniEnv->SetByteArrayRegion(jbuffer, 0, size, (const jbyte *) buffer);
        jniEnv->CallVoidMethod(jobj, jmid_pcm2aac, size, jbuffer);
        jniEnv->DeleteLocalRef(jbuffer);
    } else if (thread_type == NATIVE_THREAD) {
        JNIEnv *jniEnv;
        if (javaVm->AttachCurrentThread(&jniEnv, 0) != JNI_OK) {
            LogUtil::e("get child thread jnienv error");
            return;
        }
        jbyteArray jbuffer = jniEnv->NewByteArray(size);
        jniEnv->SetByteArrayRegion(jbuffer, 0, size, (const jbyte *) buffer);
        jniEnv->CallVoidMethod(jobj, jmid_pcm2aac, size, jbuffer);
        jniEnv->DeleteLocalRef(jbuffer);
        javaVm->DetachCurrentThread();
    }
}

void NativeCallJava::onCallRenderYUV(int width, int height, uint8_t *y, uint8_t *u, uint8_t *v) {
    JNIEnv *jniEnv;
    if (javaVm->AttachCurrentThread(&jniEnv, 0) != JNI_OK) {
        LogUtil::e("get child thread jnienv error");
        return;
    }
    jbyteArray jy = jniEnv->NewByteArray(width * height);
    jniEnv->SetByteArrayRegion(jy, 0, width * height, (const jbyte *) y);

    jbyteArray ju = jniEnv->NewByteArray(width * height / 4);
    jniEnv->SetByteArrayRegion(ju, 0, width * height / 4, (const jbyte *) u);

    jbyteArray jv = jniEnv->NewByteArray(width * height / 4);
    jniEnv->SetByteArrayRegion(jv, 0, width * height / 4, (const jbyte *) v);

    jniEnv->CallVoidMethod(jobj, jmid_renderyuv, width, height, jy, ju, jv);
    jniEnv->DeleteLocalRef(jy);
    jniEnv->DeleteLocalRef(ju);
    jniEnv->DeleteLocalRef(jv);

    javaVm->DetachCurrentThread();
}
