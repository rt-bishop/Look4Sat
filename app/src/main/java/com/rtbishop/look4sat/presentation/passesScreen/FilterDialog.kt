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
package com.rtbishop.look4sat.presentation.passesScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import com.rtbishop.look4sat.presentation.CardButton
import com.rtbishop.look4sat.presentation.MainTheme

@Preview(showBackground = true)
@Composable
private fun FilterDialogPreview() {
    MainTheme { FilterDialog(8, 16.0, {}) { _, _ -> } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDialog(hours: Int, elevation: Double, toggle: () -> Unit, save: (Int, Double) -> Unit) {
    val hoursValue = rememberSaveable { mutableStateOf(hours) }
    val elevValue = rememberSaveable { mutableStateOf(elevation) }
    Dialog(onDismissRequest = { toggle() }) {
        ElevatedCard {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(1f)
            ) {
                Text(text = "Filter passes", color = MaterialTheme.colorScheme.primary)
                Text(text = "Show passes that occur within X hours")
                OutlinedTextField(value = hoursValue.value.toString(), onValueChange = { newValue ->
                    val hoursAhead = try {
                        newValue.toInt()
                    } catch (exception: Exception) {
                        12
                    }
                    hoursValue.value = hoursAhead
                })
                Text(text = "Show passes with max elevation above")
                OutlinedTextField(value = elevValue.value.toString(), onValueChange = { newValue ->
                    val maxElevation = try {
                        newValue.toDouble()
                    } catch (exception: Exception) {
                        16.0
                    }
                    elevValue.value = maxElevation
                })
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CardButton(onClick = { toggle() }, text = "Cancel")
                    CardButton(
                        onClick = {
                            save(hoursValue.value, elevValue.value)
                            toggle()
                        }, text = "Accept"
                    )
                }
            }
        }
    }
}
