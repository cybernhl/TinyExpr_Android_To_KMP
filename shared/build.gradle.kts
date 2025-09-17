import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.konan.target.KonanTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.compose.hot.reload)
}

group = "org.adman.kmp.tiny.expr"
version = "1.0-SNAPSHOT"
val androidHome = System.getenv("ANDROID_HOME")
val cmakeVersion = "3.22.1" // Or read from a property if it changes often

val cmakeExecutable = if (androidHome != null && androidHome.isNotBlank()) {
    // Construct the path using File objects for better path handling (joins, etc.)
    // and ensure the path is valid.
    val cmakeBinDir = File(File(androidHome, "cmake"), cmakeVersion).resolve("bin")
    val cmakeFile = File(cmakeBinDir, "cmake")
    if (cmakeFile.exists() && cmakeFile.canExecute()) {
        project.logger.lifecycle("Using CMake from ANDROID_HOME: ${cmakeFile.absolutePath}")
        cmakeFile.absolutePath
    } else {
        project.logger.warn("CMake not found or not executable at ${cmakeFile.absolutePath}. Falling back to 'cmake' in PATH.")
        "cmake" // Fallback to hoping 'cmake' is in PATH if specific one isn't found
    }
} else {
    project.logger.warn("ANDROID_HOME environment variable not set or empty. Falling back to 'cmake' in PATH.")
    "cmake" // Fallback if ANDROID_HOME is not set
}

