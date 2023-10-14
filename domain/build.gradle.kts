plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())
}

dependencies {
    implementation(libs.other.coroutines)
    implementation(libs.other.json)

    testImplementation(libs.bundles.unitTest)
}
