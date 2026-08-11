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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rtbishop.look4sat.core.presentation.LocalSpacing
import com.rtbishop.look4sat.core.presentation.MainTheme
import com.rtbishop.look4sat.core.presentation.R
import com.rtbishop.look4sat.core.presentation.ConfirmDialog
import com.rtbishop.look4sat.core.presentation.ElevationHighColor
import com.rtbishop.look4sat.core.presentation.ElevationLowColor
import com.rtbishop.look4sat.core.presentation.elevationColor
import com.rtbishop.look4sat.core.domain.source.Sources
import kotlin.math.roundToInt

private val hourSteps = listOf(1, 2, 4, 8, 12, 24, 48, 72, 96, 120, 144, 168, 192, 216, 240)
private const val dayMinutes = 24 * 60
private const val minuteStep = 15
private const val endOfDayMinute = dayMinutes - 1
private const val quarterHourSlots = dayMinutes / minuteStep

data class PassFilterParams(
    val hours: Int,
    val elevation: Double,
    val lowElevation: Double,
    val highElevation: Double,
    val aosStartMinute: Int,
    val aosEndMinute: Int,
    val invertAosTimeWindow: Boolean,
    val showDeepSpace: Boolean
)

@Preview
@Composable
private fun PassesDialogPreview() {
    MainTheme {
        PassesFilterDialog(
            hours = 24,
            elevation = 16.0,
            lowElevation = 16.0,
            highElevation = 65.0,
            aosStartMinute = 0,
            aosEndMinute = 23 * 60 + 59,
            invertAosTimeWindow = false,
            showDeepSpace = true,
            cancel = {},
            accept = {}
        )
    }
}

@Composable
internal fun PassesFilterDialog(
    hours: Int,
    elevation: Double,
    lowElevation: Double,
    highElevation: Double,
    aosStartMinute: Int,
    aosEndMinute: Int,
    invertAosTimeWindow: Boolean,
    showDeepSpace: Boolean,
    cancel: () -> Unit,
    accept: (PassFilterParams) -> Unit
) {
    val hoursIndex = remember { mutableIntStateOf(hourSteps.indexOfFirst { it >= hours }.coerceAtLeast(0)) }
    val elevationValueNew = remember { mutableDoubleStateOf(elevation) }
    val highlightBounds = 0f..90f
    var highlightRange by remember(lowElevation, highElevation) {
        mutableStateOf(lowElevation.toFloat()..highElevation.toFloat())
    }
    var aosRangeValue by remember {
        mutableStateOf(minuteToSlot(aosStartMinute).toFloat()..minuteToSlot(aosEndMinute).toFloat())
    }
    var invertedAosRange by remember { mutableStateOf(invertAosTimeWindow) }
    var deepSpaceEnabled by remember { mutableStateOf(showDeepSpace) }
    val onAccept = {
        accept(
            PassFilterParams(
                hours = hourSteps[hoursIndex.intValue],
                elevation = elevationValueNew.doubleValue,
                lowElevation = highlightRange.start.roundToInt().toDouble(),
                highElevation = highlightRange.endInclusive.roundToInt().toDouble(),
                aosStartMinute = slotToMinute(aosRangeValue.start),
                aosEndMinute = slotToMinute(aosRangeValue.endInclusive),
                invertAosTimeWindow = invertedAosRange,
                showDeepSpace = deepSpaceEnabled
            )
        )
        cancel()
    }
    ConfirmDialog(title = stringResource(R.string.pass_filter_title), onCancel = cancel, onAccept = onAccept) {
        SliderRow(
            title = stringResource(R.string.pass_filter_elev),
            value = elevationValueNew.doubleValue,
            displayValue = "${elevationValueNew.doubleValue.toInt()}°",
            valueResId = R.drawable.ic_elevation,
            valueRange = 0f..60f,
            accentColor = elevationColor(elevationValueNew.doubleValue)
        ) { elevationValueNew.doubleValue = it.toDouble() }
        SliderRow(
            title = stringResource(R.string.pass_filter_hours),
            value = hoursIndex.intValue.toDouble(),
            displayValue = formatHoursLabel(hourSteps[hoursIndex.intValue]),
            valueResId = R.drawable.ic_clock,
            valueRange = 0f..(hourSteps.size - 1).toFloat(),
            steps = hourSteps.size - 2
        ) { hoursIndex.intValue = it.toInt().coerceIn(0, hourSteps.size - 1) }
        ToggleRow(
            title = stringResource(R.string.pass_filter_deep_space),
            checked = deepSpaceEnabled,
            onCheckedChange = { deepSpaceEnabled = it }
        )
        ElevationColorsRangeSliderRow(
            title = stringResource(R.string.prefs_highlight_title),
            range = highlightRange,
            valueRange = highlightBounds
        ) { highlightRange = it }
        TimeRangeSliderRow(
            title = stringResource(R.string.pass_filter_aos_time),
            range = aosRangeValue,
            displayValue = formatTimeRange(aosRangeValue, invertedAosRange),
            valueResId = R.drawable.ic_clock,
            valueRange = 0f..quarterHourSlots.toFloat(),
        ) { aosRangeValue = it }
        ToggleRow(
            title = stringResource(R.string.pass_filter_invert_time),
            checked = invertedAosRange,
            onCheckedChange = { invertedAosRange = it }
        )
        Spacer(modifier = Modifier.height(0.dp))
    }
}

