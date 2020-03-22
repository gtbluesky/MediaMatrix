//
// Created by gtbluesky on 2019-07-16.
//

#include "WindowSurface.h"

WindowSurface::WindowSurface(
        EglCore *eglCore,
        ANativeWindow *window,
        bool releaseSurface) : EglSurfaceBase(eglCore) {
    createWindowSurface(window);
    mSurface = window;
    mReleaseSurface = releaseSurface;
}

WindowSurface::WindowSurface(
        EglCore *eglCore,
        ANativeWindow *window) : EglSurfaceBase(eglCore) {
    createWindowSurface(window);
    mSurface = window;
}

void WindowSurface::release() {
    releaseEglSurface();
    if (mSurface != nullptr) {
        if (mReleaseSurface) {
            ANativeWindow_release(mSurface);
        }
        mSurface = nullptr;
    }
}

void WindowSurface::recreate(EglCore *eglCore) {
    if (mSurface == nullptr) {
        LOGE("not yet implemented ANativeWindow");
        return;
    }
    mEglCore = eglCore;
    createWindowSurface(mSurface);
}