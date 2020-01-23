//
// Created by gtbluesky on 2019-07-18.
//

#include <android/native_window.h>
#include "GLLooper.h"

GLLooper::GLLooper() {
    renderer = new GLRenderer();
}

GLLooper::~GLLooper() {
    delete renderer;
}

void GLLooper::handleMessage(LooperMessage *msg) {
    switch (msg && msg->what) {
        case kMsgSurfaceCreated:
            if (renderer) {
                renderer->surfaceCreated((ANativeWindow *) msg->obj);
            }
            break;

        case kMsgSurfaceChanged:
            if (renderer) {
                renderer->surfaceChanged(msg->arg1, msg->arg2);
            }
            break;

        case kMsgSurfaceDestroyed:
            if (renderer) {
                renderer->surfaceDestroyed();
            }
            break;
        default:
            break;
    }
}