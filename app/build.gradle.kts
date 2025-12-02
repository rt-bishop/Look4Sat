plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.kotlin.android)
}

kotlin {
    jvmToolchain(21)
}

android {
    namespace = "com.rtbishop.look4sat"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.rtbishop.look4sat"
        minSdk = 24
        targetSdk = 36
        versionCode = 400
        versionName = "4.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        compose = true
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    androidResources {
        generateLocaleConfig = true
        localeFilters.addAll(listOf("en", "es-rES", "ru", "si", "uk", "zh-rCN"))
    }
}

dependencies {
    implementation(project(":data"))
    implementation(project(":domain"))

    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.room)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.composeAll)

    implementation(libs.other.okhttp)
    implementation(libs.other.osmdroid)

    debugImplementation(libs.bundles.composeDebug)
    testImplementation(libs.bundles.unitTest)
    androidTestImplementation(libs.bundles.androidTest)
}
