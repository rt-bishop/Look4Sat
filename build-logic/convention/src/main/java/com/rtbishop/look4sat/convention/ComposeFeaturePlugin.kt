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

import com.android.build.api.dsl.LibraryExtension
import com.rtbishop.look4sat.convention.IMPLEMENTATION
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class ComposeFeaturePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                alias(libs.plugins.android.library)
                alias(libs.plugins.compose.compiler)
            }
            kotlinLibraryPlugin()
            extensions.configure<LibraryExtension> {
                androidLibraryPlugin(this)
                composeFeaturePlugin(this)
            }
            dependencies {
                IMPLEMENTATION(project(":domain"))

                implementation(libs.androidx.core.splashscreen)
                implementation(platform(libs.compose.bom))
                implementation(libs.compose.activity)
                implementation(libs.compose.lifecycle)
                implementation(libs.compose.navigation)
                implementation(libs.compose.viewmodel)
                implementation(libs.compose.animation)
                implementation(libs.compose.material3.asProvider())
                implementation(libs.compose.material3.navigation)
                implementation(libs.compose.runtime)
                implementation(libs.compose.tooling)
                implementation(libs.other.osmdroid)
                DEBUG_IMPLEMENTATION(libs.bundles.composeDebug)

                testImplementation(libs.test.coroutines)
                testImplementation(libs.test.junit4)
                ANDROID_TEST_IMPLEMENTATION(libs.bundles.androidTest)
            }
        }
    }
}
