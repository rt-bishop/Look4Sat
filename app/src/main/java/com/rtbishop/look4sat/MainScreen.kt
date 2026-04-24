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
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.rtbishop.look4sat.core.domain.repository.IContainerProvider
import com.rtbishop.look4sat.core.presentation.Screen
import com.rtbishop.look4sat.core.presentation.hasEnoughHeight
import com.rtbishop.look4sat.core.presentation.hasEnoughWidth
import com.rtbishop.look4sat.feature.map.MapDestination
import com.rtbishop.look4sat.feature.passes.PassesDestination
import com.rtbishop.look4sat.feature.radar.RadarDestination
import com.rtbishop.look4sat.feature.radiocontrol.RadioControlDestination
import com.rtbishop.look4sat.feature.satellites.SatellitesDestination
import com.rtbishop.look4sat.feature.settings.SettingsDestination

@Composable
fun MainScreen() {
    val backStack = rememberNavBackStack(Screen.Passes)
    val currentKey = backStack.lastOrNull()
    val navigateBack: () -> Unit = { backStack.removeAt(backStack.size - 1) }
    val fadeTransition = fadeIn(animationSpec = tween(350)) togetherWith fadeOut(animationSpec = tween(350))
    val navItems = listOf(Screen.Satellites, Screen.Passes, Screen.Radar(), Screen.Map, Screen.Settings)

    val context = LocalContext.current
    val container = (context.applicationContext as IContainerProvider).getMainContainer()
    val trackingState by container.radioTrackingService.state.collectAsStateWithLifecycle()

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            navItems.forEach { screen ->
                val isSelected = when (currentKey) {
                    is Screen.Satellites -> screen is Screen.Satellites
                    is Screen.Passes -> screen is Screen.Passes
                    is Screen.Radar -> screen is Screen.Radar
                    is Screen.Map -> screen is Screen.Map
                    is Screen.Settings -> screen is Screen.Settings
                    else -> false
                }
                item(
                    icon = { Icon(painterResource(screen.iconResId), stringResource(screen.titleResId)) },
                    label = { Text(stringResource(screen.titleResId)) },
                    selected = isSelected,
                    onClick = {
                        if (isSelected) return@item
                        while (backStack.size > 1) backStack.removeAt(backStack.size - 1)
                        if (screen !is Screen.Passes) backStack.add(screen)
                    }
                )
            }
        },
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationRailContainerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        layoutType = when {
            !hasEnoughHeight() && hasEnoughWidth() -> NavigationSuiteType.NavigationRail
            !hasEnoughWidth() -> NavigationSuiteType.ShortNavigationBarCompact
            else -> NavigationSuiteType.ShortNavigationBarMedium
        }
    ) {
        Column {
            NavDisplay(
                backStack = backStack,
                modifier = Modifier.weight(1f),
                onBack = navigateBack,
                transitionSpec = { fadeTransition },
                popTransitionSpec = { fadeTransition },
                predictivePopTransitionSpec = { fadeTransition },
                entryProvider = entryProvider {
                    entry<Screen.Satellites> {
                        SatellitesDestination(navigateUp = navigateBack)
                    }
                    entry<Screen.Passes> {
                        PassesDestination { catNum, aosTime ->
                            backStack.add(Screen.Radar(catNum, aosTime))
                        }
                    }
                    entry<Screen.Radar> { route ->
                        RadarDestination(
                            catNum = route.catNum,
                            aosTime = route.aosTime,
                            navigateUp = navigateBack,
                            navigateToRadioControl = { catNum, aosTime ->
                                backStack.add(Screen.RadioControl(catNum, aosTime))
                            }
                        )
                    }
                    entry<Screen.RadioControl> { route ->
                        RadioControlDestination(
                            catNum = route.catNum,
                            aosTime = route.aosTime,
                            navigateUp = navigateBack
                        )
                    }
                    entry<Screen.Map> {
                        MapDestination()
                    }
                    entry<Screen.Settings> {
                        SettingsDestination()
                    }
                }
            )
            // Radio tracking status banner
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
                                backStack.add(Screen.RadioControl(pass.catNum, pass.aosTime))
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
        }
    }
}
