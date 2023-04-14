import com.android.build.gradle.internal.errors.MessageReceiverImpl
import com.android.build.gradle.options.SyncOptions
import com.android.build.gradle.tasks.BundleAar
import com.android.builder.dexing.ClassFileEntry
import com.android.builder.dexing.ClassFileInput
import com.android.builder.dexing.DexArchiveBuilder
import com.android.builder.dexing.DexParameters
import com.android.builder.dexing.r8.ClassFileProviderFactory
import com.google.common.io.Closer
import org.gradle.kotlin.dsl.support.listFilesOrdered
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
    implementation("org.lsposed.lsplant:lsplant:5.2")
    implementation("io.github.vvb2060.ndk:dobby:1.2")

    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test:runner:1.4.0")
}

android {
    compileSdk = 33
    buildToolsVersion = "32.0.0"
    ndkVersion = sdkDirectory.resolve("ndk").listFilesOrdered().last().name

    buildFeatures {
        buildConfig = false
        prefab = true
    }

    defaultConfig {
        minSdk = 21
        targetSdk = 33
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

// fuck you agp
tasks.register("buildDexRelease") {
    outputs.dir(buildDir.resolve("intermediates/dex/"))

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
                coreLibDesugarOutputKeepRuleFile = null,
                messageReceiver = MessageReceiverImpl(
                    SyncOptions.ErrorFormatMode.HUMAN_READABLE,
                    LoggerFactory.getLogger("buildDexRelease")
                )
            )
        )

        val files = inputs.files.files.map {
            val bytes = it.readBytes()
            MemoryClassFileEntry(it.name, bytes.size.toLong(), bytes) as ClassFileEntry
        }

        dexBuilder.convert(
            files.stream(),
            outputs.files.singleFile.toPath()
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
