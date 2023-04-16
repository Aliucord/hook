# AliuHook

Java Xposed Api for [LSPlant](https://github.com/LSPosed/LSPlant)

Please note that this is only a partial implementation of Xposed, since we only need method hooking.
Thus, only XposedBridge and hook classes are implemented. If you need the rest, just copy paste it
from original Xposed and it should work with almost no modificiations.

Additionally, XposedBridge contains these new methods:

- `makeClassInheritable` - Makes a final class inheritable, see LSPlant doc for more info
- `deoptimizeMethod` - Deoptimises method to solve inline issues, see LSPlant doc for more info
- `disableProfileSaver` - Disables Android Profile Saver to try to prevent ahead of time compilation
  of code which leads to aggressive inlining,
  see https://source.android.com/devices/tech/dalvik/configure#how_art_works
- `disableHiddenApiRestrictions` - Disables all hidden api restrictions, allowing full access to
  internal Android APIs,
  see https://developer.android.com/guide/app-compatibility/restrictions-non-sdk-interfaces

## Supported Android versions (same as LSPlant)

- Android 5.0 - 13 (API level 21 - 34)
- armeabi-v7a, arm64-v8a, x86, x86-64

## Get Started

```gradle
repositories {
    maven("https://maven.aliucord.com/snapshots")
}

dependencies {
    // or change main-SNAPSHOT to short commit hash to target a specific commit
    implementation "com.aliucord:Aliuhook:main-SNAPSHOT"
}
```

#### Now you're ready to get hooking! No init needed

```java
XposedBridge.hookMethod(Activity.class.getDeclaredMethod("onCreate", Bundle.class), new XC_MethodHook() {
    @Override
    public void beforeHookedMethod(MethodHookParam param) throws Throwable {
        Log.d(TAG, "Activity" + param.thisObject + "about to be created!");
    }
});
```

## Credits

- [LSPlant](https://github.com/LSPosed/LSPlant) obviously
- [Dobby](https://github.com/LSPosed/Dobby) - a lightweight, multi-platform, multi-architecture hook framework
- [Pine](https://github.com/canyie/Pine) - AliuHook uses Pine's ElfImg parser
- [Original Xposed API](https://github.com/rovo89/XposedBridge) 
