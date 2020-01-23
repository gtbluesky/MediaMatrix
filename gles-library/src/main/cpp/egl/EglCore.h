//
// Created by gtbluesky on 2019-07-16.
//

#ifndef NATIVEOPENGL_EGLCORE_H
#define NATIVEOPENGL_EGLCORE_H

#ifndef EGL_EGLEXT_PROTOTYPES
#define EGL_EGLEXT_PROTOTYPES
#endif

#include <android/native_window.h>
#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <EGL/eglplatform.h>

/**
 * Constructor flag: surface must be recordable.  This discourages EGL from using a
 * pixel format that cannot be converted efficiently to something usable by the video
 * encoder.
 */
#define FLAG_RECORDABLE 0x01

/**
 * Constructor flag: ask for GLES3, fall back to GLES2 if not available.  Without this
 * flag, GLES2 is used.
 */
#define FLAG_TRY_GLES3 002

// Android-specific extension
#define EGL_RECORDABLE_ANDROID 0x3142

class EglCore {

private:
    EGLDisplay mEGLDisplay = EGL_NO_DISPLAY;
    EGLConfig  mEGLConfig = nullptr;
    EGLContext mEGLContext = EGL_NO_CONTEXT;
    int mGlVersion = -1;

    EGLConfig getConfig(int flags, int version);
    void checkEglError(const char *msg);

public:
    EglCore();
    EglCore(EGLContext sharedContext, int flags);
    ~EglCore();
    bool init(EGLContext sharedContext, int flags);
    void release();
    EGLContext getEGLContext();
    void releaseSurface(EGLSurface eglSurface);
    EGLSurface createWindowSurface(ANativeWindow *surface);
    EGLSurface createOffscreenSurface(int width, int height);
    void makeCurrent(EGLSurface eglSurface);
    void makeCurrent(EGLSurface drawSurface, EGLSurface readSurface);
    void makeNothingCurrent();
    bool swapBuffers(EGLSurface eglSurface);
    void setPresentationTime(EGLSurface eglSurface, long nsecs);
    bool isCurrent(EGLSurface eglSurface);
    int querySurface(EGLSurface eglSurface, int what);
    const char *queryString(int what);
    int getGlVersion();
};


#endif //NATIVEOPENGL_EGLCORE_H
