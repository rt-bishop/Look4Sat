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
package com.rtbishop.look4sat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rtbishop.look4sat.core.presentation.Screen
import com.rtbishop.look4sat.core.presentation.hasEnoughHeight
import com.rtbishop.look4sat.core.presentation.hasEnoughWidth
import com.rtbishop.look4sat.core.domain.repository.IContainerProvider
import com.rtbishop.look4sat.feature.map.mapDestination
import com.rtbishop.look4sat.feature.passes.passesDestination
import com.rtbishop.look4sat.feature.radar.radarDestination
import com.rtbishop.look4sat.feature.radiocontrol.radioControlDestination
import com.rtbishop.look4sat.feature.satellites.satellitesDestination
import com.rtbishop.look4sat.feature.settings.settingsDestination

@Composable
fun MainScreen(navController: NavHostController = rememberNavController()) {
    val items = listOf(Screen.Satellites, Screen.Passes, Screen.Radar, Screen.Map, Screen.Settings)
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination?.route
    val startDestination = Screen.Passes.route

    // Observe radio tracking state for the status bar
    val context = LocalContext.current
    val container = (context.applicationContext as IContainerProvider).getMainContainer()
    val trackingState by container.radioTrackingService.state.collectAsStateWithLifecycle()

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
        Column {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition =  { fadeIn(animationSpec = tween(350)) },
            exitTransition = { fadeOut(animationSpec = tween(350)) },
            modifier = Modifier.weight(1f)
        ) {
            satellitesDestination { navController.navigateUp() }
            passesDestination { catNum: Int, aosTime: Long ->
                val radarRoute = "${Screen.Radar.route}?catNum=${catNum}&aosTime=${aosTime}"
                navController.navigate(radarRoute)
            }
            radarDestination(
                navigateUp = { navController.navigateUp() },
                navigateToRadioControl = { catNum, aosTime ->
                    val route = "${Screen.RadioControl.route}?catNum=$catNum&aosTime=$aosTime"
                    navController.navigate(route)
                }
            )
            radioControlDestination { navController.navigateUp() }
            mapDestination()
            settingsDestination()
        }

        // Radio tracking status banner (above bottom navigation)
        if (trackingState.isActive) {
            val infiniteTransition = rememberInfiniteTransition(label = "trackingPulse")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 1f, targetValue = 0.4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ), label = "pulseAlpha"
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable {
                        val pass = trackingState.currentPass
                        if (pass != null) {
                            val route = "${Screen.RadioControl.route}?catNum=${pass.catNum}&aosTime=${pass.aosTime}"
                            navController.navigate(route)
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50).copy(alpha = alpha))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tracking: ${trackingState.currentPass?.name ?: ""}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                val txOk = if (trackingState.txConnected) "TX" else ""
                val rxOk = if (trackingState.rxConnected) "RX" else ""
                Text(
                    text = listOf(txOk, rxOk).filter { it.isNotBlank() }.joinToString("/"),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        } // end Column
    }
}
