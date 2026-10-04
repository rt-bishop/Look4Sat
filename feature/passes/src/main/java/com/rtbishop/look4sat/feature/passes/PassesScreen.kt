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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
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
import com.rtbishop.look4sat.core.domain.repository.IContainerProvider
import com.rtbishop.look4sat.core.domain.utility.toTimerString
import com.rtbishop.look4sat.core.presentation.EmptyListCard
import com.rtbishop.look4sat.core.presentation.IconCard
import com.rtbishop.look4sat.core.presentation.MainTheme
import com.rtbishop.look4sat.core.presentation.NextPassRow
import com.rtbishop.look4sat.core.presentation.R
import com.rtbishop.look4sat.core.presentation.ScreenColumn
import com.rtbishop.look4sat.core.presentation.SearchBar
import com.rtbishop.look4sat.core.presentation.TopBar
import com.rtbishop.look4sat.core.presentation.WhatsNewDialog
import com.rtbishop.look4sat.core.presentation.elevationColor
import com.rtbishop.look4sat.core.presentation.infiniteMarquee
import com.rtbishop.look4sat.core.presentation.isVerticalLayout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun PassesDestination(navigateToRadar: (Int, Long) -> Unit) {
    val context = LocalContext.current
    val container = (context.applicationContext as IContainerProvider).getMainContainer()
    val viewModel: PassesViewModel = viewModel(factory = PassesViewModel.factory(container))
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    PassesScreen(uiState, viewModel.timeNow, viewModel::onAction, navigateToRadar)
}

@Composable
private fun PassesScreen(
    uiState: PassesState,
    timeNow: StateFlow<Long>,
    onAction: (PassesAction) -> Unit,
    navigateToRadar: (Int, Long) -> Unit
) {
    if (uiState.isPassesDialogShown) {
        PassesFilterDialog(
            hours = uiState.hours,
            elevation = uiState.elevation,
            lowElevation = uiState.lowElevation,
            highElevation = uiState.highElevation,
            aosStartMinute = uiState.aosStartMinute,
            aosEndMinute = uiState.aosEndMinute,
            invertAosTimeWindow = uiState.invertAosTimeWindow,
            showDeepSpace = uiState.showDeepSpace,
            cancel = { onAction(PassesAction.TogglePassesDialog) },
            accept = { params ->
                onAction(
                    PassesAction.FilterPasses(
                        hoursAhead = params.hours,
                        minElevation = params.elevation,
                        lowElevation = params.lowElevation,
                        highElevation = params.highElevation,
                        aosStartMinute = params.aosStartMinute,
                        aosEndMinute = params.aosEndMinute,
                        invertAosTimeWindow = params.invertAosTimeWindow,
                        showDeepSpace = params.showDeepSpace
                    )
                )
            }
        )
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
        val dismiss = { onAction(PassesAction.DismissWhatsNew) }
        WhatsNewDialog(onDismiss = dismiss)
    }
    ScreenColumn(
        topBar = { isVerticalLayout ->
            TopBar(
                isVerticalLayout = isVerticalLayout,
                startAction = {
                    IconCard(action = { onAction(PassesAction.ToggleRadiosDialog) }, resId = R.drawable.ic_radios)
                },
                topInfo = {
                    SearchBar(
                        onQueryChange = { onAction(PassesAction.SearchFor(it)) },
                        modifier = Modifier.weight(1f)
                    )
                },
                bottomInfo = {
                    NextPassRow(pass = uiState.nextPass, isUtc = uiState.isUtc)
                },
                endAction = {
                    IconCard(action = { onAction(PassesAction.TogglePassesDialog) }, resId = R.drawable.ic_filter)
                }
            )
        }
    ) { _ ->
        PassesList(
            isRefreshing = uiState.isRefreshing,
            isUtc = uiState.isUtc,
            passes = uiState.itemsList,
            groupedPasses = uiState.groupedPasses,
            sunTimes = uiState.sunTimes,
            isSearching = uiState.searchQuery.isNotBlank(),
            timeNow = timeNow,
            navigateToRadar = navigateToRadar,
            onAction = onAction
        )
    }
}

