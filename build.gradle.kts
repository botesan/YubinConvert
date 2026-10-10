import com.codingfeline.buildkonfig.compiler.FieldSpec
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.github.benmanes.gradle.versions.updates.resolutionstrategy.ComponentSelectionRulesWithCurrent
import com.github.benmanes.gradle.versions.updates.resolutionstrategy.ComponentSelectionWithCurrent
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests
import org.jetbrains.kotlin.konan.target.Family
import java.util.*

buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("com.github.ben-manes:gradle-versions-plugin:0.54.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.20")
        classpath("com.codingfeline.buildkonfig:buildkonfig-gradle-plugin:0.23.0")
    }
}

@Suppress("AvoidApplyPluginMethod")
apply(plugin = "com.github.ben-manes.versions")

plugins {
    kotlin("multiplatform") version "2.4.20"
    id("com.codingfeline.buildkonfig") version "0.23.0"
}

group = "jp.mito.yconvert"
version = "1.5.0"

repositories {
    mavenCentral()
}

enum class HostOs {
    MacOS, Linux, Windows, ;

    val family: Family
        get() = when (this) {
            MacOS -> Family.OSX
            Linux -> Family.LINUX
            Windows -> Family.MINGW
        }
}

val hostOs = System.getProperty("os.name").let { os ->
    when {
        os == "Mac OS X" -> HostOs.MacOS
        os == "Linux" -> HostOs.Linux
        os.startsWith(prefix = "Windows") -> HostOs.Windows
        else -> throw GradleException("Host OS($os) is not supported in Kotlin/Native.")
    }
}
if (hostOs == HostOs.MacOS) {
    throw GradleException("MacOS is not supported in this project.")
}

kotlin {
    fun createNativeConfigure(
        configure: KotlinNativeTargetWithHostTests.() -> Unit = {}
    ): KotlinNativeTargetWithHostTests.() -> Unit = {
        compilations["main"].cinterops {
            create("sqlite3") { includeDirs(project.file("sqlite-amalgamation")) }
            create("zopfli") { includeDirs(project.file("zopfli/src/zopfli")) }
            create("zenzop-ffi")
        }
        binaries {
            executable {
                entryPoint("main.main")
            }
        }
        configure()
    }

    val mingwX64 = mingwX64(configure = createNativeConfigure())
    val linuxX64 = linuxX64(configure = createNativeConfigure())
    val nativeTarget = when (hostOs) {
        HostOs.MacOS -> throw GradleException("MacOS is not supported in this project.")
        HostOs.Linux -> linuxX64
        HostOs.Windows -> mingwX64
    }
    val nativeTargetCapitalizeName = nativeTarget.name.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }

    val rustTarget = when (hostOs) {
        HostOs.Windows -> "x86_64-pc-windows-gnullvm"
        else -> ""
    }

    // TODO: キャッシュを効かせる（インストール済みか確認する方法をさがす）
    val rustTargetInstall = tasks.register<Exec>(name = "rustTargetInstall") {
        group = "build"
        description = "Install Rust target"
        if (rustTarget.isEmpty()) {
            enabled = false
        } else {
            commandLine("rustup", "target", "add", rustTarget)
        }
    }

    val cargoZenzopFfiBuild = tasks.register<Exec>(name = "cargoZenzopFfiBuild") {
        dependsOn(rustTargetInstall)
        group = "build"
        description = "Build Rust project using Cargo"
        workingDir = file(path = "zenzop-ffi")
        inputs.file(file(path = "zenzop-ffi/Cargo.toml"))
        inputs.dir(file(path = "zenzop-ffi/src"))
        outputs.dir(file(path = "zenzop-ffi/target/$rustTarget/release"))
        commandLine(
            listOf(
                "cargo",
                "build",
                "--release",
            ) + listOfNotNull(
                if (rustTarget.isNotEmpty()) {
                    listOf("--target", rustTarget)
                } else {
                    null
                }
            ).flatten()
        )
    }

    tasks.getByName<Task>(name = "cinteropZenzop-ffi$nativeTargetCapitalizeName") {
        dependsOn(cargoZenzopFfiBuild)
    }

    // ホストOSに応じてビルド対象を限定する
    // TODO: 本当は GradleException を投げるか HostOs でビルド対象を限定した方がいいと思う
    tasks.forEach { task ->
        if (task.name.startsWith(prefix = "cinterop") &&
            task.name.endsWith(suffix = nativeTarget.name, ignoreCase = true).not()
        ) {
            logger.info("task.name=${task.name} disabled, nativeTarget.name=${nativeTarget.name}, hostOs.family.name=${hostOs.family.name}")
            task.enabled = false
        } else if (task.name.startsWith(prefix = "compile") &&
            task.name.endsWith(suffix = "kotlinMetadata", ignoreCase = true).not() &&
            task.name.endsWith(suffix = "kotlin${nativeTarget.name}", ignoreCase = true).not()
        ) {
            logger.info("task.name=${task.name} disabled, nativeTarget.name=${nativeTarget.name}, hostOs.family.name=${hostOs.family.name}")
            task.enabled = false
        }
    }

    sourceSets {
        val ktorVersion = "3.6.0"
        getByName("commonMain") {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0-0.6.x-compat")
                implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.8.0")
                implementation("org.kotlincrypto.hash:md")
                implementation("io.ktor:ktor-client-core:$ktorVersion")
            }
        }
        getByName("mingwX64Main") {
            dependencies {
                implementation("io.ktor:ktor-client-winhttp:$ktorVersion")
            }
        }
        getByName("linuxX64Main") {
            dependencies {
                // ktor & curl が windows ホストでビルドエラー（リンクエラー？）
                implementation("io.ktor:ktor-client-curl:$ktorVersion")
                // ktor & cio は https 通信ができない
                //implementation("io.ktor:ktor-client-cio:$ktorVersion")
            }
        }
    }
}

tasks.getByName<Task>(name = "clean") {
    doFirst {
        delete(file(path = "zenzop-ffi/target"))
    }
}

dependencies {
    commonMainImplementation(platform(notation = "org.kotlincrypto.hash:bom:0.8.0"))
}

buildkonfig {
    packageName = group.toString()
    objectName = "BuildKonfig"
    defaultConfigs {
        buildConfigField(
            type = FieldSpec.Type.STRING,
            name = "PROGRAM_NAME",
            value = rootProject.name,
        )
        buildConfigField(
            type = FieldSpec.Type.STRING,
            name = "VERSION",
            value = "$version",
        )
        buildConfigField(
            type = FieldSpec.Type.STRING,
            name = "PROGRAM_COMMAND",
            value = "${rootProject.name}.${hostOs.family.exeSuffix}",
        )
    }
}

tasks.named<DependencyUpdatesTask>(name = "dependencyUpdates") {
    resolutionStrategy {
        componentSelection(Action<ComponentSelectionRulesWithCurrent> {
            all(Action<ComponentSelectionWithCurrent> {
                val rejected = arrayOf("alpha", "beta", "rc", "cr", "m", "preview", "b", "ea", "eap")
                    .map { "(?i).*[.-]$it[.\\d-+]*[.\\d\\w-+]*".toRegex() }
                    .any { candidate.version.matches(it) }
                if (rejected) {
                    reject("Release candidate")
                }
            })
        })
    }
}
