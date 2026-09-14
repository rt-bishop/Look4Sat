plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.google.ksp) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.other.detekt)
}

val detektCheck = tasks.register<io.gitlab.arturbosch.detekt.Detekt>("detektCheck") {
    description = "Checks that source code satisfies detekt rules."
    autoCorrect = false
}

val detektApply = tasks.register<io.gitlab.arturbosch.detekt.Detekt>("detektApply") {
    description = "Applies code formatting rules to source code in-place."
    autoCorrect = true
}

configure(listOf(detektCheck, detektApply)) {
    configure {
        group = "verification"
        parallel = true
        ignoreFailures = false
        setSource(file(rootDir))

        // Custom detekt config
        config.setFrom("$projectDir/config/detekt/detekt.yml")

        // Use default configuration on top of custom config
        // (new detect rules will work out of the box after upgrading detekt version)
        buildUponDefaultConfig = true

        // Runs detekt for all files in the Gradle project and all subprojects without
        // a need to configure detekt plugin in every subproject.
        include("**/*.kt", "**/*.kts")
        exclude("**/resources/**", "**/build/**", "**/generated/**", "**/testing/**")

        reports {
            html.required.set(true)
            xml.required.set(true)
        }
    }
    dependencies {
        detektPlugins(libs.other.detekt.formatting)
    }
}

tasks.register("clean", Delete::class.java) {
    description = "Cleans the build directory"
    delete(rootProject.layout.buildDirectory)
}
