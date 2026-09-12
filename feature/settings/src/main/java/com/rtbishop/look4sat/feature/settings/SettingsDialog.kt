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

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rtbishop.look4sat.core.domain.model.RCSettings
import com.rtbishop.look4sat.core.domain.model.RadioControlSettings
import com.rtbishop.look4sat.core.domain.model.Constants
import com.rtbishop.look4sat.core.domain.source.NetworkResult
import com.rtbishop.look4sat.core.domain.source.Sources
import com.rtbishop.look4sat.core.presentation.CardButton
import com.rtbishop.look4sat.core.presentation.DragReorderState
import com.rtbishop.look4sat.core.presentation.IconCard
import com.rtbishop.look4sat.core.presentation.LocalSpacing
import com.rtbishop.look4sat.core.presentation.MainTheme
import com.rtbishop.look4sat.core.presentation.R
import com.rtbishop.look4sat.core.presentation.ConfirmDialog
import com.rtbishop.look4sat.core.presentation.dragHandle
import com.rtbishop.look4sat.core.presentation.dragLift
import com.rtbishop.look4sat.core.presentation.rememberDragReorderState
import com.rtbishop.look4sat.core.presentation.rememberDragRowState

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
    ConfirmDialog(title = titleText, onCancel = dismiss, onAccept = onAccept) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LocalSpacing.current.large)
        ) {
            OutlinedTextField(
                value = latValue.value,
                onValueChange = { latValue.value = it },
                label = { Text(text = stringResource(id = R.string.prefs_station_lat_text)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = lonValue.value,
                onValueChange = { lonValue.value = it },
                label = { Text(text = stringResource(id = R.string.prefs_station_lon_text)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(0.dp))
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
    ConfirmDialog(
        title = stringResource(R.string.prefs_locator_title),
        onCancel = dismiss,
        onAccept = onAccept
    ) {
        OutlinedTextField(
            value = locator.value,
            onValueChange = { locator.value = it },
            label = { Text(text = stringResource(id = R.string.prefs_locator_text)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LocalSpacing.current.large),
        )
        Spacer(modifier = Modifier.height(0.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun TransceiversDialogPreview() {
    MainTheme {
        DataSourcesDialog(
            satelliteUrls = listOf(
                "celestrak.org/NORAD/elements/gp.php?GROUP=active&FORMAT=csv",
                "amsat.org/tle/current/nasabare.txt"
            ),
            transceiversUrls = listOf(
                "db.satnogs.org/api/transmitters/?format=json&status=active"
            ),
            satelliteEnabled = listOf(true, false),
            transceiversEnabled = listOf(true),
            statusCodes = mapOf(
                "celestrak.org/NORAD/elements/gp.php?GROUP=active&FORMAT=csv" to 200,
                "amsat.org/tle/current/nasabare.txt" to 404
            ),
            onImportTle = {},
            onImportTransceivers = {},
            onDismiss = {},
            onSave = { _, _, _, _ -> }
        )
    }
}

/**
 * Single source of truth for one data-source row: url, enabled flag and stable id all live
 * together so a reorder, a toggle or an edit is a single list mutation instead of several parallel
 * lists/maps having to stay in sync (which used to fan out into extra recompositions).
 */
private data class SourceEntry(val id: Long, val url: String, val enabled: Boolean = true)

@Composable
fun DataSourcesDialog(
    satelliteUrls: List<String>,
    transceiversUrls: List<String>,
    satelliteEnabled: List<Boolean>,
    transceiversEnabled: List<Boolean>,
    statusCodes: Map<String, Int>,
    onImportTle: () -> Unit,
    onImportTransceivers: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (List<String>, List<String>, List<Boolean>, List<Boolean>) -> Unit
) {
    val padding = LocalSpacing.current.large
    // Use stable Long IDs to avoid key collisions (e.g. multiple empty "" entries).
    val nextId = remember { mutableLongStateOf((satelliteUrls.size + transceiversUrls.size).toLong()) }
    val satEntries = remember {
        satelliteUrls.mapIndexed { i, url ->
            SourceEntry(i.toLong(), url, satelliteEnabled.getOrElse(i) { true })
        }.toMutableStateList()
    }
    val txEntries = remember {
        transceiversUrls.mapIndexed { i, url ->
            SourceEntry((satelliteUrls.size + i).toLong(), url, transceiversEnabled.getOrElse(i) { true })
        }.toMutableStateList()
    }
    val listState = rememberLazyListState()
    val dragState = rememberDragReorderState(listState)
    val onRestoreSatDefaults = {
        satEntries.clear()
        satEntries.addAll(Sources.satelliteDataUrls.map { url -> SourceEntry(nextId.longValue++, url) })
        Unit
    }
    val onRestoreTxDefaults = {
        txEntries.clear()
        txEntries.addAll(Sources.transceiversDataUrls.map { url -> SourceEntry(nextId.longValue++, url) })
        Unit
    }
    val onAccept = {
        val satFiltered = satEntries.filter { it.url.isNotBlank() }
        val txFiltered = txEntries.filter { it.url.isNotBlank() }
        try {
            onSave(
                satFiltered.map { it.url },
                txFiltered.map { it.url },
                satFiltered.map { it.enabled },
                txFiltered.map { it.enabled }
            )
        } finally {
            onDismiss()
        }
    }
    val satTitle = stringResource(R.string.prefs_data_sources_sat_title)
    val satHint = stringResource(R.string.prefs_data_sources_sat_hint)
    val transceiversTitle = stringResource(R.string.prefs_data_sources_transceivers_title)
    val transceiversHint = stringResource(R.string.prefs_data_sources_transceivers_hint)
    ConfirmDialog(
        title = stringResource(id = R.string.prefs_data_sources_title),
        onCancel = onDismiss,
        onAccept = onAccept,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxHeight(0.84f)
                .padding(horizontal = padding),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(vertical = 0.dp)
        ) {
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CardButton(
                        onClick = { onImportTle(); onDismiss() },
                        text = "TLE/3LE (.txt)\nOMM (.csv)",
                        modifier = Modifier.weight(1f)
                    )
                    CardButton(
                        onClick = { onImportTransceivers(); onDismiss() },
                        text = "Transceivers\nSatNOGS (.json)",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            sourceSection(
                sectionKey = "sat",
                label = satTitle,
                hint = satHint,
                entries = satEntries,
                dragState = dragState,
                statusCodes = statusCodes,
                onAdd = { satEntries.add(SourceEntry(nextId.longValue++, "")) },
                onRestore = onRestoreSatDefaults,
                onMove = { from, to -> satEntries.add(to, satEntries.removeAt(from)) },
                onRemove = { i -> satEntries.removeAt(i) },
                onToggle = { i -> satEntries[i] = satEntries[i].copy(enabled = !satEntries[i].enabled) },
                onUrlChange = { i, v -> satEntries[i] = satEntries[i].copy(url = v) }
            )
            sourceSection(
                sectionKey = "tx",
                label = transceiversTitle,
                hint = transceiversHint,
                entries = txEntries,
                dragState = dragState,
                statusCodes = statusCodes,
                onAdd = { txEntries.add(SourceEntry(nextId.longValue++, "")) },
                onRestore = onRestoreTxDefaults,
                onMove = { from, to -> txEntries.add(to, txEntries.removeAt(from)) },
                onRemove = { i -> txEntries.removeAt(i) },
                onToggle = { i -> txEntries[i] = txEntries[i].copy(enabled = !txEntries[i].enabled) },
                onUrlChange = { i, v -> txEntries[i] = txEntries[i].copy(url = v) }
            )
        }
    }
}

private fun LazyListScope.sourceSection(
    sectionKey: String,
    label: String,
    hint: String,
    entries: SnapshotStateList<SourceEntry>,
    dragState: DragReorderState,
    statusCodes: Map<String, Int>,
    onAdd: () -> Unit,
    onRestore: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
    onToggle: (Int) -> Unit,
    onUrlChange: (Int, String) -> Unit
) {
    item {
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            IconCard(
                action = onRestore,
                resId = R.drawable.ic_restore,
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            IconCard(
                action = onAdd,
                resId = R.drawable.ic_add,
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
        Text(
            text = hint,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 8.dp)
        )
    }
    itemsIndexed(entries, key = { _, entry -> "$sectionKey-${entry.id}" }) { index, entry ->
        val enabledTint = MaterialTheme.colorScheme.onSurfaceVariant
        val rowState = rememberDragRowState(
            dragState = dragState,
            items = entries,
            item = entry,
            key = { "$sectionKey-${it.id}" },
            onMove = onMove
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .dragLift(isLifted = rowState.isLifted, translationY = rowState.translationY)
                .animateItem(
                    fadeInSpec = tween(durationMillis = 200),
                    // The dragged row repositions instantly, while its neighbors smoothly
                    // slide out of the way (the "squeeze" effect).
                    placementSpec = if (rowState.isDragging) {
                        tween(durationMillis = 0)
                    } else {
                        tween(durationMillis = 250, easing = FastOutSlowInEasing)
                    },
                    fadeOutSpec = tween(durationMillis = 200)
                )
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_drag_handle),
                contentDescription = null,
                tint = enabledTint,
                modifier = Modifier
                    .padding(top = textFieldLabelOffset)
                    .dragHandle(rowState)
            )
            Spacer(modifier = Modifier.width(12.dp))
            OutlinedTextField(
                value = entry.url,
                onValueChange = { onUrlChange(index, it) },
                label = {
                    Row {
                        Text("Source URL")
                        statusCodes[entry.url]?.let { code ->
                            Text(" - ${statusLabel(code)}", color = statusColor(code), fontSize = 12.sp)
                        }
                    }
                },
                trailingIcon = {
                    IconButton(onClick = { onRemove(index) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = null
                        )
                    }
                },
                singleLine = true,
                enabled = entry.enabled,
                modifier = Modifier.weight(1f)
            )
            Checkbox(
                checked = entry.enabled,
                onCheckedChange = { onToggle(index) },
                modifier = Modifier.padding(top = textFieldLabelOffset)
            )
        }
    }
}

/**
 * Compensates for the visual weight of the [OutlinedTextField]'s floating label: the label
 * pushes the perceived center of the field's content down a bit, so the drag handle and
 * checkbox are nudged down by the same amount to stay visually centered on the input line.
 */
private val textFieldLabelOffset = 8.dp

private fun statusLabel(code: Int): String = if (code == NetworkResult.CONNECTION_ERROR) "ERR" else code.toString()

@Composable
private fun statusColor(code: Int): Color = when (code) {
    NetworkResult.CONNECTION_ERROR -> MaterialTheme.colorScheme.error
    in 200..299 -> Color(0xFF66BB6A)
    else -> MaterialTheme.colorScheme.error
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
                frequencyOffsetHz = 0L,
                bluetoothRotatorState = false,
                bluetoothRotatorFormat = $$"P $AZ $EL",
                bluetoothRotatorName = "Default",
                bluetoothRotatorAddress = "00:0C:BF:13:80:5D",
                bluetoothFrequencyState = false,
                bluetoothFrequencyAddress = "00:0C:BF:13:80:5D",
                bluetoothFrequencyFormat = $$"F $FREQ"
            ),
            onDismiss = {},
            onSave = { _, _, _, _, _, _, _, _, _ -> }
        )
    }
}

@Composable
fun NetworkOutputDialog(
    initialSettings: RCSettings,
    onDismiss: () -> Unit,
    onSave: (
        Boolean, String, String, String,
        Boolean, String, String, String, Long
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
    val frequencyOffsetHz = rememberSaveable { mutableStateOf(initialSettings.frequencyOffsetHz.toString()) }
    val onAccept = {
        val (rotIp, rotPort) = splitAddress(rotatorAddress.value)
        val (freqIp, freqPort) = splitAddress(frequencyAddress.value)
        val offsetHz = (frequencyOffsetHz.value.trim().toLongOrNull() ?: 0L)
            .coerceIn(Constants.FREQ_OFFSET_MIN_HZ, Constants.FREQ_OFFSET_MAX_HZ)
        onSave(
            rotatorState.value, rotIp, rotPort, rotatorFormat.value,
            frequencyState.value, freqIp, freqPort, frequencyFormat.value, offsetHz
        )
        onDismiss()
    }
    ConfirmDialog(
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
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = frequencyOffsetHz.value,
                onValueChange = { frequencyOffsetHz.value = it },
                singleLine = true,
                label = { Text(stringResource(R.string.prefs_net_frequency_offset_hint)) },
                supportingText = { Text(stringResource(R.string.prefs_net_frequency_offset_help)) },
                trailingIcon = {
                    IconButton(
                        onClick = { frequencyOffsetHz.value = "0" },
                        enabled = frequencyState.value && frequencyOffsetHz.value != "0"
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = null
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = frequencyState.value
            )
            Spacer(modifier = Modifier.height(12.dp))
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
                frequencyOffsetHz = 0L,
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
    val rotatorAddress =
        rememberSaveable { mutableStateOf(initialSettings.bluetoothRotatorAddress) }
    val rotatorFormat = rememberSaveable { mutableStateOf(initialSettings.bluetoothRotatorFormat) }
    val frequencyState =
        rememberSaveable { mutableStateOf(initialSettings.bluetoothFrequencyState) }
    val frequencyAddress =
        rememberSaveable { mutableStateOf(initialSettings.bluetoothFrequencyAddress) }
    val frequencyFormat =
        rememberSaveable { mutableStateOf(initialSettings.bluetoothFrequencyFormat) }
    val onAccept = {
        onSave(
            rotatorState.value, rotatorAddress.value, rotatorFormat.value,
            frequencyState.value, frequencyAddress.value, frequencyFormat.value
        )
        onDismiss()
    }
    ConfirmDialog(
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
            Spacer(modifier = Modifier.height(12.dp))
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
    pairedBluetoothDevices: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onSave: (RadioControlSettings) -> Unit
) {
    val padding = LocalSpacing.current.large
    val enabled = rememberSaveable { mutableStateOf(initialSettings.enabled) }
    val radioModel = rememberSaveable { mutableStateOf(initialSettings.radioModel) }
    val splitMode = rememberSaveable { mutableStateOf(initialSettings.splitMode) }
    val txAddress = rememberSaveable { mutableStateOf(initialSettings.txRadioAddress) }
    val rxAddress = rememberSaveable { mutableStateOf(initialSettings.rxRadioAddress) }
    val txName = rememberSaveable { mutableStateOf(initialSettings.txRadioName) }
    val rxName = rememberSaveable { mutableStateOf(initialSettings.rxRadioName) }
    val baudRate = rememberSaveable { mutableIntStateOf(initialSettings.baudRate) }
    val selectingFor = rememberSaveable { mutableStateOf("") } // "tx", "rx", or ""

    val isIcom = radioModel.value == RadioControlSettings.MODEL_ICOM_IC705
    val isSingleRadio = isIcom && splitMode.value

    // Reset split mode when switching away from IC-705
    if (!isIcom && splitMode.value) splitMode.value = false

    val baudRates = if (isIcom) RadioControlSettings.BAUD_RATES_ICOM
    else RadioControlSettings.BAUD_RATES_YAESU

    // If current baud rate is not in the new list, default to the first available
    if (baudRate.intValue !in baudRates) baudRate.intValue = baudRates.first()

    val onAccept = {
        onSave(
            RadioControlSettings(
                enabled = enabled.value,
                radioModel = radioModel.value,
                txRadioAddress = txAddress.value,
                rxRadioAddress = if (isSingleRadio) "" else rxAddress.value,
                txRadioName = txName.value,
                rxRadioName = if (isSingleRadio) "" else rxName.value,
                baudRate = baudRate.intValue,
                splitMode = splitMode.value
            )
        )
        onDismiss()
    }

    ConfirmDialog(
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

            Text(
                text = stringResource(R.string.rc_radio_model),
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                RadioControlSettings.SUPPORTED_RADIOS.forEach { model ->
                    FilterChip(
                        selected = radioModel.value == model,
                        onClick = { radioModel.value = model },
                        label = {
                            Text(
                                text = compactRadioModelLabel(model),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        enabled = enabled.value,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            val isSplitModeAvailable = enabled.value && isIcom
            val splitModeLabelColor = if (isSplitModeAvailable) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f)
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.settings_split_mode),
                    fontWeight = FontWeight.Medium,
                    color = splitModeLabelColor
                )
                Switch(
                    checked = splitMode.value,
                    onCheckedChange = { splitMode.value = it },
                    enabled = isSplitModeAvailable
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            Text("Radio devices", fontWeight = FontWeight.Medium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                CardButton(
                    onClick = { selectingFor.value = "tx" },
                    text = if (isSingleRadio) "Select Radio" else "Select TX",
                    modifier = Modifier.weight(1f)
                )
                CardButton(
                    onClick = { if (!isSingleRadio) selectingFor.value = "rx" },
                    text = if (isSingleRadio) "RX = TX" else "Select RX",
                    modifier = Modifier.weight(1f)
                )
            }
            if (txAddress.value.isNotBlank()) {
                Text("TX: ${txName.value} — ${txAddress.value}", fontSize = 13.sp)
            }
            if (isSingleRadio && txAddress.value.isNotBlank()) {
                Text("RX: same as TX", fontSize = 13.sp)
            } else if (rxAddress.value.isNotBlank()) {
                Text("RX: ${rxName.value} — ${rxAddress.value}", fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))

            if (selectingFor.value.isNotBlank()) {
                Text(
                    text = stringResource(R.string.settings_paired_devices),
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (pairedBluetoothDevices.isEmpty()) {
                    Text(
                        "No paired devices found. Pair your BT adapter in Android Bluetooth settings first.",
                        fontSize = 13.sp
                    )
                } else {
                    pairedBluetoothDevices.forEach { (name, address) ->
                        androidx.compose.material3.Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (selectingFor.value == "tx") {
                                        txAddress.value = address
                                        txName.value = name
                                        if (isSingleRadio) {
                                            rxAddress.value = address
                                            rxName.value = name
                                        }
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
                Spacer(modifier = Modifier.height(6.dp))
            }

            Text("Baud Rate:", fontWeight = FontWeight.Medium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                maxItemsInEachRow = 6,
                modifier = Modifier.fillMaxWidth()
            ) {
                baudRates.forEach { rate ->
                    FilterChip(
                        selected = rate == baudRate.intValue,
                        onClick = { baudRate.intValue = rate },
                        label = {
                            Text(
                                text = baudLabel(rate),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        enabled = enabled.value,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

private fun compactRadioModelLabel(model: String): String = when (model) {
    RadioControlSettings.MODEL_YAESU_FT817 -> "FT-817/818"
    RadioControlSettings.MODEL_YAESU_FT857 -> "FT-857/897"
    RadioControlSettings.MODEL_ICOM_IC705 -> "IC-705"
    else -> model
}

private fun baudLabel(rate: Int): String = when (rate) {
    4800 -> "4k"
    9600 -> "9k"
    19200 -> "19k"
    38400 -> "38k"
    57600 -> "57k"
    115200 -> "115k"
    else -> rate.toString()
}
