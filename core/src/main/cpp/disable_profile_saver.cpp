/*
 * This file is part of AliuHook, a library providing XposedAPI bindings to LSPlant
 * Copyright (c) 2021 Juby210 & Vendicated
 * Licensed under the Open Software License version 3.0
 */

#include "disable_profile_saver.h"
#include <dobby.h>

#include "log.h"

static bool stub_miui(bool a, uint16_t *b, bool c) {
    LOGD("Ignoring profile saver request");
    return true;
}

static bool stub_below_26(uint16_t *a) {
    LOGD("Ignoring profile saver request");
    return true;
}


static bool stub_below_31(bool a, uint16_t *b) {
    LOGD("Ignoring profile saver request");
    return true;
}

static bool stub_above_31(bool a, bool b, uint16_t *c) {
    LOGD("Ignoring profile saver request");
    return true;
}

static void* backup = nullptr;

bool disableProfileSaver(jint android_version, pine::ElfImg *elf_img) {
    if (backup) {
        LOGW("disableProfileSaver called multiple times - It is already disabled.");
        return true;
    }

    void *stub;

    void *process_profiling_info;
    {
        // MIUI moment, see https://github.com/canyie/pine/commit/ef0f5fb08e6aa42656065e431c65106b41f87799
        process_profiling_info = elf_img->GetSymbolAddress(
                "_ZN3art12ProfileSaver20ProcessProfilingInfoEbPtb", false);
        if (!process_profiling_info) {
            const char *symbol;
            if (android_version < 26) {
                // https://android.googlesource.com/platform/art/+/nougat-release/runtime/jit/profile_saver.cc#270
                symbol = "_ZN3art12ProfileSaver20ProcessProfilingInfoEPt";
                stub = reinterpret_cast<void *>(stub_below_26);
            } else if (android_version < 31) {
                // https://android.googlesource.com/platform/art/+/android11-release/runtime/jit/profile_saver.cc#514
                symbol = "_ZN3art12ProfileSaver20ProcessProfilingInfoEbPt";
                stub = reinterpret_cast<void *>(stub_below_31);
            } else {
                // https://android.googlesource.com/platform/art/+/android12-release/runtime/jit/profile_saver.cc#823
                symbol = "_ZN3art12ProfileSaver20ProcessProfilingInfoEbbPt";
                stub = reinterpret_cast<void *>(stub_above_31);
            }
            process_profiling_info = elf_img->GetSymbolAddress(symbol);
        } else {
            stub = reinterpret_cast<void *>(stub_miui);
        }
    }

    if (!process_profiling_info) {
        LOGE("Failed to disable ProfileSaver: ProfileSaver::ProcessProfilingInfo not found");
        return false;
    }

    DobbyHook(process_profiling_info, stub, &backup);

    LOGI("Successfully disabled ProfileSaver");

    return true;
}