package com.rtbishop.look4sat.presentation.bottomNav

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
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rtbishop.look4sat.presentation.MainTheme
import com.rtbishop.look4sat.presentation.aboutScreen.AboutScreen
import com.rtbishop.look4sat.presentation.entriesScreen.EntriesScreen

@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(
        BottomNavItem.Satellites,
        BottomNavItem.Passes,
        BottomNavItem.WorldMap,
        BottomNavItem.Settings,
        BottomNavItem.About
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
    NavHost(navController, startDestination = BottomNavItem.Passes.screen_route) {
        composable(BottomNavItem.Satellites.screen_route) { EntriesScreen(navToPasses) }
        composable(BottomNavItem.Passes.screen_route) { PassesScreen() }
        composable(BottomNavItem.WorldMap.screen_route) { WorldMapScreen() }
        composable(BottomNavItem.Settings.screen_route) { SettingsScreen() }
        composable(BottomNavItem.About.screen_route) { AboutScreen() }
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