kotlin {
    jvm("desktop")
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    val iOSTargets = listOf(
        iosX64("iosX64"), // for simulator on Intel Macs
        iosArm64("iosArm64"), // for real devices
        iosSimulatorArm64("iosSimulatorArm64") // for simulator on Apple Silicon Macs
    )

    val topLevelCMakeListsDir = project.file("native")//FIXME can not use Top
    // 指向包含 CMakeLists.txt 的目錄
    val cmkelistfolder = project.file("src/iosMain/native")
    // CMake 建置的輸出目錄
    val cLibBuildDir = layout.buildDirectory.dir(".cxx")

    iOSTargets.forEach { iosTarget ->
        // 1. ===== 為每個 iOS 架構建立執行 CMake 的 Gradle Task 來執行 CMake 為每個 iOS 架構編譯 *.a =====
        val buildCLibTaskName = "buildTinyexprFor${iosTarget.name.capitalize()}"
        val buildCLibTaskProvider = tasks.register<Exec>(buildCLibTaskName) {
            group = "C Library"
            description = "Builds the tinyexpr C static library for ${iosTarget.name}"

            val targetBuildDir = cLibBuildDir.get().dir(iosTarget.name)
            workingDir = targetBuildDir.asFile

            inputs.dir(project.file("src"))
            inputs.dir(project.file("native"))
            outputs.file(targetBuildDir.file("lib/libtinyexpr.a"))

            doFirst {
                targetBuildDir.asFile.deleteRecursively()
                targetBuildDir.asFile.mkdirs()
            }

            val sdkPathProvider = providers.exec { // Ensure this provider is correctly defined as before
                commandLine("xcrun", "--sdk", iosTarget.sdkName().toLowerCase(), "--show-sdk-path")
            }

            // 【為 Clang 編譯器提供必要的旗標來解決 "compiler is broken" 問題
            // --- Correctly map KonanTarget architecture to Clang -arch flag ---
            val clangArch = when (iosTarget.konanTarget) {
                org.jetbrains.kotlin.konan.target.KonanTarget.IOS_ARM64 -> "arm64"
                org.jetbrains.kotlin.konan.target.KonanTarget.IOS_X64 -> "x86_64"
                org.jetbrains.kotlin.konan.target.KonanTarget.IOS_SIMULATOR_ARM64 -> "arm64"
                else -> error("Unsupported iOS target for CMake build: ${iosTarget.konanTarget}")
            }

            val sdkPath = sdkPathProvider.standardOutput.asText.get().trim()
            val arch = iosTarget.konanTarget.architecture.name

// Ensure miphoneos-version-min matches your project's deployment target
            val cFlagsForCMake = "-miphoneos-version-min=13.0" // Or any other general flags you need, but avoid -arch and -isysroot

            commandLine(
                cmakeExecutable,
                cmkelistfolder.absolutePath,
                "-G", "Ninja",
                "-DCMAKE_BUILD_TYPE=Release",

                // Set fundamental toolchain variables for CMake
                "-DCMAKE_SYSTEM_NAME=iOS",
                "-DCMAKE_OSX_ARCHITECTURES=${clangArch}",
                "-DCMAKE_OSX_DEPLOYMENT_TARGET=13.0", // This should set -miphoneos-version-min

                // --- Explicitly provide the SDK path to CMake ---
                "-DCMAKE_OSX_SYSROOT=${sdkPath}",

                // Pass other C flags if necessary.
                // If CMAKE_OSX_DEPLOYMENT_TARGET correctly sets the min version,
                // CMAKE_C_FLAGS might not need anything specific here, or only very specific flags.
                "-DCMAKE_C_FLAGS=${cFlagsForCMake}"
            )

            doLast {
                exec {
                    workingDir = targetBuildDir.asFile
                    commandLine(cmakeExecutable, "--build", ".")
                }
            }
        }
        // 2. ===== 設定 C-Interop (包含連結器選項和任務依賴) =====
        val cinterop = iosTarget.compilations.getByName("main").cinterops.create("tinyexpr") {

            defFile(project.file("src/iosMain/cinterop/tinyexpr.def"))

            compilerOpts("-I${project.projectDir}/native/include")
        }
        // 將 cinterop 任務設定為依賴 CMake 任務
        tasks.named(cinterop.interopProcessingTaskName).configure(object : Action<Task> {
            override fun execute(cinteropTask: Task) {
                cinteropTask.dependsOn(buildCLibTaskProvider)
            }
        })
        // 3. ===== 設定最終 Framework 的連結 =====
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = false

            // 取得對應此 target 的 C 函式庫建置目錄
            val cLibOutputDir = cLibBuildDir.get().dir(iosTarget.name).dir("lib")

            // 將 CMake Task 作為 linkTask 的依賴，確保 .a 檔案先被編譯好
//            linkTaskProvider.dependsOn(buildCLibTaskProvider)
            linkTaskProvider.configure(object : Action<org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink> {
                override fun execute(linkTask: org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink) {
                    linkTask.dependsOn(buildCLibTaskProvider)
                }
            })

            // 在 framework 區塊內，linkerOpts 是一個函式
            // 將我們的 .a 函式庫路徑和名稱傳遞給最終的連結器
            linkerOpts(
                "-L${cLibOutputDir.asFile.absolutePath}",
                "-ltinyexpr"
            )
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.ui)
            implementation(compose.runtime)
            implementation(compose.material)
            implementation(compose.material3)
            implementation(compose.components.resources)
        }

        //Ref : https://www.jetbrains.com/help/kotlin-multiplatform-dev/multiplatform-run-tests.html#work-with-more-complex-projects
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        val desktopMain by getting

        desktopMain.dependencies {
            api(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }

        androidMain.dependencies {
        }

        androidUnitTest.dependencies {
            implementation(libs.junit)
        }

        androidInstrumentedTest.dependencies {
            implementation(libs.androidx.junit)
            implementation(libs.androidx.espresso.core)
        }
    }
}

// 輔助函數，用於獲取 iOS SDK 名稱
fun org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget.sdkName(): String {
    return when (konanTarget) {
        KonanTarget.IOS_X64, KonanTarget.IOS_SIMULATOR_ARM64 -> "iphonesimulator"
        KonanTarget.IOS_ARM64 -> "iphoneos"
        else -> "unknown"
    }
}

