plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.kotlin.android)
}

kotlin {
    jvmToolchain(21)
}

android {
    namespace = "com.rtbishop.look4sat.data"
    compileSdk = 36
    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(project(":domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.room)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    implementation(libs.other.coroutines)
    implementation(libs.other.okhttp)

    testImplementation(libs.bundles.unitTest)
    androidTestImplementation(libs.bundles.androidTest)
}
