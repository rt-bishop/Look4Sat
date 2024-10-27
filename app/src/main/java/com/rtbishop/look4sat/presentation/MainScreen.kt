package com.rtbishop.look4sat.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.presentation.info.InfoScreen
import com.rtbishop.look4sat.presentation.map.MapScreen
import com.rtbishop.look4sat.presentation.passes.PassesScreen
import com.rtbishop.look4sat.presentation.passes.PassesViewModel
import com.rtbishop.look4sat.presentation.radar.RadarScreen
import com.rtbishop.look4sat.presentation.satellites.SatellitesScreen
import com.rtbishop.look4sat.presentation.satellites.SatellitesViewModel
import com.rtbishop.look4sat.presentation.settings.SettingsScreen

sealed class Screen(var title: String, var icon: Int, var route: String) {
    data object Main : Screen("Main", R.drawable.ic_sputnik, "main")
    data object Radar : Screen("Radar", R.drawable.ic_sputnik, "radar")
    data object Satellites : Screen("Satellites", R.drawable.ic_sputnik, "satellites")
    data object Passes : Screen("Passes", R.drawable.ic_passes, "passes")
    data object Map : Screen("Map", R.drawable.ic_map, "map")
    data object Settings : Screen("Settings", R.drawable.ic_settings, "settings")
    data object Info : Screen("Info", R.drawable.ic_info, "info")
}

@Composable
fun MainScreen() {
    val outerNavController: NavHostController = rememberNavController()
    val radarRoute = "${Screen.Radar.route}?catNum={catNum}&aosTime={aosTime}"
    val radarArgs = listOf(navArgument("catNum") { defaultValue = 0 },
        navArgument("aosTime") { defaultValue = 0L })
    val navToRadar = { catNum: Int, aosTime: Long ->
        val navRoute = "${Screen.Radar.route}?catNum=${catNum}&aosTime=${aosTime}"
        outerNavController.navigate(navRoute)
    }
    NavHost(navController = outerNavController, startDestination = Screen.Main.route) {
        composable(Screen.Main.route) { NavBarScreen(navToRadar) }
        composable(radarRoute, radarArgs) { RadarScreen() }
    }
}

@Composable
private fun NavBarScreen(navToRadar: (Int, Long) -> Unit) {
    val innerNavController: NavHostController = rememberNavController()
    val navToPasses = { innerNavController.navigate(Screen.Passes.route) }
    Scaffold(bottomBar = { MainNavBar(navController = innerNavController) }) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(innerNavController, startDestination = Screen.Passes.route) {
                composable(Screen.Satellites.route) {
                    val viewModel = viewModel(
                        SatellitesViewModel::class.java, factory = SatellitesViewModel.Factory
                    )
                    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                    SatellitesScreen(uiState, navToPasses)
                }
                composable(Screen.Passes.route) {
                    val viewModel = viewModel(
                        PassesViewModel::class.java, factory = PassesViewModel.Factory
                    )
                    val uiState = viewModel.uiState.value
                    PassesScreen(uiState, navToRadar)
                }
                composable(Screen.Map.route) { MapScreen() }
                composable(Screen.Settings.route) { SettingsScreen() }
                composable(Screen.Info.route) { InfoScreen() }
            }
        }
    }
}

@Composable
private fun MainNavBar(navController: NavController) {
    val items = listOf(Screen.Satellites, Screen.Passes, Screen.Map, Screen.Settings, Screen.Info)
    val currentBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry.value?.destination?.route
    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(selected = currentRoute?.contains(item.route) ?: false,
                onClick = {
                    navController.navigate(item.route) {
                        navController.graph.startDestinationRoute?.let {
                            popUpTo(it) { saveState = false }
                        }
                        launchSingleTop = true
                        restoreState = false
                    }
                },
                icon = { Icon(painterResource(id = item.icon), contentDescription = item.title) },
                label = { Text(item.title) })
        }
    }
}
