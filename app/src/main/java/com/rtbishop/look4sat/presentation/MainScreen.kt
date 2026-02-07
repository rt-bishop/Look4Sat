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
package com.rtbishop.look4sat.presentation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.presentation.common.hasEnoughHeight
import com.rtbishop.look4sat.presentation.common.hasEnoughWidth
import com.rtbishop.look4sat.presentation.map.mapDestination
import com.rtbishop.look4sat.presentation.passes.passesDestination
import com.rtbishop.look4sat.presentation.radar.radarDestination
import com.rtbishop.look4sat.presentation.satellites.satellitesDestination
import com.rtbishop.look4sat.presentation.settings.settingsDestination

sealed class Screen(val route: String, val iconResId: Int, val titleResId: Int) {
    data object Satellites : Screen("satellites", R.drawable.ic_satellites, R.string.nav_sat)
    data object Passes : Screen("passes", R.drawable.ic_passes, R.string.nav_pass)
    data object Radar : Screen("radar", R.drawable.ic_radar, R.string.nav_radar)
    data object Map : Screen("map", R.drawable.ic_map, R.string.nav_map)
    data object Settings : Screen("settings", R.drawable.ic_settings, R.string.nav_prefs)
}

@Composable
fun MainScreen(navController: NavHostController = rememberNavController()) {
    val items = listOf(Screen.Satellites, Screen.Passes, Screen.Radar, Screen.Map, Screen.Settings)
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination?.route
    val startDestination = Screen.Passes.route
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            items.forEach {
                item(
                    icon = { Icon(painterResource(it.iconResId), stringResource(it.titleResId)) },
                    label = { Text(stringResource(it.titleResId)) },
                    selected = currentDestination?.contains(it.route) ?: false,
                    onClick = {
                        if (currentDestination?.contains(it.route) ?: false) return@item
                        navController.navigate(it.route) {
                            popUpTo(startDestination) { saveState = false }
                            launchSingleTop = true
                            restoreState = false
                        }
                    })
            }
        }, navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationRailContainerColor = MaterialTheme.colorScheme.surfaceContainer
        ), layoutType = when {
            !hasEnoughHeight() && hasEnoughWidth() -> NavigationSuiteType.NavigationRail
            !hasEnoughWidth() -> NavigationSuiteType.ShortNavigationBarCompact
            else -> NavigationSuiteType.ShortNavigationBarMedium
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition =  { fadeIn(animationSpec = tween(350)) },
            exitTransition = { fadeOut(animationSpec = tween(350)) }
        ) {
            satellitesDestination { navController.navigateUp() }
            passesDestination { catNum: Int, aosTime: Long ->
                val radarRoute = "${Screen.Radar.route}?catNum=${catNum}&aosTime=${aosTime}"
                navController.navigate(radarRoute)
            }
            radarDestination { navController.navigateUp() }
            mapDestination()
            settingsDestination()
        }
    }
}
