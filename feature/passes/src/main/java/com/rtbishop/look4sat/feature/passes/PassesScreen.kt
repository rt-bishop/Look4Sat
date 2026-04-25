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
package com.rtbishop.look4sat.feature.passes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rtbishop.look4sat.core.domain.predict.DeepSpaceObject
import com.rtbishop.look4sat.core.domain.predict.NearEarthObject
import com.rtbishop.look4sat.core.domain.predict.OrbitalData
import com.rtbishop.look4sat.core.domain.predict.OrbitalPass
import com.rtbishop.look4sat.core.presentation.EmptyListCard
import com.rtbishop.look4sat.core.presentation.IconCard
import com.rtbishop.look4sat.core.presentation.InfoDialog
import com.rtbishop.look4sat.core.presentation.MainTheme
import com.rtbishop.look4sat.core.presentation.NextPassRow
import com.rtbishop.look4sat.core.presentation.R
import com.rtbishop.look4sat.core.presentation.ScreenColumn
import com.rtbishop.look4sat.core.presentation.TimerRow
import com.rtbishop.look4sat.core.presentation.TopBar
import com.rtbishop.look4sat.core.presentation.infiniteMarquee
import com.rtbishop.look4sat.core.presentation.isVerticalLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun PassesDestination(navigateToRadar: (Int, Long) -> Unit) {
    val viewModel = viewModel(
        modelClass = PassesViewModel::class.java,
        factory = PassesViewModel.Factory
    )
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    PassesScreen(uiState, viewModel::onAction, navigateToRadar)
}

