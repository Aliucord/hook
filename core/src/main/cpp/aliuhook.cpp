#include <jni.h>
#include <string>
#include <lsplant.hpp>
#include <dobby.h>
#include <sys/mman.h>
#include <bits/sysconf.h>
#include "elf_img.h"
#include "log.h"

// Method taken from https://github.com/canyie/pine/blob/8fd10e7c7f4d64bbed5710ca446bd943b047b696/enhances/src/main/cpp/enhances.cpp#L60-L69
static size_t page_size_;

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

void *InlineHooker(void *target, void *hooker) {
    if (!Unprotect(target)) {
        return nullptr;
    }

    void *origin_call;
    if (DobbyHook(target, hooker, &origin_call) == RS_SUCCESS) {
        return origin_call;
    } else {
        return nullptr;
    }
}

bool InlineUnhooker(void *func) {
    return DobbyDestroy(func) == RT_SUCCESS;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_aliucord_hook_AliuHook_hook0(JNIEnv *env, jobject clazz, jobject original,
                                      jobject callback) {
    return lsplant::Hook(env, original, clazz, callback);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_aliucord_hook_AliuHook_unhook0(JNIEnv *env, jobject, jobject target) {
    return lsplant::UnHook(env, target);
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