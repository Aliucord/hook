//
// Created by ven on 24/03/2022.
//

#include "hidden_api.h"

#include <dobby.h>
#include "elf_img.h"
#include "log.h"
#include "aliuhook.h"

static int replacer() {
    return 0;
}

bool disable_hidden_api() {
    bool success = true;

#define HOOK(symbol) { \
    void* addr = AliuHook::elf_img.GetSymbolAddress((symbol), false); \
    void* backup; \
    if (addr) { \
        DobbyHook(addr, reinterpret_cast<void*>(replacer), &backup); \
    } else { \
        LOGE("HiddenAPI bypass: Couldn't find symbol " symbol); \
        success = false; \
    } \
}

    if (AliuHook::android_version >= 29) {
        HOOK("_ZN3art9hiddenapi6detail28ShouldDenyAccessToMemberImplINS_9ArtMethodEEEbPT_NS0_7ApiListENS0_12AccessMethodE");
        HOOK("_ZN3art9hiddenapi6detail28ShouldDenyAccessToMemberImplINS_8ArtFieldEEEbPT_NS0_7ApiListENS0_12AccessMethodE");
    } else if (AliuHook::android_version == 28) {
        HOOK("_ZN3art9hiddenapi6detail19GetMemberActionImplINS_9ArtMethodEEENS0_6ActionEPT_NS_20HiddenApiAccessFlags7ApiListES4_NS0_12AccessMethodE");
        HOOK("_ZN3art9hiddenapi6detail19GetMemberActionImplINS_8ArtFieldEEENS0_6ActionEPT_NS_20HiddenApiAccessFlags7ApiListES4_NS0_12AccessMethodE");
    }

    return success;

#undef HOOK
}
