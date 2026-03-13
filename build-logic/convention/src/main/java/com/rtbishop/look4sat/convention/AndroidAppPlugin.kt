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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

@Suppress("Unused")
internal class AndroidAppPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            setupAndroidApplication()
            setupComposeFeature()
            setupKotlinToolchain()
            setupAndroidTestDependencies()
            setupTestDependencies()
            dependencies {
                IMPLEMENTATION(project(":core:data"))
                IMPLEMENTATION(project(":core:domain"))
                IMPLEMENTATION(project(":core:presentation"))
                IMPLEMENTATION(project(":feature:map"))
                IMPLEMENTATION(project(":feature:passes"))
                IMPLEMENTATION(project(":feature:radar"))
                IMPLEMENTATION(project(":feature:satellites"))
                IMPLEMENTATION(project(":feature:settings"))
                IMPLEMENTATION(libs.androidx.core.splashscreen)
            }
        }
    }
}
