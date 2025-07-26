package com.rtbishop.look4sat.presentation

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.presentation.common.isVerticalLayout
import com.rtbishop.look4sat.presentation.map.mapDestination
import com.rtbishop.look4sat.presentation.passes.passesDestination
import com.rtbishop.look4sat.presentation.radar.radarDestination
import com.rtbishop.look4sat.presentation.satellites.satellitesDestination
import com.rtbishop.look4sat.presentation.settings.settingsDestination

sealed class Screen(val title: String, val icon: Int, val route: String) {
    data object Satellites : Screen("Satellites", R.drawable.ic_sputnik, "satellites")
    data object Passes : Screen("Passes", R.drawable.ic_passes, "passes")
    data object Radar : Screen("Radar", R.drawable.ic_satellite, "radar")
    data object Map : Screen("Map", R.drawable.ic_map, "map")
    data object Settings : Screen("Settings", R.drawable.ic_settings, "settings")
}

private val startDestination = Screen.Passes.route

@Composable
fun MainScreen(navController: NavHostController = rememberNavController()) {
    val items = listOf(Screen.Satellites, Screen.Passes, Screen.Radar, Screen.Map, Screen.Settings)
    val destinationRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            items.forEach {
                item(
                    icon = { Icon(painterResource(it.icon), it.title) },
                    label = { Text(it.title) },
                    selected = destinationRoute?.contains(it.route) ?: false,
                    onClick = {
                        if (destinationRoute?.contains(it.route) ?: false) return@item
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
            isVerticalLayout() -> NavigationSuiteType.NavigationBar
            else -> NavigationSuiteType.NavigationRail
        }
    ) { MainNavHost(navController) }
}

@Composable
private fun MainNavHost(navController: NavHostController) {
    val navigateToRadar = { catNum: Int, aosTime: Long ->
        val routeWithParams = "${Screen.Radar.route}?catNum=${catNum}&aosTime=${aosTime}"
        navController.navigate(routeWithParams)
    }
    NavHost(navController = navController, startDestination = startDestination) {
        satellitesDestination { navController.navigateUp() }
        passesDestination(navigateToRadar)
        radarDestination { navController.navigateUp() }
        mapDestination()
        settingsDestination()
    }
}
