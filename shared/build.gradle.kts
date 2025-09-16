
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


//val jniSourceDir = project.file("src/desktopMain/jni")
val jniSourceDir = project.file("native")
val cmakeBuildDir = project.layout.buildDirectory.dir(".cxx/$platformIdentifier") // Temporary CMake build dir, platform specific
//val finalDesktopNativeLibsDir = project.file("native/desktop/$platformIdentifier")
val finalDesktopNativeLibsDir = project.file("native")
//val runtimeNativeLibsDir = project.layout.buildDirectory.dir(cmakeOutputSubDirName).get().asFile

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

        // Copy the built library to the final destination
        finalDesktopNativeLibsDir.mkdirs() // Ensure final directory exists
        val builtLibName = System.mapLibraryName("tinyexpr_jni") // From your CMakeLists.txt: add_library(tinyexpr_jni ...)
        // This will generate libtinyexpr_jni.so, tinyexpr_jni.dll, etc.

        var foundLib: File? = null
        cmakeBuildDir.get().asFile.walkTopDown().forEach { file ->
            if (file.isFile) {
                // For Windows, CMake with MinGW Makefiles might produce "lib<name>.dll" or "<name>.dll"
                // For Unix, it's usually "lib<name>.so" or "lib<name>.dylib"
                val targetNameWithoutLibPrefix = builtLibName.removePrefix("lib") // e.g. tinyexpr_jni.dll
                if (file.name == builtLibName || file.name == targetNameWithoutLibPrefix) {
                    foundLib = file
                    return@forEach
                }
            }
        }

        if (foundLib != null && foundLib!!.exists()) {
            val destinationFile = File(finalDesktopNativeLibsDir, foundLib!!.name.removePrefix("lib")) // Remove "lib" prefix for final name if desired, especially for DLLs

            project.copy {
                from(foundLib)
                into(finalDesktopNativeLibsDir)
                // Standardize the name by removing "lib" prefix, which is common for DLLs.
                // For .so and .dylib, keeping "lib" is standard.
                rename { fileName ->
                    if (currentOs.isWindows && fileName.startsWith("lib") && fileName.endsWith(".dll")) {
                        fileName.substring(3)
                    } else {
                        fileName
                    }
                }
            }
            var finalCopiedName = foundLib!!.name
            if (currentOs.isWindows && foundLib!!.name.startsWith("lib") && foundLib!!.name.endsWith(".dll")) {
                finalCopiedName = foundLib!!.name.substring(3)
            }
            println("JNI library '${destinationFile.name}' for Desktop JVM ($platformIdentifier) copied to ${finalDesktopNativeLibsDir.path}")
        } else {
            logger.error("Failed to find built JNI library '$builtLibName' (or without 'lib' prefix) in ${cmakeBuildDir.get().asFile.path}")
            // Consider throwing an exception if the library is crucial
            // throw GradleException("JNI library build failed: output not found.")
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