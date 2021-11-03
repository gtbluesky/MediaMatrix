//
// Created by 甘涛 on 2018/9/28.
//

#ifndef VIDEOEDITSDK_LOGUTIL_H
#define VIDEOEDITSDK_LOGUTIL_H

#include <android/log.h>

#define SHOW_LOG true
#define TAG "JNI_LOG" // 这个是自定义的LOG的标识
#define LOGD(...) __android_log_vprint(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__) // 定义LOGD类型
#define LOGI(...) __android_log_vprint(ANDROID_LOG_INFO, TAG,  __VA_ARGS__) // 定义LOGI类型
#define LOGW(...) __android_log_vprint(ANDROID_LOG_WARN, TAG, __VA_ARGS__) // 定义LOGW类型
#define LOGE(...) __android_log_vprint(ANDROID_LOG_ERROR, TAG, __VA_ARGS__) // 定义LOGE类型
#define LOGF(...) __android_log_vprint(ANDROID_LOG_FATAL, TAG, __VA_ARGS__) // 定义LOGF类型

class LogUtil {

public:
    static void d(const char* log, ...);
    static void i(const char* log, ...);
    static void w(const char* log, ...);
    static void e(const char* log, ...);
    static void f(const char* log, ...);
};


#endif //VIDEOEDITSDK_LOGUTIL_H
