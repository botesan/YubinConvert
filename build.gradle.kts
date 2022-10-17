import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.gradle.configurationcache.extensions.capitalized
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
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
    kotlin("multiplatform") version "1.7.20"
}

group = "jp.mito.yconvert"
version = "1.1.0"

repositories {
    mavenCentral()
}

kotlin {
    /** SQLite3のバージョン */
    val sqlite3Version = "3390400"

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
        val generateProgramNameSourceBaseDir = project.buildDir.resolve(relative = "generated/src")
        val sourceFilePath = "${target.name}Main/kotlin/ProgramName.kt"
        return generateProgramNameSourceBaseDir.resolve(relative = sourceFilePath)
    }

    fun createGenerateProgramNameSourceTask(target: KotlinNativeTarget, baseName: String) {
        val targetNameCapitalized = target.name.capitalized()
        val createTaskName = "generateProgramNameSource$targetNameCapitalized"
        val compileTaskName = "compileKotlin$targetNameCapitalized"
        val compileTask = tasks.findByName(compileTaskName)
        if (compileTask != null && tasks.findByName(createTaskName) == null) {
            val outputSourceFile = getGenerateProgramNameSourcePath(target)
            val exeSuffix = target.konanTarget.family.exeSuffix
            val programName = "$baseName.$exeSuffix"
            tasks.create(name = createTaskName) {
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
        @Suppress("UNUSED_VARIABLE")
        compilations["main"].cinterops {
            val sqlite3 by creating {
                includeDirs(project.file("sqlite-amalgamation-$sqlite3Version"))
            }

            @Suppress("SpellCheckingInspection")
            val zopfli by creating {
                includeDirs(project.file("zopfli/src/zopfli"))
            }
        }
        binaries {
            all {
                binaryOption("memoryModel", "experimental")
            }
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

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib:1.7.20")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
                implementation("com.soywiz.korlibs.korio:korio:3.2.0")
            }
        }
        val mingwX64Main by getting {
            kotlin.srcDirs(
                project.file("src/nativeMain/kotlin"),
                getGenerateProgramNameSourcePath(target = mingwX64).parentFile,
            )
        }
        val linuxX64Main by getting {
            kotlin.srcDirs(
                project.file("src/nativeMain/kotlin"),
                getGenerateProgramNameSourcePath(target = linuxX64).parentFile,
            )
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
