//
// Created by gtbluesky on 2019-07-18.
//

#ifndef NATIVEOPENGL_GLRENDERER_H
#define NATIVEOPENGL_GLRENDERER_H


#include <android/native_window.h>
#include "../egl/WindowSurface.h"
#include "../shape/BaseShape.h"

class GLRenderer {
public:
    GLRenderer() = default;
    virtual ~GLRenderer();
    void surfaceCreated(ANativeWindow *window);
    void surfaceChanged(int width, int height);
    void surfaceDestroyed(void);

private:
    EglCore *mEglCore = nullptr;
    WindowSurface *mWindowSurface = nullptr;
    BaseShape *shape = nullptr;
};


#endif //NATIVEOPENGL_GLRENDERER_H
