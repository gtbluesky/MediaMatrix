cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -fno-rtti -fno-exceptions")

file(GLOB CPP_SRCS "*.cpp")
file(GLOB_RECURSE COMMON_SRCS "common/*.cpp")
file(GLOB_RECURSE EGL_SRCS "egl/*.cpp")
file(GLOB_RECURSE GLES_SRCS "gles/*.cpp")
file(GLOB_RECURSE FILTER_SRCS "filter/*.cpp")
file(GLOB_RECURSE RENDER_SRCS "render/*.cpp")
file(GLOB_RECURSE SHAPE_SRCS "shape/*.cpp")

add_subdirectory(glm)

add_library(
        native_gl

        SHARED

        ${CPP_SRCS}
        ${COMMON_SRCS}
        ${EGL_SRCS}
        ${GLES_SRCS}
        ${FILTER_SRCS}
        ${RENDER_SRCS}
        ${SHAPE_SRCS}
)

target_link_libraries(
        native_gl
        android
        EGL
        GLESv3
        log
        glm
)