plugins {
    kotlin("jvm") version "2.1.0"
}

group = "com.example.newline"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    // Подключение внутренних модулей
    implementation(project(":app:models"))
    implementation(project(":app:logic"))
}

kotlin {
    jvmToolchain(17)
}