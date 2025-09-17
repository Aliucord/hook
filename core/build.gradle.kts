@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.internal.errors.MessageReceiverImpl
import com.android.build.gradle.options.SyncOptions
import com.android.build.gradle.tasks.BundleAar
import com.android.builder.dexing.ClassFileEntry
import com.android.builder.dexing.ClassFileInput
import com.android.builder.dexing.DexArchiveBuilder
import com.android.builder.dexing.DexParameters
import com.android.builder.dexing.r8.ClassFileProviderFactory
import com.google.common.io.Closer
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.BiPredicate
import java.util.stream.Stream

plugins {
    id("com.android.library")
    id("maven-publish")
}

dependencies {
    @Suppress("NewerVersionAvailable")
    implementation("com.aliucord.lsplant:lsplant:6.4-aliucord.4")
    implementation("io.github.vvb2060.ndk:dobby:1.2")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
}

android {
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    ndkVersion = "29.0.13599879" // r28+ compiles for 16-KiB aligned pages by default
    namespace = "com.aliucord.hook.core"

    buildFeatures {
        buildConfig = false
        prefab = true
    }

    defaultConfig {
        minSdk = 21
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
            version = "3.28.0+"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

// fuck you agp
tasks.register("buildDexRelease") {
    outputs.dir(layout.buildDirectory.dir("intermediates/dex/"))

    val compileTask = project.tasks.getByName("compileDebugJavaWithJavac") as AbstractCompile
    dependsOn(compileTask)
    inputs.dir(compileTask.destinationDirectory)

    doLast {
        val closer = Closer.create()
        val dexBuilder = DexArchiveBuilder.createD8DexBuilder(
            DexParameters(
                minSdkVersion = android.defaultConfig.minSdkVersion!!.apiLevel,
                debuggable = false,
                dexPerClass = false,
                withDesugaring = true,
                desugarBootclasspath = ClassFileProviderFactory(android.bootClasspath.map(File::toPath))
                    .also { closer.register(it) },
                desugarClasspath = ClassFileProviderFactory(mutableListOf())
                    .also { closer.register(it) },
                coreLibDesugarConfig = null,
                enableApiModeling = false,
                messageReceiver = MessageReceiverImpl(
                    SyncOptions.ErrorFormatMode.HUMAN_READABLE,
                    LoggerFactory.getLogger("buildDexRelease")
                )
            )
        )

        val files: Stream<ClassFileEntry> = inputs.files.files.stream().map {
            val bytes = it.readBytes()
            MemoryClassFileEntry(it.name, bytes.size.toLong(), bytes)
        }

        dexBuilder.convert(
            files,
            outputs.files.singleFile.toPath(),
            null
        )
    }
}

afterEvaluate {
    tasks.named<BundleAar>("bundleReleaseAar") {
        val dexTask = tasks.named("buildDexRelease").get()
        dependsOn(dexTask)
        from(dexTask.outputs.files.singleFile)
    }

    publishing {
        publications {
            register(project.name, MavenPublication::class.java) {
                group = "com.aliucord"
                artifactId = "Aliuhook"
                version = "1.1.4"

                from(components["release"])
            }

            repositories {
                val username = System.getenv("MAVEN_USERNAME")
                val password = System.getenv("MAVEN_PASSWORD")
                val releaseUsername = System.getenv("MAVEN_RELEASE_USERNAME")
                val releasePassword = System.getenv("MAVEN_RELEASE_PASSWORD")

                if (releaseUsername != null && releasePassword != null) {
                    maven {
                        setUrl("https://maven.aliucord.com/releases")
                        credentials {
                            this.username = releaseUsername
                            this.password = releasePassword
                        }
                    }
                }

                // Publish to snapshots repo for backwards compatibility
                if (username != null && password != null) {
                    maven {
                        setUrl("https://maven.aliucord.com/snapshots")
                        credentials {
                            this.username = username
                            this.password = password
                        }
                    }
                }
            }
        }
    }
}

class MemoryClassFileEntry(
    private val name: String,
    private val size: Long,
    private val bytes: ByteArray
) : ClassFileEntry {
    override fun name() = name
    override fun getSize() = size
    override fun getRelativePath() = ""
    override fun readAllBytes() = bytes
    override fun getInput() = object : ClassFileInput {
        override fun close() {}
        override fun entries(filter: BiPredicate<Path, String>?) = Stream.empty<ClassFileEntry>()
        override fun getPath() = Paths.get("")
    }

    override fun readAllBytes(bytes: ByteArray?): Int {
        bytes ?: return 0
        this.bytes.copyInto(bytes, 0, 0, this.bytes.lastIndex)
        return this.bytes.size
    }
}
