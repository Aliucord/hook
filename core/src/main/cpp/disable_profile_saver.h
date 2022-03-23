/*
 * This file is part of AliuHook, a library providing XposedAPI bindings to LSPlant
 * Copyright (c) 2021 Juby210 & Vendicated
 * Licensed under the Open Software License version 3.0
 */

#ifndef ALIUHOOK_DISABLE_PROFILE_SAVER_H
#define ALIUHOOK_DISABLE_PROFILE_SAVER_H

#include <jni.h>
#include "elf_img.h"

bool disableProfileSaver(jint android_version, pine::ElfImg *elf_img);

#endif //ALIUHOOK_DISABLE_PROFILE_SAVER_H
