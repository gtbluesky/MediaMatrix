//
// Created by gtbluesky on 2019-07-16.
//

#ifndef NATIVEOPENGL_WINDOWSURFACE_H
#define NATIVEOPENGL_WINDOWSURFACE_H

#include "EglSurfaceBase.h"

class WindowSurface : public EglSurfaceBase {

public:
    WindowSurface(EglCore *eglCore, ANativeWindow *window, bool releaseSurface);
    WindowSurface(EglCore *eglCore, ANativeWindow *window);
    void release();
    void recreate(EglCore *eglCore);

private:
    ANativeWindow *mSurface;
    bool mReleaseSurface;
};


#endif //NATIVEOPENGL_WINDOWSURFACE_H
