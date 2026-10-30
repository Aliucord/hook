# AliuHook

Java Xposed Api for [LSPlant](https://github.com/LSPosed/LSPlant)

LSPlant is built from source at commit
[`6ac2280`](https://github.com/LSPosed/LSPlant/commit/6ac2280d8d88e14f0864c26ac97c1dc96713bde1)
and linked statically into `libaliuhook.so`.

Please note that this is only a partial implementation of Xposed, since we only need method hooking.
Thus, only XposedBridge and hook classes are implemented. If you need the rest, just copy paste it
from original Xposed and it should work with almost no modificiations.

Additionally, XposedBridge contains these new methods:

- `allocateInstance` - Allocates a class instance without calling any constructors
   Can be used as a simpler alternative to `sun.misc.Unsafe#allocateInstance`
- `invokeConstructor` - Invokes a constructor for an existing instance, most useful in conjunction
   raw instance allocation.
- `makeClassInheritable` - Makes a final class inheritable, see LSPlant doc for more info
- `deoptimizeMethod` - Deoptimises method to solve inline issues, see LSPlant doc for more info
- `disableProfileSaver` - Disables Android Profile Saver to try to prevent ahead of time compilation
  of code which leads to aggressive inlining,
  see https://source.android.com/devices/tech/dalvik/configure#how_art_works
- `disableHiddenApiRestrictions` - Disables all hidden api restrictions, allowing full access to
  internal Android APIs,
  see https://developer.android.com/guide/app-compatibility/restrictions-non-sdk-interfaces

## Android compatibility

- LSPlant supports Android 5 - 17 (API level 21 - 37).
- armeabi-v7a, arm64-v8a, x86, x86-64

## Get Started

Build and publish AliuHook to your local Maven repository (JDK 17 or newer and an
Android SDK are required). Install CMake `4.1.2` with the SDK Manager
(`sdkmanager "cmake;4.1.2"`) and accept the Android SDK licenses first.
Gradle installs NDK `30.0.16248370` if needed.

```sh
git submodule update --init core/src/main/cpp/xz core/src/main/cpp/lsplant
git -C core/src/main/cpp/lsplant submodule update --init --recursive lsplant/src/main/jni/external/dex_builder
./gradlew :core:publishToMavenLocal
```

On Windows use `gradlew.bat`. Only the native submodules above are needed;
LSPlant's upstream test submodules use SSH and are not required to build AliuHook.
The release AAR is also available in `core/build/outputs/aar/core-release.aar`
and as an artifact of the Build workflow.

To use the locally published library:

```gradle
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation "com.aliucord:Aliuhook:1.1.4-api37.1"
}
```

Run the instrumentation suite with a connected device or emulator:

```sh
./gradlew :core:connectedDebugAndroidTest
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
