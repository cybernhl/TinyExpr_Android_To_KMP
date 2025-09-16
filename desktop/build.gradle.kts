import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrains.compose.hot.reload)
}

group = "com.live.lang.anchor"
version = "1.0-SNAPSHOT"
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
    sourceSets {
        val main by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
    }
}

tasks.withType<JavaExec> {//FIXME
    val libPath =   "../shared/native"
    systemProperty("java.library.path", libPath)
}

project.afterEvaluate {
    tasks.findByName("run")?.let { runTask ->
        println("Task :desktop:run depends on:")
        runTask.taskDependencies.getDependencies(runTask).forEach { dependency ->
            println("- ${dependency.path}")
        }
    }
}

//https://medium.com/@makeevrserg/compose-desktop-shadowjar-1cba3aba9a58
compose.desktop {
    application {
        mainClass = "org.adman.kmp.tiny.expr.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "org.adman.kmp.tiny.expr"
            packageVersion = "1.0.0"
        }
    }
}
