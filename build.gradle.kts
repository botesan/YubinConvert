import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests

buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("com.github.ben-manes:gradle-versions-plugin:0.52.0")
    }
}

apply(plugin = "com.github.ben-manes.versions")

plugins {
    kotlin("multiplatform") version "2.1.20"
}

group = "jp.mito.yconvert"
version = "1.2.1"

repositories {
    mavenCentral()
}

kotlin {
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

    fun getGenerateProgramNameSourcePath(target: KotlinNativeTarget): File {
        val buildDirectory = project.layout.buildDirectory.get().asFile
        val generateProgramNameSourceBaseDir = buildDirectory.resolve(relative = "generated/src")
        val sourceFilePath = "${target.name}Main/kotlin/ProgramName.kt"
        return generateProgramNameSourceBaseDir.resolve(relative = sourceFilePath)
    }

    fun createGenerateProgramNameSourceTask(target: KotlinNativeTarget, baseName: String) {
        val targetNameCapitalized = target.name.replaceFirstChar { if (it.isLowerCase()) it.titlecaseChar() else it }
        val createTaskName = "generateProgramNameSource$targetNameCapitalized"
        val compileTaskName = "compileKotlin$targetNameCapitalized"
        val compileTask = tasks.findByName(compileTaskName)
        if (compileTask != null && tasks.findByName(createTaskName) == null) {
            val outputSourceFile = getGenerateProgramNameSourcePath(target)
            val exeSuffix = target.konanTarget.family.exeSuffix
            val programName = "$baseName.$exeSuffix"
            tasks.register<Task>(name = createTaskName) {
                compileTask.dependsOn(this)
                doLast {
                    outputSourceFile.parentFile.mkdirs()
                    outputSourceFile.writeText(
                        """
                        |package main
                        |actual val PROGRAM_NAME: String = "$programName"
                        |""".trimMargin()
                    )
                }
            }
        }
    }

    fun createNativeConfigure(
        configure: KotlinNativeTargetWithHostTests.() -> Unit = {}
    ): KotlinNativeTargetWithHostTests.() -> Unit = {
        compilations["main"].cinterops {
            val sqlite3 by creating { includeDirs(project.file("sqlite-amalgamation/source")) }
            val zopfli by creating { includeDirs(project.file("zopfli/src/zopfli")) }
        }
        binaries {
            executable {
                entryPoint("main.main")
                createGenerateProgramNameSourceTask(target, baseName)
            }
        }
        configure()
    }

    val mingwX64 = mingwX64(configure = createNativeConfigure())
    val linuxX64 = linuxX64(configure = createNativeConfigure())
    //macosX64(configure =  createNativeConfigure())

    sourceSets {
        val ktorVersion = "3.1.2"
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
                implementation("com.soywiz.korlibs.korio:korio:4.0.10")
                implementation("io.ktor:ktor-client-core:$ktorVersion")
            }
        }
        val mingwX64Main by getting {
            kotlin.srcDirs(getGenerateProgramNameSourcePath(target = mingwX64).parentFile)
            dependencies {
                implementation("io.ktor:ktor-client-winhttp:$ktorVersion")
            }
        }
        val linuxX64Main by getting {
            kotlin.srcDirs(getGenerateProgramNameSourcePath(target = linuxX64).parentFile)
            dependencies {
                // ktor & curl が windows ホストでビルドエラー（リンクエラー？）
                implementation("io.ktor:ktor-client-curl:$ktorVersion")
                // ktor & cio は https 通信ができない
                //implementation("io.ktor:ktor-client-cio:$ktorVersion")
            }
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
