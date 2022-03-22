package com.aliucord.hook;

import java.lang.reflect.Member;
import java.lang.reflect.Method;

public class AliuHook {
    static {
        System.loadLibrary("aliuhook");
    }

    public Method backup;

    private Member target;
    private Method replacement;
    private Object owner;

    private native Method hook0(Member original, Method callback);
    private native boolean unhook0(Member target);

    public Object callback(Object[] args) {
        return "hooked";
    }

    // TODO make this actually take custom replacement and handle multiple replacements
    public static void hook(Member target, Object owner) throws Throwable {
        var hook = new AliuHook();
        hook.owner = owner;
        hook.target = target;
        var method = AliuHook.class.getDeclaredMethod("callback", Object[].class);
        hook.backup = hook.hook0(target, method);
        if (hook.backup == null) {
            return;
        }
    }
}