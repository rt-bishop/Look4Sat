@file: Suppress("UnstableApiUsage")

plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.rtbishop.look4sat"
    compileSdk = 33
    defaultConfig {
        applicationId = "com.rtbishop.look4sat"
        minSdk = 21
        targetSdk = 33
        versionCode = 313
        versionName = "3.1.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.1" }
    packaging { resources { excludes += listOf("META-INF/{AL2.0,LGPL2.1}") } }
}

dependencies {
    implementation(project(":data"))
    implementation(project(":domain"))

    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.room:room-runtime:2.5.2")
    implementation("androidx.room:room-ktx:2.5.2")
    ksp("androidx.room:room-compiler:2.5.2")

    implementation("androidx.activity:activity-compose:1.7.2")
    implementation("androidx.compose.animation:animation:1.4.3")
    implementation("androidx.compose.compiler:compiler:1.5.1")
    implementation("androidx.compose.material3:material3:1.1.1")
    implementation("androidx.compose.runtime:runtime:1.4.3")
    implementation("androidx.compose.ui:ui-tooling-preview:1.4.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    implementation("androidx.navigation:navigation-compose:2.6.0")

    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("org.osmdroid:osmdroid-android:6.1.16")
    debugImplementation("androidx.compose.ui:ui-tooling:1.4.3")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.4.3")
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")

    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.4.3")
}
