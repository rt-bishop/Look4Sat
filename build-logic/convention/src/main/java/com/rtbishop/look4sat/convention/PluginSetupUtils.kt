/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2026 Arty Bishop and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.rtbishop.look4sat.convention

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.plugins.PluginManager
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderConvertible
import org.gradle.kotlin.dsl.accessors.runtime.extensionOf
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.plugin.use.PluginDependency
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension

internal const val ANDROID_TEST_IMPLEMENTATION = "androidTestImplementation"
internal const val DEBUG_IMPLEMENTATION = "debugImplementation"
internal const val IMPLEMENTATION = "implementation"
internal const val KSP = "ksp"
internal const val TEST_IMPLEMENTATION = "testImplementation"

internal val Project.libs
//    get(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
    get(): LibrariesForLibs = extensionOf(this, "libs") as LibrariesForLibs

internal fun Project.kotlinLibraryPlugin() {
//    kotlinExtension.jvmToolchain(libs.findVersion("jdkVersion").get().requiredVersion.toInt())
    kotlinExtension.jvmToolchain(libs.versions.jdkVersion.get().toInt())
//    tasks.withType(AbstractTestTask::class.java).configureEach { setProperty("failOnNoDiscoveredTests", false) }
}

fun PluginManager.alias(notation: Provider<PluginDependency>) {
    apply(notation.get().pluginId)
}

fun PluginManager.alias(notation: ProviderConvertible<PluginDependency>) {
    apply(notation.asProvider().get().pluginId)
}

fun DependencyHandler.implementation(provider: Provider<MinimalExternalModuleDependency>) {
    add("implementation", provider.get().group + ":" + provider.get().name + ":" + provider.get().version)
}

fun DependencyHandler.testImplementation(provider: Provider<MinimalExternalModuleDependency>) {
    add("testImplementation", provider.get().group + ":" + provider.get().name + ":" + provider.get().version)
}

fun DependencyHandler.ksp(provider: Provider<MinimalExternalModuleDependency>) {
    add("ksp", provider.get().group + ":" + provider.get().name + ":" + provider.get().version)
}

internal fun Project.androidLibraryPlugin(commonExtension: LibraryExtension) {
    commonExtension.apply {
        compileSdk = libs.versions.compileSdk.get().toInt()
        defaultConfig {
            minSdk = libs.versions.minSdk.get().toInt()
        }
//        compileOptions { isCoreLibraryDesugaringEnabled = true }
//        testOptions {
//            unitTests {
//                all { it.useJUnitPlatform() }
//                isReturnDefaultValues = true
//                isIncludeAndroidResources = true
//            }
//        }
//        packaging { resources { excludes += listOf("META-INF/*") } }
//        dependencies {
//            IMPLEMENTATION(libs.findLibrary("androidx-core").get())
//            IMPLEMENTATION(libs.findLibrary("kotlinx-serialization-json").get())
//            IMPLEMENTATION(libs.findLibrary("other-timber").get())
//            IMPLEMENTATION(libs.findLibrary("google-hilt-android").get())
//            KSP(libs.findLibrary("google-hilt-compiler").get())
//
//            ANDROID_TEST_IMPLEMENTATION(libs.findBundle("androidTest").get())
//            ANDROID_TEST_IMPLEMENTATION(libs.findBundle("unitTest").get())
//            CORE_LIBRARY_DESUGARING(libs.findLibrary("other-desugaring").get())
//            TEST_IMPLEMENTATION(libs.findBundle("unitTest").get())
//        }
    }
}

internal fun Project.androidAppPlugin(commonExtension: ApplicationExtension) {
    commonExtension.apply {
        namespace = "com.rtbishop.look4sat"
        compileSdk = libs.versions.compileSdk.get().toInt()
        defaultConfig {
            applicationId = "com.rtbishop.look4sat"
            minSdk = libs.versions.minSdk.get().toInt()
            versionCode = libs.versions.appVersionCode.get().toInt()
            versionName = libs.versions.appVersionName.get()
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
//    extensions.configure<ApplicationExtension> {
//        namespace = "com.rtbishop.look4sat"
//        compileSdk = 36
//        defaultConfig {
//            applicationId = "com.rtbishop.look4sat"
//            minSdk = 24
//            versionCode = 410
//            versionName = "4.1.0"
//        }
//        buildFeatures {
//            compose = true
//        }
//        buildTypes {
//            debug {
//                applicationIdSuffix = ".debug"
//            }
//            release {
//                isMinifyEnabled = true
//                isShrinkResources = true
//                proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
//            }
//        }
//        androidResources {
//            generateLocaleConfig = true
//            localeFilters.addAll(listOf("en", "es", "ru", "si", "uk", "zh"))
//        }
//    }
}

internal fun Project.composeFeaturePlugin(commonExtension: LibraryExtension) {
    commonExtension.apply {
        buildFeatures { compose = true }
//        testOptions { animationsDisabled = true }
        dependencies {
//            IMPLEMENTATION(platform(libs.findLibrary("compose-bom").get()))
//            IMPLEMENTATION(libs.findBundle("composeAll").get())
//
//            ANDROID_TEST_IMPLEMENTATION(libs.findBundle("composeDebug").get())
//            DEBUG_IMPLEMENTATION(libs.findBundle("composeDebug").get())
//            SCREENSHOT_TEST_IMPLEMENTATION(libs.findLibrary("compose-ui-tooling").get())
        }
    }
}

fun Project.setupAndroidModule(isApplication: Boolean) {
    with(pluginManager) {
        if (isApplication) {
            alias(libs.plugins.android.application)
            alias(libs.plugins.compose.compiler)
        } else {
            alias(libs.plugins.android.library)
            alias(libs.plugins.google.ksp)
        }
    }
    extensions.configure<ApplicationExtension> {
        compileSdk = libs.versions.compileSdk.get().toInt()
        defaultConfig {
            minSdk = 26
            versionCode = 1
            versionName = "1.0"
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
        buildTypes {
            buildTypes {
                maybeCreate("debug")
                maybeCreate("release")
                named("release") {
                    isMinifyEnabled = true
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        "proguard-rules.pro"
                    )
                }
            }
        }
    }
}
