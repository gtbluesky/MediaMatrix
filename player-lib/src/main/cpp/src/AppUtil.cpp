//
// Created by 甘涛 on 2018/9/25.
//

#include "AppUtil.h"

const char *AppUtil::getAppPackageName(JNIEnv *env, jobject context) {
    jclass context_class = env->GetObjectClass(context);
    jmethodID getPackageNameId = env->GetMethodID(context_class, "getPackageName",
                                                  "()Ljava/lang/String;");
    jstring packNameString = (jstring) env->CallObjectMethod(context, getPackageNameId);
    env->DeleteLocalRef(context_class);
    jboolean isCopy;
    const char *pack_name = env->GetStringUTFChars(packNameString, &isCopy);
    return pack_name;
}

const char *AppUtil::getMetaData(JNIEnv *env, jobject context, const char *name) {
    jclass context_class = env->GetObjectClass(context);

    //context.getPackageManager()
    jmethodID methodId = env->GetMethodID(context_class, "getPackageManager",
                                          "()Landroid/content/pm/PackageManager;");
    jobject package_manager_object = env->CallObjectMethod(context, methodId);
    if (!package_manager_object) {
        return NULL;
    }

    //context.getPackageName()
    jstring package_name_string = getAppPackageNameStr(env, context);
    if (!package_name_string) {
        return NULL;
    }

    env->DeleteLocalRef(context_class);

    //PackageManager.getApplicationInfo(String, int)
    jclass pack_manager_class = env->GetObjectClass(package_manager_object);
    methodId = env->GetMethodID(pack_manager_class, "getApplicationInfo",
                                "(Ljava/lang/String;I)Landroid/content/pm/ApplicationInfo;");
    env->DeleteLocalRef(pack_manager_class);
    jobject application_info_object = env->CallObjectMethod(package_manager_object, methodId,
                                                            package_name_string, 128);
    if (!application_info_object) {
        return NULL;
    }

    env->DeleteLocalRef(package_manager_object);

    jclass application_info_class = env->GetObjectClass(application_info_object);

    //activityInfo.metaData
    jfieldID jfield_id = env->GetFieldID(application_info_class, "metaData", "Landroid/os/Bundle;");
    env->DeleteLocalRef(application_info_class);
    jobject meta_data = env->GetObjectField(application_info_object, jfield_id);
    env->DeleteLocalRef(application_info_object);
    //metaData.getString(String)
    jclass bundle_class = env->GetObjectClass(meta_data);
    methodId = env->GetMethodID(bundle_class, "getString",
                                "(Ljava/lang/String;)Ljava/lang/String;");
    jstring name_str = env->NewStringUTF(name);
    jstring value_str = (jstring) env->CallObjectMethod(meta_data, methodId, name_str);
    if (!value_str) {
        return "";
    }
    jboolean isCopy;
    const char *value = env->GetStringUTFChars(value_str, &isCopy);
    return value;
}

