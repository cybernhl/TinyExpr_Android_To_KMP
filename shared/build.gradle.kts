
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.gradle.internal.os.OperatingSystem // Import OperatingSystem

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.compose.hot.reload)
}

group = "org.adman.kmp.tiny.expr"
version = "1.0-SNAPSHOT"

kotlin {
    jvm("desktop")
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            /** Because we are linking our library inside the shared, we can not
             * use a static library. You can avoid this by linking in the def file.
             **/
            isStatic = false
            // For linking our library. You can specify this on def file also
            linkerOpts("-L${rootDir}/shared/native/ios","-ltinyexpr_ios_sim")
        }

        iosTarget.compilations["main"].cinterops.create("tinyexpr"){
            definitionFile = file("nativeInterop/cinterop/tinyexpr.def")
            // Header dir
            includeDirs("native/include")
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
            version = "3.18.1"
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
val cmakeBuildDir = project.layout.buildDirectory.dir(".cxx/$platformIdentifier") // Temporary CMake build dir, platform specific
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
            workingDir = cmakeBuildDir.get().asFile // Change workingDir to the CMake build directory
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
                    (entry.name.endsWith(".dll") || entry.name.endsWith(".so") || entry.name.endsWith(".dylib"))
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