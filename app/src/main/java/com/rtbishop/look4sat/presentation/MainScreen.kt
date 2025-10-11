package com.rtbishop.look4sat.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.rtbishop.look4sat.presentation.map.mapDestination
import com.rtbishop.look4sat.presentation.passes.passesDestination
import com.rtbishop.look4sat.presentation.radar.radarDestination
import com.rtbishop.look4sat.presentation.satellites.satellitesDestination
import com.rtbishop.look4sat.presentation.settings.settingsDestination

sealed class Screen(val route: String) {
    data object Satellites : Screen("satellites")
    data object Passes : Screen("passes")
    data object Radar : Screen("radar")
    data object Map : Screen("map")
    data object Settings : Screen("settings")
}

data class NavActions(
    val openSatellites: () -> Unit,
    val openRadar: (Int, Long) -> Unit,
    val openMap: () -> Unit,
    val openSettings: () -> Unit
)

@Composable
fun MainScreen(navController: NavHostController = rememberNavController()) {
    val navActions = NavActions(
        openSatellites = { navController.navigate(Screen.Satellites.route) },
        openRadar = { catNum: Int, aosTime: Long ->
            val routeWithParams = "${Screen.Radar.route}?catNum=${catNum}&aosTime=${aosTime}"
            navController.navigate(routeWithParams)
        },
        openMap = { navController.navigate(Screen.Map.route) },
        openSettings = { navController.navigate(Screen.Settings.route) }
    )
    NavHost(navController = navController, startDestination = Screen.Passes.route) {
        satellitesDestination { navController.navigateUp() }
        passesDestination(navActions)
        radarDestination { navController.navigateUp() }
        mapDestination { navController.navigateUp() }
        settingsDestination { navController.navigateUp() }
    }
}
