//
// Created by gtbluesky on 2019-07-17.
//

#include <jni.h>
#include "render/GLLooper.h"
#include <android/native_window_jni.h>

static GLLooper *glLooper = nullptr;
static ANativeWindow *window = nullptr;

extern "C" {
JNIEXPORT void JNICALL
Java_com_github_gtbluesky_nativeopengl_NativeGL_nativeInit(
        JNIEnv *env,
        jclass jclz) {
    glLooper = new GLLooper();
}

JNIEXPORT void JNICALL
Java_com_github_gtbluesky_nativeopengl_NativeGL_nativeRelease(
        JNIEnv *env,
        jclass jclz) {
    if (glLooper) {
        glLooper->quit();
        delete glLooper;
        glLooper = nullptr;
    }
    if (window) {
        ANativeWindow_release(window);
        window = nullptr;
    }
}

JNIEXPORT void JNICALL
Java_com_github_gtbluesky_nativeopengl_NativeGL_nativeSurfaceCreated(
        JNIEnv *env,
        jclass jclz,
        jobject surface) {
    if (window) {
        ANativeWindow_release(window);
        window = nullptr;
    }
    window = ANativeWindow_fromSurface(env, surface);
    if (glLooper) {
        glLooper->postMessage(kMsgSurfaceCreated, window);
    }
}

JNIEXPORT void JNICALL
Java_com_github_gtbluesky_nativeopengl_NativeGL_nativeSurfaceChanged(
        JNIEnv *env,
        jclass jclz,
        jint width,
        jint height) {
    if (glLooper) {
        glLooper->postMessage(kMsgSurfaceChanged, width, height);
    }
}

JNIEXPORT void JNICALL
Java_com_github_gtbluesky_nativeopengl_NativeGL_nativeSurfaceDestroyed(
        JNIEnv *env,
        jclass jclz) {
    if (glLooper) {
        glLooper->postMessage(kMsgSurfaceDestroyed);
    }
}

JNIEXPORT void JNICALL
Java_com_github_gtbluesky_nativeopengl_NativeGL_nativeRequestRender(
        JNIEnv *env,
        jclass jclz) {
//mRenderer->requestRenderFrame();
}
}