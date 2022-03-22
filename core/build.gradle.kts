plugins {
    id("com.android.library")
}

dependencies {
    implementation("org.lsposed.lsplant:lsplant:3.1")
    implementation("io.github.vvb2060.ndk:dobby:1.2")
}

android {
    compileSdk = 32
    buildToolsVersion = "32.0.0"
    ndkVersion = "24.0.8215888"

    buildFeatures {
        buildConfig = false
        prefab = true
    }

    defaultConfig {
        minSdk = 21
        targetSdk = 32

        externalNativeBuild {
            cmake {
                arguments += "-DANDROID_STL=c++_shared"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.18.1"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}