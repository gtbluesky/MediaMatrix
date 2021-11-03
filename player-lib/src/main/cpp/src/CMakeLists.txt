# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

#cmake_minimum_required(VERSION 3.4.1)
#include_directories(../json/include)
# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
#aux_source_directory(${CMAKE_HOME_DIRECTORY} SRC_LIST)
aux_source_directory(../json JSON_CPP_SRC_LIST)
aux_source_directory(../soundtouch SOUNDTOUCH_SRC_LIST)

add_library( # Sets the name of the library.
        matrixplayer

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        AppUtil.cpp
        LogUtil.cpp
        SecurityUtil.cpp
        MatrixPlayer.cpp
        AudioInfo.cpp
        FFmpegController.cpp
        NativeCallJava.cpp
        PacketQueue.cpp
        PlayStatus.cpp
        PcmBean.cpp
        PcmBufferQueue.cpp
        VideoInfo.cpp

        ${JSON_CPP_SRC_LIST}
        ${SOUNDTOUCH_SRC_LIST})

add_library(ffmpeg SHARED IMPORTED)
set_target_properties(ffmpeg
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../nativeLibs/${ANDROID_ABI}/libffmpeg.so)


#add_library(avcodec SHARED IMPORTED)
#set_target_properties(avcodec
#        PROPERTIES IMPORTED_LOCATION
#        ${CMAKE_SOURCE_DIR}/../nativeLibs/${ANDROID_ABI}/libavcodec.so)

#add_library(avformat SHARED IMPORTED)
#set_target_properties(avformat
#        PROPERTIES IMPORTED_LOCATION
#        ${CMAKE_SOURCE_DIR}/../nativeLibs/${ANDROID_ABI}/libavformat.so)
#
#
#add_library(avutil SHARED IMPORTED)
#set_target_properties(avutil
#        PROPERTIES IMPORTED_LOCATION
#        ${CMAKE_SOURCE_DIR}/../nativeLibs/${ANDROID_ABI}/libavutil.so)
#
#add_library(swresample SHARED IMPORTED)
#set_target_properties(swresample
#        PROPERTIES IMPORTED_LOCATION
#        ${CMAKE_SOURCE_DIR}/../nativeLibs/${ANDROID_ABI}/libswresample.so)
#
#
#add_library(swscale SHARED IMPORTED)
#set_target_properties(swscale
#        PROPERTIES IMPORTED_LOCATION
#        ${CMAKE_SOURCE_DIR}/../nativeLibs/${ANDROID_ABI}/libswscale.so)

#add_library(curl STATIC IMPORTED)
#set_target_properties(curl
#        PROPERTIES IMPORTED_LOCATION
#        ${CMAKE_SOURCE_DIR}/../nativeLibs/${ANDROID_ABI}/libcurl.a)

add_library(crypto STATIC IMPORTED)
set_target_properties(crypto
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../nativeLibs/${ANDROID_ABI}/libcrypto.a)

add_library(ssl STATIC IMPORTED)
set_target_properties(ssl
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../nativeLibs/${ANDROID_ABI}/libssl.a)


#add_library(jsoncpp STATIC IMPORTED)
#set_target_properties(jsoncpp
#        PROPERTIES IMPORTED_LOCATION
#        ${CMAKE_SOURCE_DIR}/../nativeLibs/${ANDROID_ABI}/libjsoncpp.a)

target_link_libraries( # Specifies the target library.
        matrixplayer
        ffmpeg
#        avcodec
#        avformat
#        avutil
#        swresample
#        swscale
#        curl
#        jsoncpp
        ssl
        crypto
        OpenSLES
        log)