jstring AppUtil::getAppPackageNameStr(JNIEnv *env, jobject context) {
    jclass context_class = env->GetObjectClass(context);
    jmethodID getPackageNameId = env->GetMethodID(context_class, "getPackageName",
                                                  "()Ljava/lang/String;");
    jstring packNameString = (jstring) env->CallObjectMethod(context, getPackageNameId);
    env->DeleteLocalRef(context_class);
    return packNameString;
}
//
//char *getAppSignature(JNIEnv *env, jobject context) {
//    jclass context_class = env->GetObjectClass(context);
//
//    //context.getPackageManager()
//    jmethodID methodId = env->GetMethodID(context_class, "getPackageManager", "()Landroid/content/pm/PackageManager;");
//    jobject package_manager_object = env->CallObjectMethod(context, methodId);
//    if (!package_manager_object) {
//        return NULL;
//    }
//
//    //context.getPackageName()
//    jstring package_name_string = getAppPackageNameStr(env, context);
//    if (!package_name_string) {
//        return NULL;
//    }
//
//    env->DeleteLocalRef(context_class);
//
//    //PackageManager.getPackageInfo(Sting, int)
//    jclass pack_manager_class = env->GetObjectClass(package_manager_object);
//    methodId = env->GetMethodID(pack_manager_class, "getPackageInfo", "(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;");
//    env->DeleteLocalRef(pack_manager_class);
//    jobject package_info_object = env->CallObjectMethod(package_manager_object, methodId, package_name_string, 64);
//    if (!package_info_object) {
//        return NULL;
//    }
//
//    env->DeleteLocalRef(package_manager_object);
//
//    //PackageInfo.signatures[0]
//    jclass package_info_class = env->GetObjectClass(package_info_object);
//    jfieldID fieldId = env->GetFieldID(package_info_class, "signatures", "[Landroid/content/pm/Signature;");
//    env->DeleteLocalRef(package_info_class);
//    jobjectArray signature_object_array = (jobjectArray)env->GetObjectField(package_info_object, fieldId);
//    if (!signature_object_array) {
//        return NULL;
//    }
//    jobject signature_object = env->GetObjectArrayElement(signature_object_array, 0);
//
//    env->DeleteLocalRef(package_info_object);
//
//    //Signature.toCharsString()
//    jclass signature_class = env->GetObjectClass(signature_object);
//    methodId = env->GetMethodID(signature_class, "toCharsString", "()Ljava/lang/String;");
//    env->DeleteLocalRef(signature_class);
//    jstring signature_string = (jstring) env->CallObjectMethod(signature_object, methodId);
//    return signature_string;
//
//}
//
//int getAppVersionCode(JNIEnv *env, jobject context) {
//    jclass context_class = env->GetObjectClass(context);
//
//    //context.getPackageManager()
//    jmethodID methodId = env->GetMethodID(context_class, "getPackageManager", "()Landroid/content/pm/PackageManager;");
//    jobject package_manager_object = env->CallObjectMethod(context, methodId);
//    if (!package_manager_object) {
//        return NULL;
//    }
//
//    //context.getPackageName()
//    jstring package_name_string = getAppPackageNameStr(env, context);
//    if (!package_name_string) {
//        return NULL;
//    }
//
//    env->DeleteLocalRef(context_class);
//
//    //PackageManager.getPackageInfo(Sting, int)
//    jclass pack_manager_class = env->GetObjectClass(package_manager_object);
//    methodId = env->GetMethodID(pack_manager_class, "getPackageInfo", "(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;");
//    env->DeleteLocalRef(pack_manager_class);
//    jobject package_info_object = env->CallObjectMethod(package_manager_object, methodId, package_name_string, 64);
//    if (!package_info_object) {
//        return NULL;
//    }
//
//    env->DeleteLocalRef(package_manager_object);
//
//    jclass package_info_class = env->GetObjectClass(package_info_object);
//
//    //packageInfo.versionCode;
//    jfieldID jfield_id = env->GetFieldID(package_info_class, "versionCode", "I");
//    env->DeleteLocalRef(package_info_class);
//    jint version_code = env->GetIntField(package_info_object, jfield_id);
//    env->DeleteLocalRef(package_info_object);
//    return version_code;
//}
//
//char *getAppVersionName(JNIEnv *env, jobject context) {
//    jclass context_class = env->GetObjectClass(context);
//
//    //context.getPackageManager()
//    jmethodID methodId = env->GetMethodID(context_class, "getPackageManager", "()Landroid/content/pm/PackageManager;");
//    jobject package_manager_object = env->CallObjectMethod(context, methodId);
//    if (!package_manager_object) {
//        return NULL;
//    }
//
//    //context.getPackageName()
//    jstring package_name_string = getAppPackageNameStr(env, context);
//    if (!package_name_string) {
//        return NULL;
//    }
//
//    env->DeleteLocalRef(context_class);
//
//    //PackageManager.getPackageInfo(Sting, int)
//    jclass pack_manager_class = env->GetObjectClass(package_manager_object);
//    methodId = env->GetMethodID(pack_manager_class, "getPackageInfo", "(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;");
//    env->DeleteLocalRef(pack_manager_class);
//    jobject package_info_object = env->CallObjectMethod(package_manager_object, methodId, package_name_string, 64);
//    if (!package_info_object) {
//        return NULL;
//    }
//
//    env->DeleteLocalRef(package_manager_object);
//
//    jclass package_info_class = env->GetObjectClass(package_info_object);
//
//    //packageInfo.versionName;
//    jfieldID jfield_id = env->GetFieldID(package_info_class, "versionName", "Ljava/lang/String;");
//    env->DeleteLocalRef(package_info_class);
//    jstring version_code = (jstring) env->GetObjectField(package_info_object, jfield_id);
//    env->DeleteLocalRef(package_info_object);
//
//    return version_code;}

void AppUtil::showToast(JNIEnv *env, jobject context, const char *msg) {
    jclass jclz_toast = env->FindClass("android/widget/Toast");
    jmethodID jmid_makeText = env->GetStaticMethodID(jclz_toast, "makeText",
                                                     "(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;");
    jstring jmsg = env->NewStringUTF(msg);
    jobject jobj_toast = env->CallStaticObjectMethod(jclz_toast, jmid_makeText, context, jmsg, 0);
    jmethodID jmid_show = env->GetMethodID(jclz_toast, "show", "()V");
    env->CallVoidMethod(jobj_toast, jmid_show);
    env->DeleteLocalRef(jobj_toast);
    env->DeleteLocalRef(jclz_toast);
    env->DeleteLocalRef(jmsg);
}
