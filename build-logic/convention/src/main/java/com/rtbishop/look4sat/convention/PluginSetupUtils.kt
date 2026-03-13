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
import com.android.build.api.dsl.CommonExtension
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.api.plugins.PluginManager
import org.gradle.api.provider.Provider
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
    get(): LibrariesForLibs = extensionOf(this, "libs") as LibrariesForLibs

internal fun PluginManager.alias(notation: Provider<PluginDependency>) {
    apply(notation.get().pluginId)
}

internal fun Project.setupAndroidApplication() {
    with(pluginManager) {
        alias(libs.plugins.android.application)
    }
    extensions.configure<ApplicationExtension> {
        namespace = libs.versions.packageName.get()
        compileSdk = libs.versions.compileSdk.get().toInt()
        defaultConfig {
            applicationId = libs.versions.packageName.get()
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
        packaging { resources { excludes += listOf("META-INF/*") } }
    }
}

internal fun Project.setupCommonLibrary() {
    with(pluginManager) {
        alias(libs.plugins.android.library)
    }
    extensions.configure<CommonExtension> {
        compileSdk = libs.versions.compileSdk.get().toInt()
        defaultConfig.minSdk = libs.versions.minSdk.get().toInt()
    }
}

internal fun Project.setupComposeFeature() {
    with(pluginManager) {
        alias(libs.plugins.compose.compiler)
    }
    extensions.configure<CommonExtension> {
        buildFeatures.compose = true
    }
    dependencies {
        IMPLEMENTATION(platform(libs.compose.bom))
        IMPLEMENTATION(libs.bundles.composeAll)
        DEBUG_IMPLEMENTATION(libs.bundles.composeDebug)
    }
}

internal fun Project.setupKotlinToolchain() {
    kotlinExtension.jvmToolchain(libs.versions.jdkVersion.get().toInt())
}

internal fun Project.setupAndroidTestDependencies() {
    dependencies {
        ANDROID_TEST_IMPLEMENTATION(libs.bundles.androidTest)
    }
}

internal fun Project.setupTestDependencies() {
    dependencies {
        TEST_IMPLEMENTATION(libs.test.coroutines)
        TEST_IMPLEMENTATION(libs.test.junit4)
    }
}
