//
// Created by gtbluesky on 2019-07-18.
//

#ifndef NATIVEOPENGL_GLLOOPER_H
#define NATIVEOPENGL_GLLOOPER_H

#include "../common/Looper.h"
#include "GLRenderer.h"

enum {
    kMsgSurfaceCreated,
    kMsgSurfaceChanged,
    kMsgSurfaceDestroyed
};

class GLLooper : public Looper {
public:
    GLLooper();
    virtual ~GLLooper();
    virtual void handleMessage(LooperMessage *msg);

private:
    GLRenderer *renderer = nullptr;

};


#endif //NATIVEOPENGL_GLLOOPER_H