private fun slotToMinute(value: Float): Int {
    val slot = value.roundToInt().coerceIn(0, quarterHourSlots)
    return if (slot == quarterHourSlots) endOfDayMinute else slot * minuteStep
}

private fun minuteToSlot(minute: Int): Int {
    if (minute >= endOfDayMinute) return quarterHourSlots
    return ((minute + minuteStep / 2) / minuteStep).coerceIn(0, quarterHourSlots)
}

private fun formatMinuteOfDay(minute: Int): String {
    val hourPart = minute / 60
    val minutePart = minute % 60
    return "%02d:%02d".format(hourPart, minutePart)
}

private fun formatHoursLabel(hours: Int): String {
    if (hours < 24) return "${hours}h"
    val days = hours / 24
    val remainder = hours % 24
    return if (remainder == 0) "${days}d" else "${days}d ${remainder}h"
}

private fun formatTimeRange(
    range: ClosedFloatingPointRange<Float>,
    inverted: Boolean
): String {
    val start = formatMinuteOfDay(slotToMinute(range.start))
    val end = formatMinuteOfDay(slotToMinute(range.endInclusive))
    return if (inverted) "$end - $start" else "$start - $end"
}

@Composable
private fun SliderSection(
    title: String,
    trailingContent: @Composable () -> Unit,
    sliderContent: @Composable () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.small),
        modifier = Modifier.padding(horizontal = LocalSpacing.current.large)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface
            )
            trailingContent()
        }
        sliderContent()
    }
}

@Composable
private fun SliderRow(
    title: String,
    value: Double,
    displayValue: String,
    valueResId: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onChange: (Float) -> Unit
) {
    SliderSection(
        title = title,
        trailingContent = {
            Icon(
                painter = painterResource(id = valueResId),
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = displayValue,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = accentColor
            )
        }
    ) {
        Slider(value = value.toFloat(), onValueChange = onChange, valueRange = valueRange, steps = steps)
    }
}

@Composable
private fun TimeRangeSliderRow(
    title: String,
    range: ClosedFloatingPointRange<Float>,
    displayValue: String,
    valueResId: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    onChange: (ClosedFloatingPointRange<Float>) -> Unit
) {
    SliderSection(
        title = title,
        trailingContent = {
            Icon(
                painter = painterResource(id = valueResId),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = displayValue,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    ) {
        RangeSlider(
            value = range,
            onValueChange = { onChange(it.start..it.endInclusive) },
            valueRange = valueRange,
            steps = 0,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ElevationColorsRangeSliderRow(
    title: String,
    range: ClosedFloatingPointRange<Float>,
    valueRange: ClosedFloatingPointRange<Float>,
    onChange: (ClosedFloatingPointRange<Float>) -> Unit
) {
    val low = range.start.roundToInt()
    val high = range.endInclusive.roundToInt()
    SliderSection(
        title = title,
        trailingContent = {
            Text(
                text = "$low°",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = ElevationLowColor
            )
            Text(
                text = "..",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "$high°",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = ElevationHighColor
            )
        }
    ) {
        RangeSlider(
            value = range,
            onValueChange = { onChange(it.start..it.endInclusive) },
            valueRange = valueRange,
            steps = 0,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RadiosDialogPreview() {
    MainTheme { RadiosDialog(emptyList(), {}) { _ -> } }
}

@Composable
internal fun RadiosDialog(modes: List<String>, cancel: () -> Unit, accept: (List<String>) -> Unit) {
    val selected = remember { mutableStateOf(modes.toSet()) }
    val toggle = { mode: String ->
        selected.value = if (mode in selected.value) selected.value - mode else selected.value + mode
    }
    val onAccept = { accept(selected.value.toList()).also { cancel() } }
    ConfirmDialog(title = stringResource(R.string.pass_modes_title), onCancel = cancel, onAccept = onAccept) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(240.dp),
            modifier = Modifier
                .fillMaxHeight(0.69f)
                .background(MaterialTheme.colorScheme.background),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            itemsIndexed(Sources.satelliteModes) { index, item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { toggle(item) }
                ) {
                    Text(
                        text = "${index + 1}).",
                        modifier = Modifier.padding(start = 16.dp, end = 8.dp),
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = item,
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Checkbox(
                        checked = item in selected.value,
                        onCheckedChange = null,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = LocalSpacing.current.large)
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
