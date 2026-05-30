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
package com.rtbishop.look4sat.feature.radar

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rtbishop.look4sat.core.domain.predict.OrbitalPos
import com.rtbishop.look4sat.core.domain.repository.IContainerProvider
import com.rtbishop.look4sat.core.domain.utility.toDegrees
import com.rtbishop.look4sat.core.presentation.EmptyListCard
import com.rtbishop.look4sat.core.presentation.IconCard
import com.rtbishop.look4sat.core.presentation.NextPassRow
import com.rtbishop.look4sat.core.presentation.R
import com.rtbishop.look4sat.core.presentation.TimerRow
import com.rtbishop.look4sat.core.presentation.TopBar
import com.rtbishop.look4sat.core.presentation.getDefaultPass
import com.rtbishop.look4sat.core.presentation.isVerticalLayout
import com.rtbishop.look4sat.core.presentation.layoutPadding
import kotlinx.coroutines.launch

private enum class RadarPage(val title: String) {
    Transceivers("Transceivers"),
    Sstv("SSTV")
}

@Composable
fun RadarDestination(navigateUp: () -> Unit) {
    val context = LocalContext.current
    val container = (context.applicationContext as IContainerProvider).getMainContainer()
    val viewModel: RadarViewModel = viewModel(factory = RadarViewModel.factory(container))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RadarScreen(uiState, viewModel::onAction, navigateUp)
}

@Composable
private fun RadarScreen(
    uiState: RadarState,
    onAction: (RadarAction) -> Unit,
    navigateUp: () -> Unit
) {
    val upcomingPass = uiState.currentPass ?: getDefaultPass()
    val addToCalendar: () -> Unit = {
        uiState.currentPass?.let { onAction(RadarAction.AddToCalendar(it.name, it.aosTime, it.losTime)) }
    }
    Column(
        modifier = Modifier
            .layoutPadding()
            .keepScreenOn(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val isVertical = isVerticalLayout()
        if (isVertical) {
            TopBar {
                IconCard(action = navigateUp, resId = R.drawable.ic_back)
                TimerRow(timeString = uiState.currentTime, isTimeAos = uiState.isTimeAos)
                IconCard(action = addToCalendar, resId = R.drawable.ic_calendar)
            }
            TopBar { NextPassRow(pass = upcomingPass, isUtc = uiState.isUtc) }
        } else {
            TopBar {
                IconCard(action = navigateUp, resId = R.drawable.ic_back)
                TimerRow(timeString = uiState.currentTime, isTimeAos = uiState.isTimeAos)
                NextPassRow(pass = upcomingPass, modifier = Modifier.weight(1f), isUtc = uiState.isUtc)
                IconCard(action = addToCalendar, resId = R.drawable.ic_calendar)
            }
        }
        if (isVertical) {
            RadarCard(uiState, Modifier.weight(1f))
            PagerCard(uiState, onAction, Modifier.weight(1f))
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RadarCard(uiState, Modifier.weight(1f))
                PagerCard(uiState, onAction, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PagerCard(
    uiState: RadarState,
    onAction: (RadarAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val pages = RadarPage.entries
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    ElevatedCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                pages.forEachIndexed { index, page ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(text = page.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                when (pages[pageIndex]) {
                    RadarPage.Transceivers -> TransceiversPage(
                        transceivers = uiState.transmitters,
                        selectedUuid = uiState.selectedTransmitterUuid,
                        radioControl = uiState.radioControl,
                        onAction = onAction
                    )
                    RadarPage.Sstv -> SstvPlaceholderPage()
                }
            }
        }
    }
}

@Composable
private fun SstvPlaceholderPage() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "🚧", fontSize = 48.sp)
            Text(
                text = "SSTV Decoder",
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Coming soon",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RadarCard(uiState: RadarState, modifier: Modifier = Modifier) {
    val satellitePos = uiState.orbitalPos
    val borderModifier = if (satellitePos?.aboveHorizon == true && satellitePos.eclipsed) {
        val infiniteTransition = rememberInfiniteTransition(label = "eclipsedBorder")
        val borderAlpha by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 0.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, delayMillis = 25, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "eclipsedBorderAlpha"
        )
        Modifier.border(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha),
            shape = MaterialTheme.shapes.medium
        )
    } else {
        Modifier
    }
    ElevatedCard(modifier = modifier.then(borderModifier)) {
        Box(contentAlignment = Alignment.Center) {
            val position = uiState.orbitalPos
            if (position == null) {
                ElevatedCard(modifier = Modifier.fillMaxSize()) {
                    EmptyListCard(message = "")
                }
            } else {
                RadarViewCompose(
                    item = position,
                    items = uiState.satTrack,
                    azimElev = uiState.orientationValues,
                    shouldShowSweep = uiState.shouldShowSweep,
                    shouldUseCompass = uiState.shouldUseCompass,
                    modifier = Modifier.align(Alignment.Center),
                    sunPosition = uiState.sunPosition,
                    moonPosition = uiState.moonPosition,
                )
                PositionOverlay(position)
            }
        }
    }
}

@Composable
private fun PositionOverlay(position: OrbitalPos) {
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            RadarLabel(
                value = stringResource(R.string.radar_az_value, position.azimuth.toDegrees()),
                label = stringResource(R.string.radar_az_text),
                alignment = Alignment.Start,
                labelFirst = false
            )
            RadarLabel(
                value = stringResource(R.string.radar_az_value, position.elevation.toDegrees()),
                label = stringResource(R.string.radar_el_text),
                alignment = Alignment.End,
                labelFirst = false
            )
        }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            RadarLabel(
                value = stringResource(R.string.radar_alt_value, position.altitude),
                label = stringResource(R.string.radar_alt_text),
                alignment = Alignment.Start,
                labelFirst = true
            )
            RadarLabel(
                value = stringResource(R.string.radar_alt_value, position.distance),
                label = stringResource(R.string.radar_dist_text),
                alignment = Alignment.End,
                labelFirst = true
            )
        }
    }
}

@Composable
private fun RadarLabel(
    value: String,
    label: String,
    alignment: Alignment.Horizontal,
    labelFirst: Boolean
) {
    Column(horizontalAlignment = alignment) {
        if (labelFirst) {
            Text(text = label, fontSize = 15.sp)
            Text(text = value, fontSize = 18.sp)
        } else {
            Text(text = value, fontSize = 18.sp)
            Text(text = label, fontSize = 15.sp)
        }
    }
}

