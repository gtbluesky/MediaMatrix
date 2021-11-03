//
// Created by 甘涛 on 2018/9/25.
//

#ifndef VIDEOEDITSDK_APPUTIL_H
#define VIDEOEDITSDK_APPUTIL_H

#include <jni.h>
#include <cstring>

class AppUtil {
public:
    const static char *getAppPackageName(JNIEnv *env, jobject context);

    const static char *getMetaData(JNIEnv *env, jobject context, const char* name);

    static void showToast(JNIEnv *env, jobject context, const char* msg);

private:
    static jstring getAppPackageNameStr(JNIEnv *env, jobject context);
//
//char *getAppSignature(JNIEnv *env, jobject context);
//
//int getAppVersionCode(JNIEnv *env, jobject context);
//
//char *getAppVersionName(JNIEnv *env, jobject context);
//
//char *getAppVersionName(JNIEnv *env, jobject context);
};


#endif //VIDEOEDITSDK_APPUTIL_H
