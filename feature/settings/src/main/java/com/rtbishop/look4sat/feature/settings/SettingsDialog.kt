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
package com.rtbishop.look4sat.feature.settings

import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rtbishop.look4sat.core.domain.model.RCSettings
import com.rtbishop.look4sat.core.domain.model.RadioControlSettings
import com.rtbishop.look4sat.core.presentation.CardButton
import com.rtbishop.look4sat.core.presentation.LocalSpacing
import com.rtbishop.look4sat.core.presentation.MainTheme
import com.rtbishop.look4sat.core.presentation.R
import com.rtbishop.look4sat.core.presentation.SharedDialog

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

@Preview(showBackground = true)
@Composable
fun PreviewNetworkOutputDialog() {
    MainTheme {
        NetworkOutputDialog(
            initialSettings = RCSettings(
                rotatorState = false,
                rotatorAddress = "127.0.0.1",
                rotatorPort = "4533",
                rotatorFormat = $$"P $AZ $EL",
                frequencyState = false,
                frequencyAddress = "127.0.0.1",
                frequencyPort = "4532",
                frequencyFormat = $$"F $FREQ",
                bluetoothRotatorState = false,
                bluetoothRotatorFormat = $$"P $AZ $EL",
                bluetoothRotatorName = "Default",
                bluetoothRotatorAddress = "00:0C:BF:13:80:5D",
                bluetoothFrequencyState = false,
                bluetoothFrequencyAddress = "00:0C:BF:13:80:5D",
                bluetoothFrequencyFormat = $$"F $FREQ"
            ),
            onDismiss = {},
            onSave = { _, _, _, _, _, _, _, _ -> }
        )
    }
}

