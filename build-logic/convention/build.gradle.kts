import org.gradle.initialization.DependenciesAccessors
import org.gradle.kotlin.dsl.support.serviceOf
//import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)

    gradle.serviceOf<DependenciesAccessors>().classes.asFiles.forEach {
        compileOnly(files(it.absolutePath))
    }
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

group = "com.rtbishop.look4sat.build_logic.convention"

//kotlin {
//    jvmToolchain(libs.versions.jdkVersion.get().toInt())
//}

//java {
//    sourceCompatibility = JavaVersion.VERSION_17
//    targetCompatibility = JavaVersion.VERSION_17
//}
//
//tasks.withType<KotlinCompile>().configureEach {
//    kotlinOptions {
//        jvmTarget = JavaVersion.VERSION_17.toString()
//    }
//}

gradlePlugin {
    plugins {
        register("androidAppPlugin") {
            id = libs.plugins.convention.androidAppPlugin.get().pluginId
            implementationClass = "com.rtbishop.look4sat.convention.AndroidAppPlugin"
        }
        register("androidLibraryModule") {
            id = libs.plugins.convention.androidLibraryPlugin.get().pluginId
            implementationClass = "com.rtbishop.look4sat.convention.AndroidLibraryPlugin"
        }
        register("composeFeatureModule") {
            id = libs.plugins.convention.composeFeaturePlugin.get().pluginId
            implementationClass = "com.rtbishop.look4sat.convention.ComposeFeaturePlugin"
        }
        register("kotlinLibraryPlugin") {
            id = libs.plugins.convention.kotlinLibraryPlugin.get().pluginId
            implementationClass = "com.rtbishop.look4sat.convention.KotlinLibraryPlugin"
        }
    }
}
