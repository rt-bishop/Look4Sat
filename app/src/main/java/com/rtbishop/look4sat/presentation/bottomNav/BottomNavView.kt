package com.rtbishop.look4sat.presentation.bottomNav

import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.presentation.theme.Yellow
import com.rtbishop.look4sat.presentation.theme.Look4SatTheme
import com.rtbishop.look4sat.presentation.theme.TextWhite

@Composable
fun BottomNavView(navController: NavController) {
    val items = listOf(
        BottomNavItem.Satellites,
        BottomNavItem.Passes,
        BottomNavItem.WorldMap,
        BottomNavItem.Settings,
        BottomNavItem.About
    )
    BottomNavigation(backgroundColor = colorResource(id = R.color.bottomBar)) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        items.forEach { item ->
            BottomNavigationItem(icon = {
                Icon(painterResource(id = item.icon), contentDescription = item.title)
            },
                label = { Text(text = item.title, fontSize = 9.sp) },
                selectedContentColor = Yellow,
                unselectedContentColor = TextWhite.copy(0.4f),
                alwaysShowLabel = true,
                selected = currentRoute == item.screen_route,
                onClick = {
                    navController.navigate(item.screen_route) {
                        navController.graph.startDestinationRoute?.let { screen_route ->
                            popUpTo(screen_route) { saveState = true }
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                })
        }
    }
}

@Composable
fun NavigationGraph(navController: NavHostController) {
    NavHost(navController, startDestination = BottomNavItem.Satellites.screen_route) {
        composable(BottomNavItem.Satellites.screen_route) { SatellitesScreen() }
        composable(BottomNavItem.Passes.screen_route) { PassesScreen() }
        composable(BottomNavItem.WorldMap.screen_route) { WorldMapScreen() }
        composable(BottomNavItem.Settings.screen_route) { SettingsScreen() }
        composable(BottomNavItem.About.screen_route) { AboutScreen() }
    }
}

@Composable
fun MainScreenView() {
    val navController = rememberNavController()
    Scaffold(bottomBar = { BottomNavView(navController = navController) }) {
        NavigationGraph(navController = navController)
        it.calculateBottomPadding()
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    Look4SatTheme { MainScreenView() }
}
