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

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.domain.model.OtherSettings
import com.rtbishop.look4sat.domain.model.RCSettings
import com.rtbishop.look4sat.domain.predict.GeoPos
import com.rtbishop.look4sat.presentation.MainTheme
import com.rtbishop.look4sat.presentation.Screen
import com.rtbishop.look4sat.presentation.common.CardButton
import com.rtbishop.look4sat.presentation.common.IconCard
import com.rtbishop.look4sat.presentation.common.PrimaryIconCard
import com.rtbishop.look4sat.presentation.common.ScreenColumn
import com.rtbishop.look4sat.presentation.common.TopBar
import com.rtbishop.look4sat.presentation.common.infiniteMarquee
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun NavGraphBuilder.settingsDestination() {
    composable(Screen.Settings.route) {
        val viewModel = viewModel(
            modelClass = SettingsViewModel::class.java,
            factory = SettingsViewModel.Factory
        )
        val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
        SettingsScreen(uiState)
    }
}

@Composable
private fun SettingsScreen(uiState: SettingsState) {
    // Permissions setup
    val bluetoothContract = ActivityResultContracts.RequestPermission()
    val bluetoothError = stringResource(R.string.prefs_bt_perm_error)
    val bluetoothPerm = when {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> Manifest.permission.BLUETOOTH
        else -> Manifest.permission.BLUETOOTH_CONNECT
    }
    val bluetoothRequest = rememberLauncherForActivityResult(bluetoothContract) { isGranted ->
        if (!isGranted) {
            uiState.sendRCAction(RCAction.SetBluetoothState(false))
            uiState.sendSystemAction(SystemAction.ShowToast(bluetoothError))
        }
    }
    val locationContract = ActivityResultContracts.RequestMultiplePermissions()
    val locationError = stringResource(R.string.prefs_loc_gps_error)
    val locationPermCoarse = Manifest.permission.ACCESS_COARSE_LOCATION
    val locationPermFine = Manifest.permission.ACCESS_FINE_LOCATION
    val locationRequest = rememberLauncherForActivityResult(locationContract) { permissions ->
        when {
            permissions[locationPermFine] == true -> uiState.sendAction(SettingsAction.SetGpsPosition)
            permissions[locationPermCoarse] == true -> uiState.sendAction(SettingsAction.SetGpsPosition)
            else -> uiState.sendSystemAction(SystemAction.ShowToast(locationError))
        }
    }
    val contentContract = ActivityResultContracts.GetContent()
    val contentRequestForTle = rememberLauncherForActivityResult(contentContract) { uri ->
        uri?.let { uiState.sendAction(SettingsAction.UpdateTLEFromFile(uri.toString())) }
    }
    val contentRequestForTransceivers = rememberLauncherForActivityResult(contentContract) { uri ->
        uri?.let { uiState.sendAction(SettingsAction.UpdateTransceiversFromFile(uri.toString())) }
    }

    // Position settings
    val posSettings = uiState.positionSettings
    val setGpsPos = { locationRequest.launch(arrayOf(locationPermCoarse, locationPermFine)) }
    val setGeoPos = { lat: Double, lon: Double ->
        uiState.sendAction(SettingsAction.SetGeoPosition(lat, lon))
    }
    val setQthPos = { locator: String ->
        uiState.sendAction(SettingsAction.SetQthPosition(locator))
    }
    val dismissPos = { uiState.sendAction(SettingsAction.DismissPosMessages) }
    val posDialogState = rememberSaveable { mutableStateOf(false) }
    val showPosDialog = { posDialogState.value = posDialogState.value.not() }
    if (posDialogState.value) {
        PositionDialog(posSettings.stationPos.latitude, posSettings.stationPos.longitude, showPosDialog, setGeoPos)
    }
    val locDialogState = rememberSaveable { mutableStateOf(false) }
    val showLocDialog = { locDialogState.value = locDialogState.value.not() }
    if (locDialogState.value) {
        LocatorDialog(posSettings.stationPos.qthLocator, showLocDialog, setQthPos)
    }

    // Data sources dialog
    val dataSourcesDialogState = rememberSaveable { mutableStateOf(false) }
    val showDataSourcesDialog = { dataSourcesDialogState.value = true }
    val dismissDataSourcesDialog = { dataSourcesDialogState.value = false }
    if (dataSourcesDialogState.value) {
        DataSourcesDialog(
            useCustomTle = uiState.dataSourcesSettings.useCustomTLE,
            useCustomTransceivers = uiState.dataSourcesSettings.useCustomTransceivers,
            tleUrl = uiState.dataSourcesSettings.tleUrl,
            transceiversUrl = uiState.dataSourcesSettings.transceiversUrl,
            onImportTle = { contentRequestForTle.launch("*/*") },
            onImportTransceivers = { contentRequestForTransceivers.launch("*/*") },
            onDismiss = dismissDataSourcesDialog,
            onSave = {
                useCustomTle, useCustomTransceivers, tleUrl, transceiversUrl  ->
                if (!useCustomTle || tleUrl.isNotBlank()) {
                    uiState.sendDataSourcesAction(DataSourcesAction.SetUseCustomTle(useCustomTle))
                    uiState.sendDataSourcesAction(DataSourcesAction.SetTleUrl(tleUrl))
                }
                if (!useCustomTransceivers || transceiversUrl.isNotBlank()) {
                    uiState.sendDataSourcesAction(DataSourcesAction.SetUseCustomTransceivers(useCustomTransceivers))
                    uiState.sendDataSourcesAction(DataSourcesAction.SetTransceiversUrl(transceiversUrl))
                }
                if (useCustomTle || useCustomTransceivers) {
                    uiState.sendAction(SettingsAction.UpdateFromWeb)
                }
            }
        )
    }

    // Data settings
    val dataSettings = uiState.dataSettings
    val updateFromWeb: () -> Unit = { uiState.sendAction(SettingsAction.UpdateFromWeb) }
    val clearAllData: () -> Unit = { uiState.sendAction(SettingsAction.ClearAllData) }

    // RC settings
    val rcSettings = uiState.rcSettings
    val setRotatorState = { value: Boolean -> uiState.sendRCAction(RCAction.SetRotatorState(value)) }
    val setRotatorAddress = { value: String -> uiState.sendRCAction(RCAction.SetRotatorAddress(value)) }
    val setRotatorPort = { value: String -> uiState.sendRCAction(RCAction.SetRotatorPort(value)) }
    val setBluetoothState = { value: Boolean ->
        bluetoothRequest.launch(bluetoothPerm)
        uiState.sendRCAction(RCAction.SetBluetoothState(value))
    }
    val setBluetoothAddress = { value: String -> uiState.sendRCAction(RCAction.SetBluetoothAddress(value)) }
    val setBluetoothFormat = { value: String -> uiState.sendRCAction(RCAction.SetBluetoothFormat(value)) }

    // Other settings
    val otherSettings = uiState.otherSettings
    val toggleUtc = { value: Boolean -> uiState.sendAction(SettingsAction.ToggleUtc(value)) }
    val toggleUpdate = { value: Boolean -> uiState.sendAction(SettingsAction.ToggleUpdate(value)) }
    val toggleSweep = { value: Boolean -> uiState.sendAction(SettingsAction.ToggleSweep(value)) }
    val toggleSensor = { value: Boolean -> uiState.sendAction(SettingsAction.ToggleSensor(value)) }
    val uriHandler = LocalUriHandler.current
    val appUrl = stringResource(R.string.prefs_app_url)
    val donateUrl = stringResource(R.string.prefs_donate_url)
    val fdroidTitle = stringResource(R.string.prefs_fdroid_title)
    val fdroidUrl = stringResource(R.string.prefs_fdroid_url)
    val gitHubTitle = stringResource(R.string.prefs_github_title)
    val gitHubUrl = stringResource(R.string.prefs_github_url)
    val licenseUrl = stringResource(R.string.prefs_license_url)
    val privacyUrl = stringResource(R.string.prefs_privacy_url)

    ScreenColumn(
        topBar = { isVerticalLayout ->
            if (isVerticalLayout) {
                TopBar {
                    TopCard(onClick = { uriHandler.openUri(appUrl) }, version = uiState.appVersionName, modifier = Modifier.weight(1f))
                    PrimaryIconCard(onClick = { uriHandler.openUri(donateUrl) }, resId = R.drawable.ic_pound)
                }
                TopBar {
                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        BotCard(onClick = { uriHandler.openUri(fdroidUrl) }, R.drawable.ic_fdroid, fdroidTitle, modifier = Modifier.weight(1f))
                        BotCard(onClick = { uriHandler.openUri(gitHubUrl) }, R.drawable.ic_github, gitHubTitle, modifier = Modifier.weight(1f))
                    }
                    IconCard(action = { uriHandler.openUri(licenseUrl) }, resId = R.drawable.ic_license)
                    IconCard(action = { uriHandler.openUri(privacyUrl) }, resId = R.drawable.ic_policy)
                }
            } else {
                TopBar {
                    PrimaryIconCard(onClick = { uriHandler.openUri(donateUrl) }, resId = R.drawable.ic_pound)
                    TopCard(onClick = { uriHandler.openUri(appUrl) }, version = uiState.appVersionName, modifier = Modifier.weight(1f))
                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        BotCard(onClick = { uriHandler.openUri(fdroidUrl) }, R.drawable.ic_fdroid, fdroidTitle, modifier = Modifier.weight(1f))
                        BotCard(onClick = { uriHandler.openUri(gitHubUrl) }, R.drawable.ic_github, gitHubTitle, modifier = Modifier.weight(1f))
                    }
                    IconCard(action = { uriHandler.openUri(licenseUrl) }, resId = R.drawable.ic_license)
                    IconCard(action = { uriHandler.openUri(privacyUrl) }, resId = R.drawable.ic_policy)
                }
            }
        }
    ) { _ ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(320.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.clip(MaterialTheme.shapes.medium)
        ) {
            item {
                LocationCard(posSettings, setGpsPos, showPosDialog, showLocDialog, dismissPos, uiState.sendSystemAction)
            }
            item { DataCard(dataSettings, updateFromWeb, clearAllData, showDataSourcesDialog) }
            item { NetworkOutputCard(rcSettings, setRotatorState, setRotatorAddress, setRotatorPort) }
            item { BluetoothOutputCard(rcSettings, setBluetoothState, setBluetoothAddress, setBluetoothFormat) }
            item { OtherCard(otherSettings, toggleUtc, toggleUpdate, toggleSweep, toggleSensor) }
            item { CardCredits() }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LocationCardPreview() = MainTheme {
    val stationPos = GeoPos(0.0, 0.0, 0.0, "IO91vl", 0L)
    val settings = PositionSettings(true, stationPos, 0)
    LocationCard(settings = settings, setGpsPos = {}, showPosDialog = {}, {}, {}) {}
}

@Composable
private fun LocationCard(
    settings: PositionSettings,
    setGpsPos: () -> Unit,
    showPosDialog: () -> Unit,
    showLocDialog: () -> Unit,
    dismissPosMessage: () -> Unit,
    sendSystemAction: (SystemAction) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(id = R.string.prefs_loc_title),
                    color = MaterialTheme.colorScheme.primary
                )
                UpdateIndicator(isUpdating = settings.isUpdating, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = setUpdateTime(updateTime = settings.stationPos.timestamp))
            Spacer(modifier = Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Lat: ${settings.stationPos.latitude}°")
                Text(text = "Lon: ${settings.stationPos.longitude}°")
                Text(text = "Qth: ${settings.stationPos.qthLocator}")
            }
            Spacer(modifier = Modifier.height(1.dp))
            Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                CardButton(
                    onClick = setGpsPos,
                    text = stringResource(id = R.string.prefs_loc_gps_title),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                CardButton(
                    onClick = showPosDialog,
                    text = stringResource(id = R.string.prefs_loc_input_title),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                CardButton(
                    onClick = showLocDialog,
                    text = stringResource(id = R.string.prefs_loc_qth_title),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
    if (settings.messageResId != 0) {
        val errorString = stringResource(id = settings.messageResId)
        LaunchedEffect(key1 = settings.messageResId) {
            sendSystemAction(SystemAction.ShowToast(errorString))
            dismissPosMessage()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DataCardPreview() = MainTheme {
    val settings = DataSettings(true, 5000, 2500, 0L)
    DataCard(settings = settings, {}, {}, {})
}

@Composable
private fun DataCard(
    settings: DataSettings,
    updateFromWeb: () -> Unit,
    clearAllData: () -> Unit,
    showDataSourcesDialog: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(id = R.string.prefs_data_title),
                    color = MaterialTheme.colorScheme.primary
                )
                UpdateIndicator(isUpdating = settings.isUpdating, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = setUpdateTime(updateTime = settings.timestamp))
            Spacer(modifier = Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.prefs_data_entries, settings.entriesTotal))
                Text(text = stringResource(R.string.prefs_data_radios, settings.radiosTotal))
            }
            Spacer(modifier = Modifier.height(1.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CardButton(
                    onClick = updateFromWeb,
                    text = stringResource(id = R.string.prefs_data_update),
                    modifier = Modifier.weight(1f)
                )
                CardButton(
                    onClick = showDataSourcesDialog,
                    text = stringResource(id = R.string.prefs_data_import),
                    modifier = Modifier.weight(1f)
                )
                CardButton(
                    onClick = clearAllData,
                    text = stringResource(id = R.string.prefs_data_clear),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NetworkOutputCardPreview() = MainTheme {
    val settings = RCSettings(
        rotatorState = false,
        rotatorAddress = "127.0.0.1",
        rotatorPort = "4533",
        bluetoothState = false,
        bluetoothFormat = $$"W$AZ $EL",
        bluetoothName = "Name",
        bluetoothAddress = "00:0C:BF:13:80:5D"
    )
    NetworkOutputCard(settings)
}

@Composable
private fun NetworkOutputCard(
    settings: RCSettings,
    setRotatorState: (Boolean) -> Unit = {},
    setRotatorAddress: (String) -> Unit = {},
    setRotatorPort: (String) -> Unit = {}
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(
                text = stringResource(id = R.string.prefs_net_title),
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.prefs_net_switch))
                Switch(checked = settings.rotatorState, onCheckedChange = { setRotatorState(it) })
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = settings.rotatorAddress,
                    singleLine = true,
                    label = { Text(text = stringResource(R.string.prefs_net_ip_hint)) },
                    onValueChange = { setRotatorAddress(it) },
                    modifier = Modifier.weight(1.5f),
                    enabled = settings.rotatorState
                )
                OutlinedTextField(
                    value = settings.rotatorPort,
                    onValueChange = { setRotatorPort(it) },
                    label = { Text(text = stringResource(R.string.prefs_net_port_hint)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    enabled = settings.rotatorState
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BluetoothOutputCardPreview() = MainTheme {
    val settings = RCSettings(
        rotatorState = false,
        rotatorAddress = "127.0.0.1",
        rotatorPort = "4533",
        bluetoothState = false,
        bluetoothFormat = $$"W$AZ $EL",
        bluetoothName = "Name",
        bluetoothAddress = "00:0C:BF:13:80:5D"
    )
    BluetoothOutputCard(settings)
}

@Composable
private fun BluetoothOutputCard(
    settings: RCSettings,
    setBluetoothState: (Boolean) -> Unit = {},
    setBluetoothAddress: (String) -> Unit = {},
    setBluetoothFormat: (String) -> Unit = {}
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(
                text = stringResource(id = R.string.prefs_bt_title),
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.prefs_bt_switch))
                Switch(checked = settings.bluetoothState, onCheckedChange = { setBluetoothState(it) })
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = settings.bluetoothAddress,
                    singleLine = true,
                    label = { Text(text = stringResource(R.string.prefs_bt_device_hint)) },
                    onValueChange = { setBluetoothAddress(it) },
                    modifier = Modifier.weight(1.5f),
                    enabled = settings.bluetoothState
                )
                OutlinedTextField(
                    value = settings.bluetoothFormat,
                    onValueChange = { setBluetoothFormat(it) },
                    label = { Text(text = stringResource(R.string.prefs_bt_output_hint)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    enabled = settings.bluetoothState
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OtherCardPreview() = MainTheme {
    val values = OtherSettings(
        stateOfAutoUpdate = true,
        stateOfSensors = true,
        stateOfSweep = true,
        stateOfUtc = false,
        stateOfLightTheme = false,
        shouldSeeWarning = false,
        shouldSeeWelcome = false
    )
    OtherCard(settings = values, {}, {}, {}, {})
}

@Composable
private fun OtherCard(
    settings: OtherSettings,
    toggleUtc: (Boolean) -> Unit,
    toggleUpdate: (Boolean) -> Unit,
    toggleSweep: (Boolean) -> Unit,
    toggleSensor: (Boolean) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth().height(220.dp)) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(
                text = stringResource(id = R.string.prefs_other_title),
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.prefs_other_switch_utc))
                Switch(checked = settings.stateOfUtc, onCheckedChange = { toggleUtc(it) })
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.prefs_other_switch_update))
                Switch(checked = settings.stateOfAutoUpdate, onCheckedChange = { toggleUpdate(it) })
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.prefs_other_switch_sweep))
                Switch(checked = settings.stateOfSweep, onCheckedChange = { toggleSweep(it) })
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.prefs_other_switch_sensors))
                Switch(checked = settings.stateOfSensors, onCheckedChange = { toggleSensor(it) })
            }
        }
    }
}

@Composable
private fun setUpdateTime(updateTime: Long): String {
    val updateDate = if (updateTime != 0L) {
        val timePattern = stringResource(id = R.string.prefs_updated_time)
        SimpleDateFormat(timePattern, Locale.getDefault()).format(Date(updateTime))
    } else {
        stringResource(id = R.string.pass_time_placeholder)
    }
    return stringResource(id = R.string.prefs_updated_title, updateDate)
}

@Composable
private fun UpdateIndicator(isUpdating: Boolean, modifier: Modifier = Modifier) = if (isUpdating) {
    LinearProgressIndicator(modifier = modifier.padding(start = 6.dp))
} else {
    LinearProgressIndicator(
        progress = { 0f },
        drawStopIndicator = {},
        modifier = modifier.padding(start = 6.dp)
    )
}

@Preview(showBackground = true)
@Composable
private fun CardCreditsPreview() = MainTheme { CardCredits() }

@Composable
private fun CardCredits(modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier.fillMaxWidth().height(220.dp)) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).fillMaxHeight()
        ) {
            Text(
                text = stringResource(id = R.string.prefs_outro_title),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(id = R.string.prefs_outro_thanks)
            )
            Text(
                text = stringResource(id = R.string.prefs_outro_license),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun TopCard(onClick: () -> Unit, modifier: Modifier = Modifier, version: String) {
    ElevatedCard(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(48.dp)
                .clickable { onClick() }) {
            Spacer(Modifier)
            Icon(
                painter = painterResource(id = R.drawable.ic_satellites),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.prefs_app_title, version),
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp)
                    .infiniteMarquee()
            )
        }
    }
}

@Composable
private fun BotCard(onClick: () -> Unit, resId: Int, text: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(48.dp)
                .clickable { onClick() }) {
            Spacer(Modifier)
            Icon(painter = painterResource(id = resId), contentDescription = null)
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp)
                    .infiniteMarquee()
            )
        }
    }
}
