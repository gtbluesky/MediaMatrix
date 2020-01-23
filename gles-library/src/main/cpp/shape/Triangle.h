//
// Created by gtbluesky on 2019-07-18.
//

#ifndef NATIVEOPENGL_TRIANGLE_H
#define NATIVEOPENGL_TRIANGLE_H

#include "../gles/GLHelper.h"
#include "../gles/GLShader.h"
#include "../common/android_log.h"
#include "BaseShape.h"

class Triangle : public BaseShape {
public:
    Triangle() = default;
    virtual ~Triangle() = default;
    virtual int init();
    virtual void onDraw(int width, int height);
    virtual void destroy();

private:
    int programHandle;
};


#endif //NATIVEOPENGL_TRIANGLE_H
