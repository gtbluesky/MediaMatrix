//
// Created by gtbluesky on 2019-07-18.
//

#include "GLRenderer.h"
#include "../shape/Triangle.h"

GLRenderer::~GLRenderer() {
    if (mEglCore) {
        mEglCore->release();
        delete mEglCore;
        mEglCore = nullptr;
    }
}

void GLRenderer::surfaceCreated(ANativeWindow *window) {
    if (!mEglCore) {
        mEglCore = new EglCore(nullptr, FLAG_RECORDABLE);
    }
    mWindowSurface = new WindowSurface(mEglCore, window, false);
    mWindowSurface->makeCurrent();
    shape = new Triangle();
    shape->init();
}

void GLRenderer::surfaceChanged(int width, int height) {
    mWindowSurface->makeCurrent();
    shape->onDraw(width, height);
    mWindowSurface->swapBuffers();
}

void GLRenderer::surfaceDestroyed() {
    if (shape) {
        shape->destroy();
        delete shape;
        shape = nullptr;
    }
    if (mWindowSurface) {
        mWindowSurface->release();
        delete mWindowSurface;
        mWindowSurface = nullptr;
    }
    if (mEglCore) {
        mEglCore->release();
        delete mEglCore;
        mEglCore = nullptr;
    }
}