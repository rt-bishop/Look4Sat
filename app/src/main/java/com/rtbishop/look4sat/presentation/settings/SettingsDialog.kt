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
package com.rtbishop.look4sat.presentation.settings

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.presentation.MainTheme
import com.rtbishop.look4sat.presentation.common.SharedDialog
import com.rtbishop.look4sat.presentation.common.CardButton
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.rtbishop.look4sat.presentation.LocalSpacing

@Preview(showBackground = true)
@Composable
private fun PositionDialogPreview() {
    MainTheme { PositionDialog(0.0, 0.0, {}) { _, _ -> } }
}

@Composable
fun PositionDialog(lat: Double, lon: Double, dismiss: () -> Unit, save: (Double, Double) -> Unit) {
    val latValue = rememberSaveable { mutableStateOf(lat.toString()) }
    val lonValue = rememberSaveable { mutableStateOf(lon.toString()) }
    val titleText = stringResource(id = R.string.prefs_station_title)
    val onAccept = { saveValues(latValue.value, lonValue.value, save).also { dismiss() } }
    SharedDialog(title = titleText, onCancel = dismiss, onAccept = onAccept) {
        OutlinedTextField(
            value = latValue.value,
            onValueChange = { latValue.value = it },
            label = { Text(text = stringResource(id = R.string.prefs_station_lat_text)) }
        )
        OutlinedTextField(
            value = lonValue.value,
            onValueChange = { lonValue.value = it },
            label = { Text(text = stringResource(id = R.string.prefs_station_lon_text)) }
        )
    }
}

private fun saveValues(latValue: String, lonValue: String, save: (Double, Double) -> Unit) {
    val latitude = latValue.toDoubleOrNull() ?: 0.0
    val longitude = lonValue.toDoubleOrNull() ?: 0.0
    val newLatitude = if (latitude > 90) 90.0 else if (latitude < -90) -90.0 else latitude
    val newLongitude = if (longitude > 180) 180.0 else if (longitude < -180) -180.0 else longitude
    save(newLatitude, newLongitude)
}

@Preview(showBackground = true)
@Composable
private fun LocatorDialogPreview() {
    MainTheme { LocatorDialog("IO91vl", {}) { } }
}

@Composable
fun LocatorDialog(qthLocator: String, dismiss: () -> Unit, save: (String) -> Unit) {
    val locator = rememberSaveable { mutableStateOf(qthLocator) }
    val onAccept = { save(locator.value).also { dismiss() } }
    SharedDialog(title = stringResource(R.string.prefs_locator_title), onCancel = dismiss, onAccept = onAccept) {
        OutlinedTextField(
            value = locator.value,
            onValueChange = { locator.value = it },
            label = { Text(text = stringResource(id = R.string.prefs_locator_text)) }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TransceiversDialogPreview() {
    MainTheme {
        DataSourcesDialog(
            useCustomTle = true,
            useCustomTransceivers = true,
            tleUrl = "https://example.com/tle.txt",
            transceiversUrl = "https://example.com/tx.json",
            onImportTle = {},
            onImportTransceivers = {},
            onDismiss = {},
            onSave = { _, _, _, _ -> }
        )
    }
}

@Composable
fun DataSourcesDialog(
    useCustomTle: Boolean,
    useCustomTransceivers: Boolean,
    tleUrl: String,
    transceiversUrl: String,
    onImportTle: () -> Unit,
    onImportTransceivers: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (Boolean, Boolean, String, String) -> Unit
) {
    val padding = LocalSpacing.current.large
    val isEnabledCustomTle = rememberSaveable { mutableStateOf(useCustomTle) }
    val isEnabledCustomTransceivers = rememberSaveable { mutableStateOf(useCustomTransceivers) }
    val urlTle = rememberSaveable { mutableStateOf(tleUrl) }
    val urlTransceivers = rememberSaveable { mutableStateOf(transceiversUrl) }
    val onAccept = {
        onSave(isEnabledCustomTle.value, isEnabledCustomTransceivers.value, urlTle.value, urlTransceivers.value)
        onDismiss()
    }
    val onCancel = { onDismiss() }
    SharedDialog(
        title = stringResource(id = R.string.prefs_data_sources_title),
        onCancel = onCancel,
        onAccept = onAccept,
    ) {
        Column(modifier = Modifier.padding(horizontal = padding)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CardButton(
                    onClick = {
                        onImportTle()
                        onDismiss()
                    },
                    text = "TLE (3LE)\nR4UAB (.txt)",
                    modifier = Modifier.weight(1f)
                )
                CardButton(
                    onClick = {
                        onImportTransceivers()
                        onDismiss()
                    },
                    text = "Transceivers\nSatNOGS (.json)",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.prefs_data_sources_tle_switch))
                Switch(
                    checked = isEnabledCustomTle.value,
                    onCheckedChange = { isEnabledCustomTle.value = it }
                )
            }
            OutlinedTextField(
                value = urlTle.value,
                onValueChange = { urlTle.value = it },
                label = { Text(text = stringResource(id = R.string.prefs_data_sources_url_title)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = isEnabledCustomTle.value,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.prefs_data_sources_transceivers_switch))
                Switch(
                    checked = isEnabledCustomTransceivers.value,
                    onCheckedChange = { isEnabledCustomTransceivers.value = it }
                )
            }
            OutlinedTextField(
                value = urlTransceivers.value,
                onValueChange = { urlTransceivers.value = it },
                label = { Text(text = stringResource(id = R.string.prefs_data_sources_url_title)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = isEnabledCustomTransceivers.value,
            )
        }
    }
}
