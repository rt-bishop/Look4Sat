plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "com.rtbishop.look4sat"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.rtbishop.look4sat"
        minSdk = 24
        versionCode = 402
        versionName = "4.0.2"
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
        localeFilters.addAll(listOf("en", "es", "ru", "si", "uk", "zh"))
    }
}

dependencies {
    implementation(project(":data"))
    implementation(project(":domain"))

    implementation(libs.androidx.core.splashscreen)
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.composeAll)
    implementation(libs.other.osmdroid)

    debugImplementation(libs.bundles.composeDebug)
    testImplementation(libs.bundles.unitTest)
    androidTestImplementation(libs.bundles.androidTest)
}
