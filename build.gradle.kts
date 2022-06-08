import org.jetbrains.kotlin.konan.target.presetName

plugins {
    kotlin("multiplatform") version "1.6.21"
}

group = "jp.mito.yconvert"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }
    nativeTarget.apply {
        compilations.getByName("main") {
            cinterops {
                val stat by creating {
                    defFile(project.file("src/nativeInterop/cinterop/files/stat.def"))
                }
                val sqlite3 by creating {
                    val dir = project.file("src/nativeInterop/cinterop/sqlite-amalgamation-3380500")
                    defFile(dir.resolve("sqlite3.def"))
                    compilerOpts("-I$dir")
                    includeDirs.allHeaders(dir)
                }
            }
        }
        binaries {
            all {
                binaryOptions["memoryModel"] = "experimental"
            }
            executable {
                entryPoint = "main"
            }
        }
    }
    sourceSets {
        val nativeMain by getting
        val nativeTest by getting
    }
}