android {
    namespace = "org.adman.kmp.tiny.shared"
    compileSdk = 36
    buildFeatures {
        compose = true
    }
    ndkVersion = "28.0.13004108"
    defaultConfig {
        minSdk = 21
        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
//        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    externalNativeBuild {
        cmake {
//            path = file("./src/androidMain/jni/CMakeLists.txt")
            path = file("native/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
}

compose.resources {
    publicResClass = true
    packageOfResClass = "org.adman.kmp.tiny.expr"
    generateResClass = always
}

val currentOs = OperatingSystem.current()
val platformIdentifier = when {
    currentOs.isWindows -> "windows"
    currentOs.isMacOsX -> "osx"
    currentOs.isLinux -> "linux"
    else -> "unknown"
}

val jniSourceDir = project.file("native")
val cmakeBuildDir =
    project.layout.buildDirectory.dir(".cxx/$platformIdentifier") // Temporary CMake build dir, platform specific
val finalDesktopNativeLibsDir = project.file("native")

val compileDesktopJniLib = tasks.register<Exec>("compileDesktopJniLib") {
    group = "build"
    description = "Builds the JNI library for Desktop JVM on ${currentOs.name}."

    inputs.dir(jniSourceDir) // CMakeLists.txt and JNI sources
    outputs.dir(finalDesktopNativeLibsDir) // Where the final .dll/.so/.dylib will be

    workingDir = jniSourceDir // Run CMake from the directory containing CMakeLists.txt

    val cmakeGenerator = if (currentOs.isWindows) "MinGW Makefiles" else "Unix Makefiles"
    val makeCommand = if (currentOs.isWindows) "mingw32-make" else "make"
    val cmakeBuildPath = cmakeBuildDir.get().asFile.relativeTo(jniSourceDir).path
    commandLine(
        "cmake",
        "-S", ".",                                 // Source directory (current workingDir)
        "-B", cmakeBuildPath,                      // Build directory (relative to workingDir)
        "-G", cmakeGenerator
        // Add any other necessary CMake definitions, e.g., toolchain for cross-compilation if needed
        // "-DCMAKE_TOOLCHAIN_FILE=..."
    )

    doLast { // Separate execution for the build command
        // Ensure the temporary build directory exists
        cmakeBuildDir.get().asFile.mkdirs()

        exec {
            workingDir =
                cmakeBuildDir.get().asFile // Change workingDir to the CMake build directory
            commandLine(makeCommand, "VERBOSE=1") // Or just 'makeCommand'
            // If you have specific targets in CMakeLists.txt:
            // commandLine(makeCommand, "your_cmake_target_name")
        }
        finalDesktopNativeLibsDir.mkdirs()
        val sourceBuildDir = cmakeBuildDir.get().asFile
        val targetOutputDir = finalDesktopNativeLibsDir

        val filesToRenameInSource = mutableListOf<Pair<File, File>>()
        sourceBuildDir.walkTopDown().forEach { file ->
            if (file.isFile && file.name.startsWith("lib") && file.name.endsWith(".dll")) {
                val newName = file.name.substring(3)
                val newFile = File(file.parentFile, newName)
                filesToRenameInSource.add(Pair(file, newFile))
            }
        }

        filesToRenameInSource.forEach { (originalFile, newFile) ->
            originalFile.renameTo(newFile)
        }
        var actualContentRoot: File? = null

        val searchQueue = ArrayDeque<File>()
        if (sourceBuildDir.isDirectory) {
            searchQueue.add(sourceBuildDir)
        }

        while (searchQueue.isNotEmpty() && actualContentRoot == null) {
            val currentDir = searchQueue.removeFirst()
            var foundLibInCurrentDir = false
            currentDir.listFiles()?.forEach { entry ->
                if (entry.isFile &&
                    (entry.name.endsWith(".dll") || entry.name.endsWith(".so") || entry.name.endsWith(
                        ".dylib"
                    ))
                ) {
                    foundLibInCurrentDir = true
                }
            }

            if (foundLibInCurrentDir) {
                actualContentRoot = currentDir
            } else {
                currentDir.listFiles()?.filter { it.isDirectory }?.forEach { subDir ->
                    searchQueue.add(subDir)
                }
            }
        }
        if (actualContentRoot != null && actualContentRoot!!.exists() && actualContentRoot!!.isDirectory) {
            project.copy {
                from(project.fileTree(actualContentRoot!!) {
                    include(
                        "**/*.dll",
                        "**/*.so",
                        "**/*.dylib"
                    )
                    exclude(
                        "**/CMakeFiles/**",        // 排除所有名為 CMakeFiles 的目錄及其內容
                        "**/CMakeFiles.*/**",     // 排除所有名為 CMakeFiles.something.dir 的目錄及其內容
                        "**/*.cmake",             // 通常也不需要複製 .cmake 檔案
                        "**/Makefile",            // 通常也不需要複製 Makefile
                        "**/cmake_install.cmake"  // 通常也不需要這個
                        // 你可以根據 CMake 產生的其他不需要的中繼檔案/目錄添加更多排除規則
                    )
                })
                into(targetOutputDir)
            }
        } else {

        }
    }
}

kotlin.targets.getByName("desktop") {
    compilations.getByName("main") {
        compileTaskProvider.configure {
            dependsOn(compileDesktopJniLib)
        }
    }
    project.tasks.named("${this.name}Jar") {
        dependsOn(compileDesktopJniLib)
    }
}