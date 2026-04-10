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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.rtbishop.look4sat.core.presentation.LocalSpacing
import com.rtbishop.look4sat.core.presentation.MainTheme
import com.rtbishop.look4sat.core.presentation.R
import com.rtbishop.look4sat.core.presentation.SharedDialog

private val allModes = listOf(
    "AFSK", "AFSK S-Net", "AFSK SALSAT", "AHRPT", "AM", "APT", "BPSK", "BPSK PMT-A3",
    "CERTO", "CW", "DQPSK", "DSTAR", "DUV", "FFSK", "FM", "FMN", "FSK", "FSK AX.100 Mode 5",
    "FSK AX.100 Mode 6", "FSK AX.25 G3RUH", "GFSK", "GFSK Rktr", "GMSK", "HRPT", "LoRa",
    "LRPT", "LSB", "MFSK", "MSK", "MSK AX.100 Mode 5", "MSK AX.100 Mode 6", "OFDM", "OQPSK",
    "PSK", "PSK31", "PSK63", "QPSK", "QPSK31", "QPSK63", "SSTV", "USB", "WSJT"
)

private val hourSteps = listOf(1, 2, 4, 8, 12, 24, 48, 72, 96, 120, 144, 168, 192, 216, 240)

@Preview
@Composable
private fun PassesDialogPreview() {
    MainTheme { PassesDialog(24, 16.0, {}) { _, _ -> } }
}

@Composable
internal fun PassesDialog(hours: Int, elevation: Double, cancel: () -> Unit, accept: (Int, Double) -> Unit) {
    val hoursIndex = remember { mutableIntStateOf(hourSteps.indexOfFirst { it >= hours }.coerceAtLeast(0)) }
    val elevationValueNew = remember { mutableDoubleStateOf(elevation) }
    val onAccept = {
        accept(hourSteps[hoursIndex.intValue], elevationValueNew.doubleValue).also { cancel() }
    }
    SharedDialog(title = stringResource(R.string.pass_filter_title), onCancel = cancel, onAccept = onAccept) {
        SliderRow(
            title = stringResource(R.string.pass_filter_elev),
            value = elevationValueNew.doubleValue,
            displayValue = "${elevationValueNew.doubleValue.toInt()}°",
            valueResId = R.drawable.ic_elevation,
            valueRange = 0f..60f
        ) { elevationValueNew.doubleValue = it.toDouble() }
        SliderRow(
            title = stringResource(R.string.pass_filter_hours),
            value = hoursIndex.intValue.toDouble(),
            displayValue = "${hourSteps[hoursIndex.intValue]}h",
            valueResId = R.drawable.ic_clock,
            valueRange = 0f..(hourSteps.size - 1).toFloat(),
            steps = hourSteps.size - 2
        ) { hoursIndex.intValue = it.toInt().coerceIn(0, hourSteps.size - 1) }
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
    onChange: (Float) -> Unit
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
        Slider(value = value.toFloat(), onValueChange = onChange, valueRange = valueRange, steps = steps)
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
    SharedDialog(title = stringResource(R.string.pass_modes_title), onCancel = cancel, onAccept = onAccept) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(240.dp),
            modifier = Modifier
                .fillMaxHeight(0.69f)
                .background(MaterialTheme.colorScheme.background),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            itemsIndexed(allModes) { index, item ->
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
