//
// Created by gtbluesky on 2019-07-16.
//

#ifndef NATIVEOPENGL_OFFSCREENSURFACE_H
#define NATIVEOPENGL_OFFSCREENSURFACE_H


#include "EglSurfaceBase.h"

class OffscreenSurface : public EglSurfaceBase {
public:
    OffscreenSurface(EglCore *eglCore, int width, int height);
    void release();
};


#endif //NATIVEOPENGL_OFFSCREENSURFACE_H
