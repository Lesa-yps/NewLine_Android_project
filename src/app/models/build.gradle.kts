plugins {
    kotlin("jvm") version "2.1.0"
}

group = "com.example.newline"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(libs.jbcrypt) // шифрование пароля
}

kotlin {
    jvmToolchain(17)
}