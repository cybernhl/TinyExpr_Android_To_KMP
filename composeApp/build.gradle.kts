import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
            baseName = "ComposeApp"
            /** Because we are linking our library inside the composeApp, we can not
             * use a static library. You can avoid this by linking in the def file.
             **/
            isStatic = false
            // For linking our library. You can specify this on def file also
            linkerOpts("-L${rootDir}/composeApp/native/ios","-ltinyexpr_ios_sim")
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