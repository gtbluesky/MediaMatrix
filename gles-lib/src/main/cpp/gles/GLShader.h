//
// Created by gtbluesky on 2019-07-18.
//

#ifndef NATIVEOPENGL_GLSHADER_H
#define NATIVEOPENGL_GLSHADER_H


// 转成字符串
#define SHADER_STRING(s) #s

typedef enum {
    VERTEX_DEFAULT,
    VERTEX_REVERSE,
    FRAGMENT_SOLID,
    FRAGMENT_ABGR,
    FRAGMENT_ARGB,
    FRAGMENT_BGR,
    FRAGMENT_RGB,
    FRAGMENT_I420,
    FRAGMENT_NV12,
    FRAGMENT_NV21
} ShaderType;

// 获取shader
const char *getShader(ShaderType type);


#endif //NATIVEOPENGL_GLSHADER_H
