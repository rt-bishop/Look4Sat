plugins {
    alias(libs.plugins.convention.composeFeaturePlugin)
}

android {
    namespace = "com.rtbishop.look4sat.feature.map"
}

dependencies {
    implementation(libs.other.osmdroid)
}
