package com.aliucord.hook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
    @SuppressWarnings("ConstantConditions")
    public void shouldHookNative() throws Throwable {
        var method = UnitTest.class.getDeclaredMethod("shouldHookNative");

        var isHooked = XposedBridge.class.getDeclaredMethod("isHooked0", Member.class);
        isHooked.setAccessible(true);

        assertFalse((boolean) isHooked.invoke(null, method));

        XposedBridge.hookMethod(isHooked, XC_MethodReplacement.returnConstant(true));

        assertTrue((boolean) isHooked.invoke(null, method));
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

    @Test
    public void shouldAllocateInstance() {
        var instance = XposedBridge.allocateInstance(Dummy.class);
        assertNotNull("failed to alloc", instance);
        assertFalse(instance.initialized);
    }

    @Test
    public void shouldInvokeConstructor() throws Throwable {
        var instance = XposedBridge.allocateInstance(Dummy.class);
        assertFalse("constructor not supposed to be called", instance.initialized);

        var success = XposedBridge.invokeConstructor(instance, Dummy.class.getDeclaredConstructor());
        assertTrue("invokeConstructor failed", success);
        assertTrue("constructor not called", instance.initialized);
    }

    @Test
    public void shouldInvokeSuperConstructor() throws Throwable {
        var instance = XposedBridge.allocateInstance(Dummy.Dummy2.class);
        assertFalse("constructor not supposed to be called", instance.initialized);

        var success = XposedBridge.invokeConstructor(instance, Dummy.class.getDeclaredConstructor());
        assertTrue("invokeConstructor failed", success);
        assertTrue("supertype ctor not called", instance.initialized);
        assertNull("subtype ctor should not be called", instance.d);
    }

    @Test
    public void shouldInvokeSubConstructor() throws Throwable {
        var instance = XposedBridge.allocateInstance(Dummy.Dummy2.class);
        assertFalse("constructor not supposed to be called", instance.initialized);

        var success = XposedBridge.invokeConstructor(instance, Dummy.Dummy2.class.getDeclaredConstructor());
        assertTrue("invokeConstructor failed", success);
        assertTrue("supertype ctor not called", instance.initialized);
        assertEquals("subtype ctor not called", "dummy2", instance.d);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotInvokeVarargsConstructor() throws Throwable {
        XposedBridge.invokeConstructor(new Dummy(), Dummy.class.getDeclaredConstructor(Object[].class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailInvokeConstructorWrongArgCount() throws Throwable {
        XposedBridge.invokeConstructor(
                new Dummy(),
                Dummy.class.getDeclaredConstructor(),
                "balls"
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailInvokeConstructorWrongArgs() throws Throwable {
        var constructor = Dummy.class.getDeclaredConstructor(Object.class, int.class, Integer.class, String.class);
        XposedBridge.invokeConstructor(
                new Dummy(),
                constructor,
                "balls",
                Integer.valueOf(1),
                "ballsv2",
                new Object()
        );
    }

    @Test
    public void shouldInvokeArgsConstructor() throws Throwable {
        var a = new Object();
        var b = 42;
        var c = Integer.valueOf(420);
        var d = "balls";

        var instance = XposedBridge.allocateInstance(Dummy.class);
        var constructor = Dummy.class.getDeclaredConstructor(Object.class, int.class, Integer.class, String.class);
        var success = XposedBridge.invokeConstructor(instance, constructor, a, b, c, d);
        assertTrue("invokeConstructor failed", success);
        assertFalse("wrong constructor called", instance.initialized);
        assertEquals("a does not match", a, instance.a);
        assertEquals("b does not match", b, instance.b);
        assertEquals("c does not match", c, instance.c);
        assertEquals("d does not match", d, instance.d);
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

            XposedBridge.disableHiddenApiRestrictions();

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
