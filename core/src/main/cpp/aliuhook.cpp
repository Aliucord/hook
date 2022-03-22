/*
 * This file is part of AliuHook, a library providing XposedAPI bindings to LSPlant
 * Copyright (c) 2021 Juby210 & Vendicated
 * Licensed under the Open Software License version 3.0
 */

#include <jni.h>
#include <string>
#include <lsplant.hpp>
#include <dobby.h>
#include <sys/mman.h>
#include <bits/sysconf.h>
#include "elf_img.h"
#include "log.h"

static size_t page_size_;

// Method taken from https://github.com/canyie/pine/blob/8fd10e7c7f4d64bbed5710ca446bd943b047b696/enhances/src/main/cpp/enhances.cpp#L60-L69
static bool Unprotect(void *addr) {
    size_t alignment = (uintptr_t) addr % page_size_;
    void *aligned_ptr = (void *) ((uintptr_t) addr - alignment);
    int result = mprotect(aligned_ptr, page_size_, PROT_READ | PROT_WRITE | PROT_EXEC);
    if (result == -1) {
        LOGE("mprotect failed for %p: %s (%d)", addr, strerror(errno), errno);
        return false;
    }
    return true;
}

void *InlineHooker(void *address, void *replacement) {
    if (!Unprotect(address)) {
        return nullptr;
    }

    void *origin_call;
    if (DobbyHook(address, replacement, &origin_call) == RS_SUCCESS) {
        return origin_call;
    } else {
        return nullptr;
    }
}

bool InlineUnhooker(void *func) {
    return DobbyDestroy(func) == RT_SUCCESS;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_de_robv_android_xposed_XposedBridge_isHooked0(JNIEnv *env, jclass, jobject method) {
    return lsplant::IsHooked(env, method);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_de_robv_android_xposed_XposedBridge_hook0(JNIEnv *env, jclass, jobject context,
                                               jobject original,
                                               jobject callback) {
    return lsplant::Hook(env, original, context, callback);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_de_robv_android_xposed_XposedBridge_unhook0(JNIEnv *env, jclass, jobject target) {
    return lsplant::UnHook(env, target);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_de_robv_android_xposed_XposedBridge_deoptimize0(JNIEnv *env, jclass, jobject method) {
    return lsplant::Deoptimize(env, method);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_de_robv_android_xposed_XposedBridge_makeClassInheritable0(JNIEnv *env, jclass, jclass clazz) {
    return lsplant::MakeClassInheritable(env, clazz);
}

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *) {
    JNIEnv *env;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    page_size_ = static_cast<const size_t>(sysconf(_SC_PAGESIZE));
    jint android_version = env->GetVersion();

    pine::ElfImg art("libart.so", android_version);
    lsplant::InitInfo initInfo{
            .inline_hooker = InlineHooker,
            .inline_unhooker = InlineUnhooker,
            .art_symbol_resolver = [&art](std::string_view symbol) -> void * {
                void *out = reinterpret_cast<void *>(art.GetSymbolAddress(symbol));
                return out;
            }
    };

    bool res = lsplant::Init(env, initInfo);
    if (!res) {
        LOGE("lsplant init failed");
        return JNI_ERR;
    }

    LOGI("lsplant init finished");

    return JNI_VERSION_1_6;
}