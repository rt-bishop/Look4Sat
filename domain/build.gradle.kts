plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.other.coroutines)
    implementation(libs.other.json)

    testImplementation(libs.bundles.unitTest)
}
