plugins {
    kotlin("jvm") version "2.1.0"
}

group = "com.example.newline"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(libs.koin.core)
    implementation(libs.kotlinx.coroutines.core)

    implementation(project(":app:models"))

    testImplementation(kotlin("test"))
    implementation(libs.jupiter.junit.jupiter)
}

kotlin {
    jvmToolchain(17)
}