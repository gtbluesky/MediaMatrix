//
// Created by gtbluesky on 2019-07-16.
//

#ifndef NATIVEOPENGL_ANDROID_LOG_H
#define NATIVEOPENGL_ANDROID_LOG_H

#include <android/log.h>

#define JNI_DEBUG 1
#define JNI_TAG "JNI_LOG"

#define LOGV(format, ...) if (JNI_DEBUG) { __android_log_print(ANDROID_LOG_VERBOSE, JNI_TAG, format, ##__VA_ARGS__); }
#define LOGD(format, ...) if (JNI_DEBUG) { __android_log_print(ANDROID_LOG_DEBUG, JNI_TAG, format, ##__VA_ARGS__); }
#define LOGI(format, ...) if (JNI_DEBUG) { __android_log_print(ANDROID_LOG_INFO,  JNI_TAG, format, ##__VA_ARGS__); }
#define LOGW(format, ...) if (JNI_DEBUG) { __android_log_print(ANDROID_LOG_WARN,  JNI_TAG, format, ##__VA_ARGS__); }
#define LOGE(format, ...) if (JNI_DEBUG) { __android_log_print(ANDROID_LOG_ERROR, JNI_TAG, format, ##__VA_ARGS__); }
#define LOGF(format, ...) if (JNI_DEBUG) { __android_log_print(ANDROID_LOG_FATAL, JNI_TAG, format, ##__VA_ARGS__); }

#endif //NATIVEOPENGL_ANDROID_LOG_H
