/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2022 Arty Bishop (bishop.arty@gmail.com)
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

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.presentation.entriesScreen.EntriesScreen
import com.rtbishop.look4sat.presentation.mapScreen.MapScreen
import com.rtbishop.look4sat.presentation.passesScreen.PassesScreen
import com.rtbishop.look4sat.presentation.radarScreen.RadarScreen
import com.rtbishop.look4sat.presentation.settingsScreen.SettingsScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent { MainTheme { MainScreen() } }
    }

    override fun attachBaseContext(newBase: Context?) {
        val newConfig = Configuration(newBase?.resources?.configuration)
        newConfig.fontScale = 1.0f
        applyOverrideConfiguration(newConfig)
        super.attachBaseContext(newBase)
    }
}

sealed class Screen(var title: String, var icon: Int, var route: String) {
    object Entries : Screen("Entries", R.drawable.ic_entries, "entries")
    object Passes : Screen("Passes", R.drawable.ic_passes, "passes")
    object Radar : Screen("Radar", R.drawable.ic_radar, "radar")
    object Map : Screen("World Map", R.drawable.ic_world_map, "world_map")
    object Settings : Screen("Settings", R.drawable.ic_settings, "settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(navController: NavHostController = rememberNavController()) {
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
    val radarRoute = "${Screen.Radar.route}?catNum={catNum}&aosTime={aosTime}"
    val radarArgs = listOf(navArgument("catNum") { defaultValue = 0 },
        navArgument("aosTime") { defaultValue = 0L })
    NavHost(navController, startDestination = Screen.Passes.route) {
        composable(Screen.Entries.route) { EntriesScreen(navController) }
        composable(Screen.Passes.route) { PassesScreen(navController) }
        composable(radarRoute, radarArgs) { RadarScreen(navController) }
        composable(Screen.Map.route) { MapScreen() }
        composable(Screen.Settings.route) { SettingsScreen(navController) }
    }
}
