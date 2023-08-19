/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2022 Arty Bishop (bishop.arty@gmail.com)
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
package com.rtbishop.look4sat.presentation.passes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.presentation.MainTheme
import com.rtbishop.look4sat.presentation.components.CardButton

@Preview(showBackground = true)
@Composable
private fun FilterDialogPreview() {
    MainTheme { FilterDialog(24, 16.0, {}) { _, _ -> } }
}

@Composable
fun FilterDialog(hours: Int, elevation: Double, dismiss: () -> Unit, save: (Int, Double) -> Unit) {
    val hoursValue = rememberSaveable { mutableIntStateOf(hours) }
    val elevationValue = rememberSaveable { mutableIntStateOf(elevation.toInt()) }
    val maxWidthModifier = Modifier.fillMaxWidth(1f)
    Dialog(onDismissRequest = { dismiss() }) {
        ElevatedCard {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = maxWidthModifier.padding(12.dp)
            ) {
                Text(text = "Filter passes", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                    Text(text = "Show passes within", fontSize = 18.sp, modifier = Modifier.weight(1f))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_time),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(bottom = 4.dp)
                    )
                    Text(
                        text = "${hoursValue.intValue}h",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = hoursValue.intValue.toFloat(),
                    onValueChange = { hoursValue.intValue = it.toInt() },
                    valueRange = 1f..168f,
                    colors = SliderDefaults.colors(inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                    Text(text = "Show passes above", fontSize = 18.sp, modifier = Modifier.weight(1f))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_elevation),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(bottom = 4.dp)
                    )
                    Text(
                        text = "${elevationValue.intValue}°",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = elevationValue.intValue.toFloat(),
                    onValueChange = { elevationValue.intValue = it.toInt() },
                    valueRange = 0f..90f,
                    colors = SliderDefaults.colors(inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    CardButton(onClick = { dismiss() }, text = stringResource(id = R.string.btn_cancel))
                    CardButton(
                        onClick = {
                            save(hoursValue.intValue, elevationValue.intValue.toDouble())
                            dismiss()
                        }, text = stringResource(id = R.string.btn_accept)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ModesDialogPreview() {
    val allModes = listOf(
        "AFSK", "AFSK S-Net", "AFSK SALSAT", "AHRPT", "AM", "APT", "BPSK", "BPSK PMT-A3",
        "CERTO", "CW", "DQPSK", "DSTAR", "DUV", "FFSK", "FM", "FMN", "FSK", "FSK AX.100 Mode 5",
        "FSK AX.100 Mode 6", "FSK AX.25 G3RUH", "GFSK", "GFSK Rktr", "GMSK", "HRPT", "LoRa",
        "LRPT", "LSB", "MFSK", "MSK", "MSK AX.100 Mode 5", "MSK AX.100 Mode 6", "OFDM", "OQPSK",
        "PSK", "PSK31", "PSK63", "QPSK", "QPSK31", "QPSK63", "SSTV", "USB", "WSJT"
    )
    MainTheme { ModesDialog(allModes, listOf("AFSK", "AFSK S-Net"), {}) {} }
}

@Composable
fun ModesDialog(items: List<String>, selected: List<String>, dismiss: () -> Unit, select: (String) -> Unit) {
    Dialog(onDismissRequest = { dismiss() }) {
        ElevatedCard(modifier = Modifier.fillMaxHeight(0.75f)) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                modifier = Modifier.background(MaterialTheme.colorScheme.background),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                itemsIndexed(items) { index, item ->
                    Surface {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { select(item) }) {
                            Text(
                                text = "$index).",
                                modifier = Modifier.padding(start = 12.dp, end = 6.dp),
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = item,
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Checkbox(checked = selected.contains(item), onCheckedChange = { select(item) })
                        }
                    }
                }
            }
        }
    }
}
