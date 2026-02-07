plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.google.ksp)
}

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "com.rtbishop.look4sat.data"
    compileSdk = 36
    defaultConfig {
        minSdk = 24
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
