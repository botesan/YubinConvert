import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.github.ben-manes:gradle-versions-plugin:0.42.0")
    }
}

apply(plugin = "com.github.ben-manes.versions")

plugins {
    kotlin("multiplatform") version "1.7.0"
}

group = "jp.mito.yconvert"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    /** SQLite3のバージョン */
    val sqlite3Version = "3380500"

    /*
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }
    */

    fun createNativeConfigure(
        configure: KotlinNativeTargetWithHostTests.() -> Unit = {}
    ): KotlinNativeTargetWithHostTests.() -> Unit = {
        @Suppress("UNUSED_VARIABLE")
        compilations["main"].cinterops {
            val sqlite3 by creating {
                includeDirs(project.file("src/sqlite-amalgamation-$sqlite3Version"))
            }
        }
        binaries {
            all {
                binaryOption("memoryModel", "experimental")
            }
            executable {
                entryPoint("main.main")
            }
        }
        configure()
    }

    mingwX64(configure = createNativeConfigure())
    linuxX64(configure = createNativeConfigure())
    //macosX64(configure =  createNativeConfigure())

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.soywiz.korlibs.korio:korio:2.7.0")
            }
        }
        val mingwX64Main by getting {
            kotlin.srcDir(project.file("src/nativeMain/kotlin"))
        }
        val linuxX64Main by getting {
            kotlin.srcDir(project.file("src/nativeMain/kotlin"))
        }
        //val macosX64Main by getting
    }
}

tasks.named<DependencyUpdatesTask>(name = "dependencyUpdates") {
    resolutionStrategy {
        componentSelection {
            all {
                val rejected = arrayOf("alpha", "beta", "rc", "cr", "m", "preview", "b", "ea", "eap")
                    .map { "(?i).*[.-]$it[.\\d-+]*[.\\d\\w-+]*".toRegex() }
                    .any { candidate.version.matches(it) }
                if (rejected) {
                    reject("Release candidate")
                }
            }
        }
    }
}