@Composable
private fun PassesList(
    isRefreshing: Boolean,
    isUtc: Boolean,
    passes: List<OrbitalPass>,
    groupedPasses: Map<String, List<OrbitalPass>>,
    sunTimes: Map<String, Pair<String, String>>,
    isSearching: Boolean,
    timeNow: StateFlow<Long>,
    navigateToRadar: (Int, Long) -> Unit,
    onAction: (PassesAction) -> Unit
) {
    val isVerticalLayout = isVerticalLayout()
    val refreshState = rememberPullToRefreshState()
    ElevatedCard(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            state = refreshState,
            onRefresh = { onAction(PassesAction.RefreshPasses) },
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
                val emptyMessage = if (isSearching) {
                    stringResource(R.string.pass_empty_search_message)
                } else {
                    stringResource(R.string.pass_empty_list_message)
                }
                EmptyListCard(message = emptyMessage)
            } else {
                LazyVerticalGrid(columns = GridCells.Adaptive(320.dp), modifier = Modifier.fillMaxSize()) {
                    for ((dateLabel, dayPasses) in groupedPasses) {
                        stickyHeader(key = "header_$dateLabel") {
                            val (rise, set) = sunTimes[dateLabel] ?: ("--:--" to "--:--")
                            StickyDateHeader(label = dateLabel, sunriseTime = rise, sunsetTime = set)
                        }
                        items(items = dayPasses, key = { item -> "${item.catNum}_${item.aosTime}" }) { pass ->
                            PassItem(
                                pass = pass,
                                navigateToRadar = navigateToRadar,
                                timeNow = timeNow,
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

internal fun displayLocale(): Locale {
    val locale = Locale.getDefault()
    return if (locale.language == Locale.CHINESE.language) locale else Locale.ENGLISH
}

@Preview(showBackground = true)
@Composable
private fun DeepSpacePassPreview() {
    val data = OrbitalData("Satellite", 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 45000, 0.0)
    val satellite = DeepSpaceObject(data)
    val pass = OrbitalPass(1L, 180.0, 10L, 180.0, 36650, 45.0, satellite, 0.5f)
    MainTheme { PassItem(pass = pass, { _, _ -> }, MutableStateFlow(0L)) }
}

@Preview(showBackground = true)
@Composable
private fun UpcomingPassPreview() {
    val data = OrbitalData("Satellite", 0.0, 15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 45000, 0.0)
    val aosTime = 1767225600000L
    val pass = OrbitalPass(aosTime, 180.0, aosTime + 702000L, 360.0, 36650, 45.0, NearEarthObject(data), 0f)
    MainTheme { PassItem(pass = pass, { _, _ -> }, MutableStateFlow(aosTime - 866000L)) }
}

@Preview(showBackground = true)
@Composable
private fun ActivePassPreview() {
    val data = OrbitalData("Satellite", 0.0, 15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 45000, 0.0)
    val aosTime = 1767225600000L
    val pass = OrbitalPass(aosTime, 180.0, aosTime + 702000L, 360.0, 36650, 45.0, NearEarthObject(data), 0.4f)
    MainTheme { PassItem(pass = pass, { _, _ -> }, MutableStateFlow(aosTime + 280000L)) }
}

/**
 * Countdown to AOS while the pass is pending, then to LOS while it is active.
 * Gray denotes a pending pass, primary an active one. Collects the clock itself so a
 * tick recomposes only this chip.
 */
@Composable
private fun PassTimerChip(pass: OrbitalPass, timeNow: StateFlow<Long>) {
    val now by timeNow.collectAsStateWithLifecycle()
    // A DeepSpace object is permanently in view, so it gets a neutral label instead of a countdown
    val isActive = !pass.isDeepSpace && now >= pass.aosTime
    val text = when {
        pass.isDeepSpace -> stringResource(R.string.pass_deep_space)
        isActive -> stringResource(R.string.pass_timer_los, (pass.losTime - now).toTimerString())
        else -> stringResource(R.string.pass_timer_aos, (pass.aosTime - now).toTimerString())
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isActive) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            style = LocalTextStyle.current.copy(
                // Tabular figures stop the chip reflowing on every tick, while the trimmed
                // line height keeps it short without shrinking the timer text
                fontFeatureSettings = "tnum",
                lineHeight = 16.sp,
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both
                )
            ),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/** Elapsed fraction of the pass. Collects the clock itself to keep ticks off the text rows. */
@Composable
private fun PassProgressBar(pass: OrbitalPass, timeNow: StateFlow<Long>, modifier: Modifier = Modifier) {
    val now by timeNow.collectAsStateWithLifecycle()
    LinearProgressIndicator(
        progress = { pass.progressAt(now) },
        drawStopIndicator = {},
        modifier = modifier
    )
}

@Composable
private fun PassItem(
    pass: OrbitalPass,
    navigateToRadar: (Int, Long) -> Unit,
    timeNow: StateFlow<Long>,
    modifier: Modifier = Modifier,
    isVerticalLayout: Boolean = true,
    isUtc: Boolean = false
) {
    val passSatId = stringResource(id = R.string.pass_satId, pass.catNum)
    val horizontalPadding = if (isVerticalLayout) 6.dp else 12.dp
    val timeZone = remember(isUtc) {
        if (isUtc) TimeZone.getTimeZone("UTC") else TimeZone.getDefault()
    }
    val sdfTime = remember(isUtc) {
        SimpleDateFormat("HH:mm:ss", displayLocale()).also { it.timeZone = timeZone }
    }
    val azimuthFormat = stringResource(id = R.string.pass_azimuth)
    val aosTimeStr = remember(pass.aosTime, isUtc) { sdfTime.format(Date(pass.aosTime)) }
    val losTimeStr = remember(pass.losTime, isUtc) { sdfTime.format(Date(pass.losTime)) }
    val aosAzStr = remember(pass.aosAzimuth, azimuthFormat) {
        String.format(displayLocale(), azimuthFormat, pass.aosAzimuth.toInt() % 360)
    }
    val losAzStr = remember(pass.losAzimuth, azimuthFormat) {
        String.format(displayLocale(), azimuthFormat, pass.losAzimuth.toInt() % 360)
    }
    val durationStr = remember(pass.aosTime, pass.losTime) {
        val seconds = (pass.losTime - pass.aosTime) / 1000
        "${seconds / 60}m ${seconds % 60}s"
    }
//    do not delete
//    val progressTint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
//    val progressFraction = if (pass.isDeepSpace) 0f else pass.progress.coerceIn(0f, 1f)

    Column(
        modifier = modifier.clickable { navigateToRadar(pass.catNum, pass.aosTime) }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.surface)
//                do not delete
//                .drawBehind { drawRect(progressTint, size = Size(size.width * progressFraction, size.height)) }
                .padding(horizontal = horizontalPadding, vertical = 6.dp)
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
                PassTimerChip(pass = pass, timeNow = timeNow)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (pass.isDeepSpace) {
                        stringResource(R.string.pass_duration_placeholder)
                    } else {
                        durationStr
                    },
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                // AOS azimuth, peak elevation and LOS azimuth read as the arc of the pass
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = aosAzStr,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val elevColor = elevationColor(pass.maxElevation)
                        Icon(
                            painter = painterResource(id = R.drawable.ic_elevation),
                            contentDescription = null,
                            tint = elevColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${pass.maxElevation}°",
                            color = elevColor
                        )
                    }
                    Text(
                        text = losAzStr,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "${pass.altitude} km",
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }
            // A DeepSpace object has no AOS/LOS to count between, so it drops the progress row
            if (!pass.isDeepSpace) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = aosTimeStr,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    PassProgressBar(
                        pass = pass,
                        timeNow = timeNow,
                        modifier = Modifier.fillMaxWidth(0.75f)
                    )
                    Text(
                        text = losTimeStr,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.background)
    }
}
