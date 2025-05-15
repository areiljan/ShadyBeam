import java.util.Properties

val envFile = file(".env")
val env = Properties().apply {
    if (envFile.exists()) {
        envFile.inputStream().use { load(it) }
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
}


android {
    namespace = "com.example.flashlightapplication"
    compileSdk = 35

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.flashlightapplication"
        minSdk = 35
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "int",
            "CAMERA_PERMISSION_REQUEST_CODE",
            env.getProperty("camera_permission_request_code", "0")
        )
        buildConfigField(
            "String",
            "API_ENDPOINT",
            "\"${env.getProperty("api_endpoint", "https://fallback.url")}\""
        )
        buildConfigField(
            "String",
            "TAG",
            "\"${env.getProperty("tag", "app")}\""
        )
        buildConfigField(
            "int",
            "IMAGE_WIDTH",
            env.getProperty("image_width", "1024")
        )
        buildConfigField(
            "int",
            "IMAGE_HEIGHT",
            env.getProperty("image_height", "768")
        )
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.filament.android)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