@Composable
fun NetworkOutputDialog(
    initialSettings: RCSettings,
    onDismiss: () -> Unit,
    onSave: (
        Boolean, String, String, String,
        Boolean, String, String, String
    ) -> Unit
) {
    val padding = LocalSpacing.current.large
    val rotatorState = rememberSaveable { mutableStateOf(initialSettings.rotatorState) }
    val rotatorAddress = rememberSaveable {
        mutableStateOf("${initialSettings.rotatorAddress}:${initialSettings.rotatorPort}")
    }
    val rotatorFormat = rememberSaveable { mutableStateOf(initialSettings.rotatorFormat) }
    val frequencyState = rememberSaveable { mutableStateOf(initialSettings.frequencyState) }
    val frequencyAddress = rememberSaveable {
        mutableStateOf("${initialSettings.frequencyAddress}:${initialSettings.frequencyPort}")
    }
    val frequencyFormat = rememberSaveable { mutableStateOf(initialSettings.frequencyFormat) }
    val onAccept = {
        val (rotIp, rotPort) = splitAddress(rotatorAddress.value)
        val (freqIp, freqPort) = splitAddress(frequencyAddress.value)
        onSave(
            rotatorState.value, rotIp, rotPort, rotatorFormat.value,
            frequencyState.value, freqIp, freqPort, frequencyFormat.value
        )
        onDismiss()
    }
    SharedDialog(
        title = stringResource(R.string.prefs_net_title),
        onCancel = onDismiss,
        onAccept = onAccept
    ) {
        Column(modifier = Modifier.padding(horizontal = padding)) {
            OutputChannelSection(
                switchLabel = stringResource(R.string.prefs_net_rotator_switch),
                enabled = rotatorState.value,
                onEnabledChange = { rotatorState.value = it },
                address = rotatorAddress.value,
                onAddressChange = { rotatorAddress.value = it },
                addressLabel = stringResource(R.string.prefs_net_rotator_address_hint),
                format = rotatorFormat.value,
                onFormatChange = { rotatorFormat.value = it },
                formatLabel = stringResource(R.string.prefs_net_rotator_format_hint)
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutputChannelSection(
                switchLabel = stringResource(R.string.prefs_net_frequency_switch),
                enabled = frequencyState.value,
                onEnabledChange = { frequencyState.value = it },
                address = frequencyAddress.value,
                onAddressChange = { frequencyAddress.value = it },
                addressLabel = stringResource(R.string.prefs_net_frequency_address_hint),
                format = frequencyFormat.value,
                onFormatChange = { frequencyFormat.value = it },
                formatLabel = stringResource(R.string.prefs_net_frequency_format_hint)
            )
        }
    }
}

private fun splitAddress(address: String): Pair<String, String> {
    val lastColon = address.lastIndexOf(':')
    return if (lastColon >= 0) {
        address.substring(0, lastColon) to address.substring(lastColon + 1)
    } else {
        address to ""
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewBluetoothOutputDialog() {
    MainTheme {
        BluetoothOutputDialog(
            initialSettings = RCSettings(
                rotatorState = false,
                rotatorAddress = "127.0.0.1",
                rotatorPort = "4533",
                rotatorFormat = $$"P $AZ $EL",
                frequencyState = false,
                frequencyAddress = "127.0.0.1",
                frequencyPort = "4532",
                frequencyFormat = $$"F $FREQ",
                bluetoothRotatorState = false,
                bluetoothRotatorFormat = $$"P $AZ $EL",
                bluetoothRotatorName = "Default",
                bluetoothRotatorAddress = "00:0C:BF:13:80:5D",
                bluetoothFrequencyState = false,
                bluetoothFrequencyAddress = "00:0C:BF:13:80:5D",
                bluetoothFrequencyFormat = $$"F $FREQ"
            ),
            onDismiss = {},
            onSave = { _, _, _, _, _, _ -> }
        )
    }
}

@Composable
fun BluetoothOutputDialog(
    initialSettings: RCSettings,
    onDismiss: () -> Unit,
    onSave: (
        Boolean, String, String,
        Boolean, String, String
    ) -> Unit
) {
    val padding = LocalSpacing.current.large
    val rotatorState = rememberSaveable { mutableStateOf(initialSettings.bluetoothRotatorState) }
    val rotatorAddress = rememberSaveable { mutableStateOf(initialSettings.bluetoothRotatorAddress) }
    val rotatorFormat = rememberSaveable { mutableStateOf(initialSettings.bluetoothRotatorFormat) }
    val frequencyState = rememberSaveable { mutableStateOf(initialSettings.bluetoothFrequencyState) }
    val frequencyAddress = rememberSaveable { mutableStateOf(initialSettings.bluetoothFrequencyAddress) }
    val frequencyFormat = rememberSaveable { mutableStateOf(initialSettings.bluetoothFrequencyFormat) }
    val onAccept = {
        onSave(
            rotatorState.value, rotatorAddress.value, rotatorFormat.value,
            frequencyState.value, frequencyAddress.value, frequencyFormat.value
        )
        onDismiss()
    }
    SharedDialog(
        title = stringResource(R.string.prefs_bt_title),
        onCancel = onDismiss,
        onAccept = onAccept
    ) {
        Column(modifier = Modifier.padding(horizontal = padding)) {
            OutputChannelSection(
                switchLabel = stringResource(R.string.prefs_bt_rotator_switch),
                enabled = rotatorState.value,
                onEnabledChange = { rotatorState.value = it },
                address = rotatorAddress.value,
                onAddressChange = { rotatorAddress.value = it },
                addressLabel = stringResource(R.string.prefs_bt_rotator_device_hint),
                format = rotatorFormat.value,
                onFormatChange = { rotatorFormat.value = it },
                formatLabel = stringResource(R.string.prefs_bt_rotator_output_hint)
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutputChannelSection(
                switchLabel = stringResource(R.string.prefs_bt_frequency_switch),
                enabled = frequencyState.value,
                onEnabledChange = { frequencyState.value = it },
                address = frequencyAddress.value,
                onAddressChange = { frequencyAddress.value = it },
                addressLabel = stringResource(R.string.prefs_bt_frequency_device_hint),
                format = frequencyFormat.value,
                onFormatChange = { frequencyFormat.value = it },
                formatLabel = stringResource(R.string.prefs_bt_frequency_output_hint)
            )
        }
    }
}

/**
 * Reusable section for a switch-toggled output channel with address and format fields.
 * Used by both Network and Bluetooth output dialogs.
 */
@Composable
private fun OutputChannelSection(
    switchLabel: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    address: String,
    onAddressChange: (String) -> Unit,
    addressLabel: String,
    format: String,
    onFormatChange: (String) -> Unit,
    formatLabel: String
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(switchLabel)
        Switch(checked = enabled, onCheckedChange = onEnabledChange)
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = address,
            onValueChange = onAddressChange,
            singleLine = true,
            label = { Text(addressLabel) },
            modifier = Modifier.weight(0.6f),
            enabled = enabled
        )
        OutlinedTextField(
            value = format,
            onValueChange = onFormatChange,
            singleLine = true,
            label = { Text(formatLabel) },
            modifier = Modifier.weight(0.4f),
            enabled = enabled
        )
    }
}

@Composable
fun RadioControlDialog(
    initialSettings: RadioControlSettings,
    onDismiss: () -> Unit,
    onSave: (RadioControlSettings) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val padding = LocalSpacing.current.large
    val baudRates = listOf(4800, 9600, 38400)
    val enabled = rememberSaveable { mutableStateOf(initialSettings.enabled) }
    val radioModel = rememberSaveable { mutableStateOf(initialSettings.radioModel) }
    val txAddress = rememberSaveable { mutableStateOf(initialSettings.txRadioAddress) }
    val rxAddress = rememberSaveable { mutableStateOf(initialSettings.rxRadioAddress) }
    val txName = rememberSaveable { mutableStateOf(initialSettings.txRadioName) }
    val rxName = rememberSaveable { mutableStateOf(initialSettings.rxRadioName) }
    val baudRate = rememberSaveable { mutableIntStateOf(initialSettings.baudRate) }
    val selectingFor = rememberSaveable { mutableStateOf("") } // "tx", "rx", or ""

    val pairedDevices = remember {
        try {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            manager.adapter?.bondedDevices?.map { Pair(it.name ?: "Unknown", it.address) } ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    val onAccept = {
        onSave(
            RadioControlSettings(
                enabled = enabled.value,
                radioModel = radioModel.value,
                txRadioAddress = txAddress.value,
                rxRadioAddress = rxAddress.value,
                txRadioName = txName.value,
                rxRadioName = rxName.value,
                baudRate = baudRate.intValue
            )
        )
        onDismiss()
    }
    SharedDialog(
        title = stringResource(R.string.rc_settings_title),
        onCancel = onDismiss,
        onAccept = onAccept
    ) {
        Column(modifier = Modifier.padding(horizontal = padding)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.rc_enable_switch))
                Switch(checked = enabled.value, onCheckedChange = { enabled.value = it })
            }
            Spacer(modifier = Modifier.height(6.dp))

            // Radio model selection
            Text(
                stringResource(R.string.rc_radio_model),
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.primary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                RadioControlSettings.SUPPORTED_RADIOS.forEach { model ->
                    androidx.compose.material3.FilterChip(
                        selected = radioModel.value == model,
                        onClick = { radioModel.value = model },
                        label = { Text(model, fontSize = 12.sp) },
                        enabled = enabled.value
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            // TX Radio selection
            Text("TX Radio (Uplink)", fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
            if (txAddress.value.isNotBlank()) {
                Text("${txName.value} - ${txAddress.value}", fontSize = 13.sp)
            }
            CardButton(
                onClick = { selectingFor.value = "tx" },
                text = "Select TX Device",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))

            // RX Radio selection
            Text("RX Radio (Downlink)", fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
            if (rxAddress.value.isNotBlank()) {
                Text("${rxName.value} - ${rxAddress.value}", fontSize = 13.sp)
            }
            CardButton(
                onClick = { selectingFor.value = "rx" },
                text = "Select RX Device",
                modifier = Modifier.fillMaxWidth()
            )

            // Paired devices list (shown when selecting)
            if (selectingFor.value.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Paired Bluetooth Devices:",
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                )
                if (pairedDevices.isEmpty()) {
                    Text("No paired devices found. Pair your BT adapter in Android Bluetooth settings first.")
                } else {
                    pairedDevices.forEach { (name, address) ->
                        androidx.compose.material3.Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (selectingFor.value == "tx") {
                                        txAddress.value = address
                                        txName.value = name
                                    } else {
                                        rxAddress.value = address
                                        rxName.value = name
                                    }
                                    selectingFor.value = ""
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(name, modifier = Modifier.weight(1f))
                                Text(address, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Baud Rate:")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    baudRates.forEach { rate ->
                        CardButton(
                            onClick = { baudRate.intValue = rate },
                            text = if (rate == baudRate.intValue) "[$rate]" else rate.toString(),
                            modifier = Modifier
                        )
                    }
                }
            }
        }
    }
}
