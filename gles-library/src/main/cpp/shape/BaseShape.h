//
// Created by gtbluesky on 2019-07-18.
//

#ifndef NATIVEOPENGL_BASESHAPE_H
#define NATIVEOPENGL_BASESHAPE_H


class BaseShape {
public:
    BaseShape() = default;
    virtual ~BaseShape() = default;
    virtual int init() = 0;
    virtual void onDraw(int width, int height) = 0;
    virtual void destroy() = 0;
};


#endif //NATIVEOPENGL_BASESHAPE_H
