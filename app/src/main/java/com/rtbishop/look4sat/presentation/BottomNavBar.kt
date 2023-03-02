package com.rtbishop.look4sat.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.presentation.entriesScreen.EntriesScreen
import com.rtbishop.look4sat.presentation.mapScreen.MapScreen
import com.rtbishop.look4sat.presentation.passesScreen.PassesScreen
import com.rtbishop.look4sat.presentation.passesScreen.PassesViewModel
import com.rtbishop.look4sat.presentation.radarScreen.RadarScreen
import com.rtbishop.look4sat.presentation.settingsScreen.SettingsScreen

sealed class BottomNavItem(var title: String, var icon: Int, var screen_route: String) {
    object Satellites : BottomNavItem("Satellites", R.drawable.ic_satellite, "satellites")
    object Passes : BottomNavItem("Passes", R.drawable.ic_list, "passes")
    object Radar : BottomNavItem("Radar", R.drawable.ic_radar, "radar")
    object WorldMap : BottomNavItem("World Map", R.drawable.ic_world, "world_map")
    object Settings : BottomNavItem("Settings", R.drawable.ic_settings, "settings")
}

@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(
        BottomNavItem.Satellites,
        BottomNavItem.Passes,
        BottomNavItem.Radar,
        BottomNavItem.WorldMap,
        BottomNavItem.Settings
    )
    NavigationBar(modifier = Modifier.height(48.dp)) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        items.forEach { item ->
            NavigationBarItem(selected = currentRoute == item.screen_route, onClick = {
                navController.navigate(item.screen_route) {
                    navController.graph.startDestinationRoute?.let { screen_route ->
                        popUpTo(screen_route) { saveState = false }
                    }
                    launchSingleTop = true
                    restoreState = false
                }
            }, icon = { Icon(painterResource(id = item.icon), contentDescription = item.title) })
        }
    }
}

@Composable
fun NavigationGraph(navController: NavHostController) {
    val navToPasses = { navController.navigate(BottomNavItem.Passes.screen_route) }
    val navToRadar = { navController.navigate(BottomNavItem.Radar.screen_route) }
    val passesViewModel: PassesViewModel = hiltViewModel()
    NavHost(navController, startDestination = BottomNavItem.Passes.screen_route) {
        composable(BottomNavItem.Satellites.screen_route) {
            EntriesScreen(navToPasses, passesViewModel = passesViewModel)
        }
        composable(BottomNavItem.Passes.screen_route) {
            PassesScreen(navToRadar, viewModel = passesViewModel)
        }
        composable(BottomNavItem.Radar.screen_route) { RadarScreen() }
        composable(BottomNavItem.WorldMap.screen_route) { MapScreen() }
        composable(BottomNavItem.Settings.screen_route) { SettingsScreen() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenView(navController: NavHostController = rememberNavController()) {
    Scaffold(bottomBar = { BottomNavBar(navController = navController) }) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) { NavigationGraph(navController) }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MainTheme { MainScreenView() }
}
