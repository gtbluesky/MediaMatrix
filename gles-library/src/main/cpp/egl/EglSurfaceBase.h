//
// Created by gtbluesky on 2019-07-16.
//

#ifndef NATIVEOPENGL_EGLSURFACEBASE_H
#define NATIVEOPENGL_EGLSURFACEBASE_H


#include "EglCore.h"
#include "../common/android_log.h"

class EglSurfaceBase {

private:
    EGLSurface mEglSurface = EGL_NO_SURFACE;
    int mWidth = -1;
    int mHeight = -1;

public:
    EglSurfaceBase(EglCore *eglCore);
    void createWindowSurface(ANativeWindow *nativeWindow);
    void createOffscreenSurface(int width, int height);
    int getWidth();
    int getHeight();
    void releaseEglSurface();
    void makeCurrent();
    void makeCurrentReadFrom(EglSurfaceBase *readSurface);
    bool swapBuffers();
    void setPresentationTime(long nsecs);
    char *getCurrentFrame();

protected:
    EglCore *mEglCore;

};


#endif //NATIVEOPENGL_EGLSURFACEBASE_H
