package com.aliucord.hook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.os.Build;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.*;

import de.robv.android.xposed.*;

@RunWith(AndroidJUnit4.class)
public class UnitTest {
    private int counter = 0;

    private static int add(int x, int y) {
        return x + y;
    }

    private void count() {
        counter++;
    }

    @SuppressWarnings("JavaJniMissingFunction")
    public native void method0();

    static abstract class AbstractClass {
        abstract void abstractMethod();
    }

    interface Interface {
        void interfaceMethod();
    }


    @Test
    public void shouldHook() throws Throwable {
        int x = 2;
        int y = 3;
        var addMethod = UnitTest.class.getDeclaredMethod("add", int.class, int.class);

        assertEquals(add(x, y), 5);

        var unhook1 = XposedBridge.hookMethod(addMethod, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                param.args[0] = 0;
            }
        });

        assertEquals(add(x, y), 3);

        var unhook2 = XposedBridge.hookMethod(addMethod, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                param.setResult((int) param.getResult() * 10);
            }
        });

        assertEquals(add(x, y), 30);

        unhook1.unhook();

        assertEquals(add(x, y), 50);

        unhook2.unhook();

        assertEquals(add(x, y), 5);
    }

    @Test
    public void shouldReplace() throws Throwable {
        var countMethod = UnitTest.class.getDeclaredMethod("count");
        counter = 0;

        count();

        assertEquals(counter, 1);

        XposedBridge.hookMethod(countMethod, XC_MethodReplacement.DO_NOTHING);
        count();

        assertEquals(counter, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotHookAbstract() throws Throwable {
        XposedBridge.hookMethod(AbstractClass.class.getDeclaredMethod("abstractMethod"), XC_MethodReplacement.DO_NOTHING);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotHookInterface() throws Throwable {
        XposedBridge.hookMethod(Interface.class.getDeclaredMethod("interfaceMethod"), XC_MethodReplacement.DO_NOTHING);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotHookField() throws Throwable {
        XposedBridge.hookMethod(UnitTest.class.getDeclaredField("counter"), XC_MethodReplacement.DO_NOTHING);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotHookNative() throws Throwable {
        XposedBridge.hookMethod(UnitTest.class.getDeclaredMethod("method0"), XC_MethodReplacement.DO_NOTHING);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotHookProxyClass() throws Throwable {
        var proxy = Proxy.getProxyClass(UnitTest.class.getClassLoader(), Interface.class);
        XposedBridge.hookMethod(proxy.getDeclaredMethod("interfaceMethod"), XC_MethodReplacement.DO_NOTHING);
    }

    @Test
    public void shouldDisableProfileSaver() {
        assertTrue(XposedBridge.disableProfileSaver());
    }

    @Test
    public void shouldBypassHiddenApi() throws Throwable {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            boolean hiddenMethodFound = true;
            boolean hiddenFieldFound = true;

            // Should fail due to being hidden api
            try {
                obtainHiddenMethod();
            } catch (NoSuchMethodException ignored) {
                hiddenMethodFound = false;
            }
            try {
                obtainHiddenField();
            } catch (NoSuchFieldException ignored) {
                hiddenFieldFound = false;
            }

            assertFalse("Method found without bypass", hiddenMethodFound);
            assertFalse("Field found without bypass", hiddenFieldFound);

            assertTrue("Failed to disable hidden api restrictions", XposedBridge.disableHiddenApiRestrictions());

            // Now should work
            var method = obtainHiddenMethod();
            var field = obtainHiddenField();

            assertEquals(method.getName(), "setHiddenApiExemptions");
            assertEquals(field.getType(), Class.forName("dalvik.system.VMRuntime$HiddenApiUsageLogger"));
        }
    }

    private Method obtainHiddenMethod() throws Throwable {
        return Class.forName("dalvik.system.VMRuntime").getDeclaredMethod("setHiddenApiExemptions", String[].class);
    }

    private Field obtainHiddenField() throws Throwable {
        return Class.forName("dalvik.system.VMRuntime").getDeclaredField("hiddenApiUsageLogger");
    }
}
