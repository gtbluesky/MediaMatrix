//
// Created by 甘涛 on 2018/9/28.
//

#include "LogUtil.h"

void LogUtil::d(const char* log, ...) {
    if (!SHOW_LOG) {
        return;
    }
    va_list arg;
    va_start(arg, log);
    LOGD(log, arg);
    va_end(arg);
}

void LogUtil::i(const char* log, ...) {
    if (!SHOW_LOG) {
        return;
    }
    va_list arg;
    va_start(arg, log);
    LOGI(log, arg);
    va_end(arg);
}

void LogUtil::w(const char* log, ...) {
    if (!SHOW_LOG) {
        return;
    }
    va_list arg;
    va_start(arg, log);
    LOGW(log, arg);
    va_end(arg);
}

void LogUtil::e(const char* log, ...) {
    if (!SHOW_LOG) {
        return;
    }
    va_list arg;
    va_start(arg, log);
    LOGE(log, arg);
    va_end(arg);
}

void LogUtil::f(const char* log, ...) {
    if (!SHOW_LOG) {
        return;
    }
    va_list arg;
    va_start(arg, log);
    LOGF(log, arg);
    va_end(arg);
}