plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.data"
    compileSdk = 35

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Firebase
    implementation(platform(libs.firebase.bom.v33140))
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.com.google.firebase.firebase.auth.ktx)
    implementation(libs.google.firebase.analytics.ktx)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.androidx.core.ktx.v1160)
    implementation(libs.kotlinx.coroutines.android)

    // Модули проекта
    implementation(project(":app:models"))
    implementation(project(":app:logic"))
}