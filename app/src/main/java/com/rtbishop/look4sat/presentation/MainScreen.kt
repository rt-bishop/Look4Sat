package com.rtbishop.look4sat.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.presentation.entries.EntriesScreen
import com.rtbishop.look4sat.presentation.map.MapScreen
import com.rtbishop.look4sat.presentation.passes.PassesScreen
import com.rtbishop.look4sat.presentation.radar.RadarScreen
import com.rtbishop.look4sat.presentation.settings.SettingsScreen

sealed class Screen(var title: String, var icon: Int, var route: String) {
    data object Entries : Screen("Entries", R.drawable.ic_sputnik, "entries")
    data object Passes : Screen("Passes", R.drawable.ic_passes, "passes")
    data object Radar : Screen("Radar", R.drawable.ic_settings, "radar")
    data object Map : Screen("World Map", R.drawable.ic_world_map, "world_map")
    data object Settings : Screen("Settings", R.drawable.ic_settings, "settings")
}

@Composable
fun MainScreen(navController: NavHostController = rememberNavController()) {
    Scaffold(bottomBar = { MainNavBar(navController = navController) }) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) { MainNavGraph(navController) }
    }
}

@Composable
private fun MainNavBar(navController: NavController) {
    val items = listOf(Screen.Entries, Screen.Passes, Screen.Radar, Screen.Map, Screen.Settings)
    NavigationBar(modifier = Modifier.height(48.dp)) {
        val navBackStackEntry = navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry.value?.destination?.route
        items.forEach { item ->
            NavigationBarItem(selected = currentRoute?.contains(item.route) ?: false, onClick = {
                navController.navigate(item.route) {
                    navController.graph.startDestinationRoute?.let { route ->
                        popUpTo(route) { saveState = false }
                    }
                    launchSingleTop = true
                    restoreState = false
                }
            }, icon = { Icon(painterResource(id = item.icon), contentDescription = item.title) })
        }
    }
}

@Composable
private fun MainNavGraph(navController: NavHostController) {
    val navToPasses = { navController.navigate(Screen.Passes.route) }
    val navToRadar = { catNum: Int, aosTime: Long ->
        navController.navigate("${Screen.Radar.route}?catNum=${catNum}&aosTime=${aosTime}")
    }
    val radarRoute = "${Screen.Radar.route}?catNum={catNum}&aosTime={aosTime}"
    val radarArgs = listOf(navArgument("catNum") { defaultValue = 0 },
        navArgument("aosTime") { defaultValue = 0L })
    NavHost(navController, startDestination = Screen.Passes.route) {
        composable(Screen.Entries.route) { EntriesScreen(navToPasses) }
        composable(Screen.Passes.route) { PassesScreen(navToRadar) }
        composable(radarRoute, radarArgs) { RadarScreen() }
        composable(Screen.Map.route) { MapScreen() }
        composable(Screen.Settings.route) { SettingsScreen() }
    }
}
