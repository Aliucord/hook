plugins {
    id("com.android.library")
    id("maven-publish")
}

dependencies {
    implementation("org.lsposed.lsplant:lsplant:3.2")
    implementation("io.github.vvb2060.ndk:dobby:1.2")

    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test:runner:1.4.0")
}

android {
    compileSdk = 31
    buildToolsVersion = "32.0.0"
    ndkVersion = "24.0.8215888"

    buildFeatures {
        buildConfig = false
        prefab = true
    }

    defaultConfig {
        minSdk = 21
        targetSdk = 31
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

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

afterEvaluate {
    publishing {
        publications {
            register(project.name, MavenPublication::class.java) {
                group = "com.aliucord"
                artifactId = "Aliuhook"

                from(components["release"])
            }

            repositories {
                val username = System.getenv("MAVEN_USERNAME")
                val password = System.getenv("MAVEN_PASSWORD")

                if (username != null && password != null) {
                    maven {
                        credentials {
                            this.username = username
                            this.password = password
                        }
                        setUrl("https://maven.aliucord.com/snapshots")
                    }
                } else {
                    mavenLocal()
                }
            }
        }
    }
}
