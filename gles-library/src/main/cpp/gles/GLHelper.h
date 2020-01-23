//
// Created by gtbluesky on 2019-07-16.
//

#ifndef NATIVEOPENGL_GLHELPER_H
#define NATIVEOPENGL_GLHELPER_H


#include <stdio.h>
#include <stdlib.h>
#include <GLES3/gl3.h>
#include <GLES3/gl3ext.h>
#include <GLES3/gl3platform.h>

#include "../common/android_log.h"

#define PI 3.1415926535897932384626433832795f

typedef struct {
    GLfloat m[16];
} ESMatrix;


GLuint createProgram(const char *vertexSource, const char *fragmentSource);
GLuint loadShader(GLenum type, const char* shaderSrc);
void checkActiveUniform(GLuint program);
GLuint createTexture(GLenum type);
GLuint createTextureWithBytes(unsigned char* bytes, int width, int height);
GLuint createTextureWithOldTexture(GLuint texture, unsigned char* bytes, int width, int height);
void createFrameBuffer(GLuint *framebuffer, GLuint* texture, int width, int height);
void createFrameBuffers(GLuint* frambuffers, GLuint* textures, int width, int height, int size);
void checkGLError(const char * op);

void scaleM(ESMatrix *result, int offset, GLfloat sx, GLfloat sy, GLfloat sz);
void translateM(ESMatrix *result, int offset, GLfloat x, GLfloat y, GLfloat z);
void rotateM(ESMatrix *result, GLfloat angle, GLfloat x, GLfloat y, GLfloat z);
int orthoM(ESMatrix *result, int mOffset, float left, float right,
           float bottom, float top, float near, float far);
int frustumM(ESMatrix *result, int offset, float left, float right,
             float bottom, float top, float near, float far);
int perspectiveM(ESMatrix *result, int offset,
                 float fovy, float aspect, float zNear, float zFar);
void setIdentityM(ESMatrix *result);
void multiplyMM(ESMatrix *result, ESMatrix *lhs, ESMatrix * rhs);


#endif //NATIVEOPENGL_GLHELPER_H
