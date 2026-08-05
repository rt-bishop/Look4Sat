import com.android.build.api.dsl.LibraryExtension

plugins {
    alias(libs.plugins.convention.featurePlugin)
}

android {
    namespace = "com.rtbishop.look4sat.feature.status"
    compileOptions {
        encoding = "UTF-8"
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:presentation"))
}
