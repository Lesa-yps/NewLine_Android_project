// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    id("com.github.johnrengelman.shadow") version "7.1.2"
    alias(libs.plugins.kotlin.compose) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
    id("nl.littlerobots.version-catalog-update") version "0.8.1"
}

buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
    dependencies {
        classpath(libs.google.services)
    }
}

val jarModules = listOf(
    ":app:data",
    ":app:logic",
    ":app:models",
    ":app:app_ui",
    ":app:facade"
)

tasks.register<Copy>("copyJars") {
    jarModules.forEach { module ->
        dependsOn("$module:jar")
        from(project(module).layout.buildDirectory.dir("libs"))
    }
    into("distribution")
}
