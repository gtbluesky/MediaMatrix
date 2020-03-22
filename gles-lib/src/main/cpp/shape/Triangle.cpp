//
// Created by gtbluesky on 2019-07-18.
//

#include "Triangle.h"

const GLint COORDS_PER_VERTEX = 3;
const GLint vertexStride = COORDS_PER_VERTEX * 4;

int Triangle::init() {
    char vertexShader[] =
            "#version 300 es\n"
            "layout(location = 0) in vec4 aPosition;\n"
            "layout(location = 1) in vec4 a_Color;\n"
            "out vec4 v_Color;"
            "void main()\n"
            "{\n"
            "   gl_Position = aPosition;\n"
            "   v_Color = a_Color;\n"
            "}\n";

    char fragmentShader[] =
            "#version 300 es\n"
            "precision mediump float;\n"
            "in vec4 v_Color;\n"
            "out vec4 fragColor;\n"
            "void main()\n"
            "{\n"
            "   fragColor = v_Color;\n"
            "}\n";
    programHandle = createProgram(vertexShader, fragmentShader);
    if (programHandle <= 0) {
        return -1;
    }
    glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    return 0;
}


void Triangle::onDraw(int width, int height) {
    GLfloat vertices[] = {
            0.0f,  0.5f, 0.0f,
            -0.5f, -0.5f, 0.0f,
            0.5f, -0.5f, 0.0f
    };

    GLfloat color[] = {
            1.0f, 0.0f, 0.0f, 1.0f
    };

    GLint vertexCount = sizeof(vertices) / (sizeof(vertices[0]) * COORDS_PER_VERTEX);

    glViewport(0, 0, width, height);

    glClear(GL_COLOR_BUFFER_BIT);

    glUseProgram(programHandle);

    GLint positionHandle = glGetAttribLocation(programHandle, "aPosition");
    glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, GL_FLOAT, GL_FALSE, vertexStride, vertices);
    glEnableVertexAttribArray(positionHandle);
    glVertexAttrib4fv(1, color);

    glDrawArrays(GL_TRIANGLES, 0, vertexCount);

    glDisableVertexAttribArray(positionHandle);
}

void Triangle::destroy() {
    if (programHandle > 0) {
        glDeleteProgram(programHandle);
    }
    programHandle = -1;
}