@Composable
private fun PassesScreen(
    uiState: PassesState,
    onAction: (PassesAction) -> Unit,
    navigateToRadar: (Int, Long) -> Unit
) {
    if (uiState.isPassesDialogShown) {
        PassesDialog(
            hours = uiState.hours,
            elevation = uiState.elevation,
            showDeepSpace = uiState.showDeepSpace,
            cancel = { onAction(PassesAction.TogglePassesDialog) }
        ) { hours, elevation, showDeepSpace ->
            onAction(PassesAction.FilterPasses(hours, elevation, showDeepSpace))
        }
    }
    if (uiState.isRadiosDialogShown) {
        RadiosDialog(
            modes = uiState.modes,
            cancel = { onAction(PassesAction.ToggleRadiosDialog) }
        ) { modes ->
            onAction(PassesAction.FilterRadios(modes))
        }
    }
    if (uiState.shouldSeeWhatsNew) {
        InfoDialog(
            title = stringResource(R.string.pass_whatsnew_title),
            text = stringResource(R.string.pass_whatsnew_message)
        ) {
            onAction(PassesAction.DismissWhatsNew)
        }
    }
    val gridState = rememberLazyGridState()
    ScreenColumn(
        topBar = { isVerticalLayout ->
            TopBar(
                isVerticalLayout = isVerticalLayout,
                startAction = {
                    IconCard(action = { onAction(PassesAction.TogglePassesDialog) }, resId = R.drawable.ic_filter)
                },
                topInfo = {
                    TimerRow(timeString = uiState.nextTime, isTimeAos = uiState.isNextTimeAos)
                },
                bottomInfo = {
                    NextPassRow(pass = uiState.nextPass, isUtc = uiState.isUtc)
                },
                endAction = {
                    IconCard(action = { onAction(PassesAction.ToggleRadiosDialog) }, resId = R.drawable.ic_radios)
                }
            )
        }
    ) { _ ->
        PassesList(
            isRefreshing = uiState.isRefreshing,
            isUtc = uiState.isUtc,
            passes = uiState.itemsList,
            navigateToRadar = navigateToRadar,
            refreshPasses = { onAction(PassesAction.RefreshPasses) },
            gridState = gridState,
            sunTimes = uiState.sunTimes
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PassesList(
    isRefreshing: Boolean,
    isUtc: Boolean,
    passes: List<OrbitalPass>,
    navigateToRadar: (Int, Long) -> Unit,
    refreshPasses: () -> Unit,
    gridState: LazyGridState,
    sunTimes: Map<String, Pair<String, String>>
) {
    val isVerticalLayout = isVerticalLayout()
    val refreshState = rememberPullToRefreshState()
    val timeZone = remember(isUtc) { if (isUtc) TimeZone.getTimeZone("UTC") else TimeZone.getDefault() }
    val sdfDate = remember(isUtc) {
        SimpleDateFormat("EEE, dd MMM yyyy", Locale.ENGLISH).also { it.timeZone = timeZone }
    }

    // Group passes by calendar day so we know where headers go
    val groupedPasses = remember(passes, isUtc) {
        passes.filter { !it.isDeepSpace }.groupBy { sdfDate.format(Date(it.aosTime)) } +
            if (passes.any { it.isDeepSpace }) mapOf("Deep Space" to passes.filter { it.isDeepSpace }) else emptyMap()
    }

    // Derive the sticky header label from the first visible item index
    val stickyHeader by remember(gridState, groupedPasses) {
        derivedStateOf {
            val firstVisible = gridState.firstVisibleItemIndex
            var itemIndex = 0
            var header = groupedPasses.keys.firstOrNull() ?: ""
            for ((key, items) in groupedPasses) {
                header = key
                itemIndex += 1 + items.size
                if (itemIndex > firstVisible) break
            }
            header
        }
    }

    // The sticky overlay should only show when the first in-list header has scrolled out of view.
    // firstVisibleItemIndex == 0 means the first header is still visible — hide the overlay.
    val showStickyOverlay by remember(gridState) {
        derivedStateOf { gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 0 }
    }

    ElevatedCard(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            state = refreshState,
            onRefresh = refreshPasses,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = refreshState,
                    isRefreshing = isRefreshing,
                    color = MaterialTheme.colorScheme.background,
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        ) {
            if (passes.isEmpty()) {
                EmptyListCard(message = stringResource(R.string.pass_empty_list_message))
            } else {
                Column {
                    // Sticky header overlay — only visible once the in-list header scrolls away
                    if (showStickyOverlay) {
                        val (rise, set) = sunTimes[stickyHeader] ?: ("--:--" to "--:--")
                        StickyDateHeader(label = stickyHeader, sunriseTime = rise, sunsetTime = set)
                    }
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(320.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        for ((dateLabel, dayPasses) in groupedPasses) {
                            // In-list section header (scrolls away, drives the sticky overlay)
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                val (rise, set) = sunTimes[dateLabel] ?: ("--:--" to "--:--")
                                StickyDateHeader(label = dateLabel, sunriseTime = rise, sunsetTime = set)
                            }
                            items(items = dayPasses, key = { item -> item.catNum + item.aosTime }) { pass ->
                                PassItem(
                                    pass = pass,
                                    navigateToRadar = navigateToRadar,
                                    modifier = Modifier.animateItem(),
                                    isVerticalLayout = isVerticalLayout,
                                    isUtc = isUtc
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StickyDateHeader(label: String, sunriseTime: String, sunsetTime: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.primary
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_sun),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(text = sunriseTime, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_moon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
                Text(text = sunsetTime, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DeepSpacePassPreview() {
    val data = OrbitalData("Satellite", 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 45000, 0.0)
    val satellite = DeepSpaceObject(data)
    val pass = OrbitalPass(1L, 180.0, 10L, 360.0, 36650, 45.0, satellite, 0.5f)
    MainTheme { PassItem(pass = pass, { _, _ -> }) }
}

@Preview(showBackground = true)
@Composable
private fun NearEarthPassPreview() {
    val data = OrbitalData("Satellite", 0.0, 15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 45000, 0.0)
    val satellite = NearEarthObject(data)
    val pass = OrbitalPass(1L, 180.0, 10L, 360.0, 36650, 45.0, satellite, 0.5f)
    MainTheme { PassItem(pass = pass, { _, _ -> }) }
}

@Composable
private fun PassItem(
    pass: OrbitalPass,
    navigateToRadar: (Int, Long) -> Unit,
    modifier: Modifier = Modifier,
    isVerticalLayout: Boolean = true,
    isUtc: Boolean = false
) {
    val passSatId = stringResource(id = R.string.pass_satId, pass.catNum)
    val horizontalPadding = if (isVerticalLayout) 6.dp else 10.dp
    val timeZone = remember(isUtc) {
        if (isUtc) TimeZone.getTimeZone("UTC") else TimeZone.getDefault()
    }
    val sdfTime = remember(isUtc) {
        SimpleDateFormat("HH:mm:ss", Locale.ENGLISH).also { it.timeZone = timeZone }
    }
    val aosTimeStr = remember(pass.aosTime, isUtc) { sdfTime.format(Date(pass.aosTime)) }
    val losTimeStr = remember(pass.losTime, isUtc) { sdfTime.format(Date(pass.losTime)) }
    val durationStr = remember(pass.aosTime, pass.losTime) {
        val seconds = (pass.losTime - pass.aosTime) / 1000
        "${seconds / 60}m ${seconds % 60}s"
    }

    Column(
        modifier = modifier.clickable { navigateToRadar(pass.catNum, pass.aosTime) }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(1.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.surface)
                .padding(horizontal = horizontalPadding, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$passSatId - ",
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = pass.name,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp)
                        .infiniteMarquee(),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_elevation),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${pass.maxElevation}°",
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (pass.isDeepSpace) {
                        Text(
                            text = stringResource(R.string.pass_deep_space),
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Text(text = durationStr, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_altitude),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${pass.altitude} km", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            id = R.string.pass_aosLos,
                            pass.aosAzimuth.toInt(),
                            pass.losAzimuth.toInt()
                        ),
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                val defaultTime = "   - - : - -   "
                Text(
                    text = if (pass.isDeepSpace) defaultTime else aosTimeStr,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                LinearProgressIndicator(
                    progress = { if (pass.isDeepSpace) 100f else pass.progress },
                    drawStopIndicator = {},
                    modifier = Modifier.fillMaxWidth(0.75f)
                )
                Text(
                    text = if (pass.isDeepSpace) defaultTime else losTimeStr,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.background)
    }
}
