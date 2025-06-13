@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.rtbishop.look4sat.presentation

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.presentation.info.infoDestination
import com.rtbishop.look4sat.presentation.map.mapDestination
import com.rtbishop.look4sat.presentation.passes.passesDestination
import com.rtbishop.look4sat.presentation.radar.radarDestination
import com.rtbishop.look4sat.presentation.satellites.satellitesDestination
import com.rtbishop.look4sat.presentation.settings.settingsDestination

sealed class Screen(var title: String, var icon: Int, var route: String) {
    data object Satellites : Screen("Satellites", R.drawable.ic_sputnik, "satellites")
    data object PassesGraph : Screen("Passes", R.drawable.ic_passes, "passesGraph")
    data object Passes : Screen("Passes", R.drawable.ic_passes, "passes")
    data object Radar : Screen("Radar", R.drawable.ic_sputnik, "radar")
    data object Map : Screen("Map", R.drawable.ic_map, "map")
    data object Settings : Screen("Settings", R.drawable.ic_settings, "settings")
    data object Info : Screen("Info", R.drawable.ic_info, "info")
}

val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalNavAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

@Composable
fun MainScreen() {
    val navController: NavHostController = rememberNavController()
    val navigateBack: () -> Unit = { navController.navigateUp() }
    val navigateToRadar = { catNum: Int, aosTime: Long ->
        val routeWithParams = "${Screen.Radar.route}?catNum=${catNum}&aosTime=${aosTime}"
        navController.navigate(routeWithParams)
    }
    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavHost(navController = navController, startDestination = Screen.PassesGraph.route) {
                satellitesDestination(navController, navigateBack)
                passesGraph(navController, navigateBack, navigateToRadar)
                radarDestination(navigateBack)
                mapDestination(navController)
                settingsDestination(navController)
                infoDestination(navController)
            }
        }
    }
}

private fun NavGraphBuilder.passesGraph(
    navController: NavHostController,
    navigateBack: () -> Unit,
    navigateToRadar: (Int, Long) -> Unit
) {
    navigation(route = Screen.PassesGraph.route, startDestination = Screen.Passes.route) {
        passesDestination(navController, navigateToRadar)
        radarDestination(navigateBack)
    }
}

@SuppressLint("RestrictedApi", "StateFlowValueCalledInComposition")
@Composable
fun MainNavBar(navController: NavController) {
    val sharedTransitionScope = LocalSharedTransitionScope.current
        ?: throw IllegalStateException("No SharedElementScope found")
    val animatedVisibilityScope = LocalNavAnimatedVisibilityScope.current
        ?: throw IllegalStateException("No SharedElementScope found")
    val sharedTransitionState = sharedTransitionScope.rememberSharedContentState("bottomNav")
    val items = listOf(Screen.Satellites, Screen.Passes, Screen.Map, Screen.Settings, Screen.Info)
    val stack = navController.currentBackStack.value.map { it.destination.route }
    Log.d("NavBar", "NavStack: $stack")
    with(sharedTransitionScope) {
        NavigationBar(
            modifier = Modifier.sharedElement(sharedTransitionState, animatedVisibilityScope)
        ) {
            items.forEach { item ->
                NavigationBarItem(
                    icon = { Icon(painterResource(item.icon), item.title) },
                    label = { Text(item.title) },
                    selected = item.route == navController.currentDestination?.route,
                    onClick = {
                        if (item.route == navController.currentDestination?.route) {
                            // selecting the same tab
                        } else {
                            navController.navigate(item.route) {
                                popUpTo(Screen.Passes.route) { saveState = false }
                                launchSingleTop = true
                                restoreState = false
                            }
                        }
                    }
                )
            }
        }
    }
}
