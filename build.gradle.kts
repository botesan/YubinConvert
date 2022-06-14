import org.jetbrains.kotlin.konan.target.Family

plugins {
    kotlin("multiplatform") version "1.7.0"
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
            @Suppress("UNUSED_VARIABLE")
            cinterops {
                val sqlite3 by creating {
                    val dir = project.file("src/nativeInterop/cinterop/sqlite-amalgamation-3380500")
                    defFile(dir.resolve("sqlite3.def"))
                    compilerOpts("-I$dir")
                }
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
    }
    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.soywiz.korlibs.korio:korio:2.7.0")
            }
        }
        val nativeMain by getting
        val nativeTest by getting
        when (val family = nativeTarget.konanTarget.family) {
            Family.MINGW -> {
                val mingwMain by creating {
                    nativeMain.dependsOn(this)
                    dependsOn(commonMain)
                }
                val mingwTest by creating {
                    nativeTest.dependsOn(this)
                }
            }
            Family.LINUX -> {
                val linuxMain by creating {
                    nativeMain.dependsOn(this)
                    dependsOn(commonMain)
                }
                val linuxTest by creating {
                    nativeTest.dependsOn(this)
                }
            }
            else ->
                throw GradleException("'$family' is not supported.")
        }
    }
}
