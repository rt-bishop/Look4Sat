package com.rtbishop.look4sat.presentation.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.domain.model.OtherSettings
import com.rtbishop.look4sat.domain.predict.GeoPos
import com.rtbishop.look4sat.presentation.MainTheme
import com.rtbishop.look4sat.presentation.Screen
import com.rtbishop.look4sat.presentation.components.CardButton
import org.osmdroid.library.BuildConfig
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
    val bluetoothError = stringResource(R.string.BTremote_perm_error)
    val bluetoothPerm = when {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> Manifest.permission.BLUETOOTH
        else -> Manifest.permission.BLUETOOTH_CONNECT
    }
    val bluetoothRequest = rememberLauncherForActivityResult(bluetoothContract) { isGranted ->
        uiState.sendRCAction(RCAction.SetBluetoothState(isGranted))
        if (!isGranted) uiState.sendSystemAction(SystemAction.ShowToast(bluetoothError))
    }
    val locationContract = ActivityResultContracts.RequestMultiplePermissions()
    val locationError = stringResource(R.string.location_gps_error)
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
    val contentRequest = rememberLauncherForActivityResult(contentContract) { uri ->
        uri?.let { uiState.sendAction(SettingsAction.UpdateFromFile(uri.toString())) }
    }

    // Position settings
    val positionSettings = uiState.positionSettings
    val stationPos = positionSettings.stationPos
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
        PositionDialog(stationPos.latitude, stationPos.longitude, showPosDialog, setGeoPos)
    }
    val locDialogState = rememberSaveable { mutableStateOf(false) }
    val showLocDialog = { locDialogState.value = locDialogState.value.not() }
    if (locDialogState.value) {
        LocatorDialog(positionSettings.stationPos.qthLocator, showLocDialog, setQthPos)
    }

    // Data settings
    val dataSettings = uiState.dataSettings
    val updateFromWeb: () -> Unit = { uiState.sendAction(SettingsAction.UpdateFromWeb) }
    val updateFromFile = { contentRequest.launch("*/*") }
    val clearAllData: () -> Unit = { uiState.sendAction(SettingsAction.ClearAllData) }

    // Other settings
    val otherSettings = uiState.otherSettings
    val toggleUtc = { value: Boolean -> uiState.sendAction(SettingsAction.ToggleUtc(value)) }
    val toggleUpdate = { value: Boolean -> uiState.sendAction(SettingsAction.ToggleUpdate(value)) }
    val toggleSweep = { value: Boolean -> uiState.sendAction(SettingsAction.ToggleSweep(value)) }
    val toggleSensor = { value: Boolean -> uiState.sendAction(SettingsAction.ToggleSensor(value)) }
    val toggleLightTheme = { value: Boolean ->
        uiState.sendAction(SettingsAction.ToggleLightTheme(value))
    }

    // Screen setup
    LazyColumn(
        modifier = Modifier.padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item { CardAbout(uiState.appVersionName, uiState.sendSystemAction) }
        item {
            LocationCard(
                positionSettings,
                setGpsPos,
                showPosDialog,
                showLocDialog,
                dismissPos,
                uiState.sendSystemAction
            )
        }
        item { DataCard(dataSettings, updateFromWeb, updateFromFile, clearAllData) }
        item {
            OtherCard(
                otherSettings,
                toggleUtc,
                toggleUpdate,
                toggleSweep,
                toggleSensor,
                toggleLightTheme
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CardAboutPreview() = MainTheme { CardAbout(version = "4.0.0") }

@Composable
private fun CardAbout(version: String, sendUrlAction: (SystemAction) -> Unit = {}) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_sputnik),
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp)
                )
                Column {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        fontSize = 48.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(id = R.string.app_version, version), fontSize = 20.sp
                    )
                }
            }
            Text(
                text = stringResource(id = R.string.app_subtitle),
                fontSize = 20.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )
            Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                CardButton(
                    onClick = { sendUrlAction(SystemAction.OpenGitHub) },
                    text = stringResource(id = R.string.btn_github),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                CardButton(
                    onClick = { sendUrlAction(SystemAction.OpenDonate) },
                    text = stringResource(id = R.string.btn_donate),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                CardButton(
                    onClick = { sendUrlAction(SystemAction.OpenFDroid) },
                    text = stringResource(id = R.string.btn_fdroid),
                    modifier = Modifier.weight(1f)
                )
            }
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
                    text = stringResource(id = R.string.location_title),
                    color = MaterialTheme.colorScheme.primary
                )
                UpdateIndicator(isUpdating = settings.isUpdating, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = setUpdateTime(updateTime = settings.stationPos.timestamp))
            Spacer(modifier = Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "Lat: ${settings.stationPos.latitude}°",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start
                )
                Text(
                    text = "Lon: ${settings.stationPos.longitude}°",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Qth: ${settings.stationPos.qthLocator}",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
            }
            Spacer(modifier = Modifier.height(1.dp))
            Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                CardButton(
                    onClick = setGpsPos,
                    text = stringResource(id = R.string.btn_gps),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                CardButton(
                    onClick = showPosDialog,
                    text = stringResource(id = R.string.btn_manual),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                CardButton(
                    onClick = showLocDialog,
                    text = stringResource(id = R.string.btn_qth),
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
    DataCard(settings = settings, {}, {}) {}
}

@Composable
private fun DataCard(
    settings: DataSettings,
    updateFromWeb: () -> Unit,
    updateFromFile: () -> Unit,
    clearAllData: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(id = R.string.data_title),
                    color = MaterialTheme.colorScheme.primary
                )
                UpdateIndicator(isUpdating = settings.isUpdating, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = setUpdateTime(updateTime = settings.timestamp))
            Spacer(modifier = Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Satellites: ${settings.entriesTotal}")
                Spacer(modifier = Modifier.weight(1f))
                Text(text = "Transceivers: ${settings.radiosTotal}")
            }
            Spacer(modifier = Modifier.height(1.dp))
            Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                CardButton(
                    onClick = { updateFromWeb() },
                    text = stringResource(id = R.string.btn_web),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                CardButton(
                    onClick = { updateFromFile() },
                    text = stringResource(id = R.string.btn_file),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                CardButton(
                    onClick = { clearAllData() },
                    text = stringResource(id = R.string.btn_clear),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

//private fun setupRemoteCard() {
//    binding.run {
//        settingsRemote.remoteSwitch.apply {
//            isChecked = viewModel.getRotatorEnabled()
//            settingsRemote.remoteIp.isEnabled = isChecked
//            settingsRemote.remoteIpEdit.setText(viewModel.getRotatorServer())
//            settingsRemote.remotePort.isEnabled = isChecked
//            settingsRemote.remotePortEdit.setText(viewModel.getRotatorPort())
//            setOnCheckedChangeListener { _, isChecked ->
//                viewModel.setRotatorEnabled(isChecked)
//                settingsRemote.remoteIp.isEnabled = isChecked
//                settingsRemote.remotePort.isEnabled = isChecked
//            }
//        }
//        settingsRemote.remoteIpEdit.doOnTextChanged { text, _, _, _ ->
//            if (text.toString().isValidIPv4()) viewModel.setRotatorServer(text.toString())
//        }
//        settingsRemote.remotePortEdit.doOnTextChanged { text, _, _, _ ->
//            if (text.toString().isValidPort()) viewModel.setRotatorPort(text.toString())
//        }
//    }
//}
//
//private fun setupBTCard() {
//    binding.run {
//        settingsBtremote.BTremoteSwitch.apply {
//            isChecked = viewModel.getBTEnabled()
//            settingsBtremote.BTremoteAddress.isEnabled = isChecked
//            settingsBtremote.BTAddressEdit.setText(viewModel.getBTDeviceAddr())
//            settingsBtremote.BTremoteFormat.isEnabled = isChecked
//            settingsBtremote.BTFormatEdit.setText(viewModel.getBTFormat())
//            setOnCheckedChangeListener { _, isChecked ->
//                toggleBTstate(isChecked)
//                bluetoothRequest.launch(bluetooth)
//            }
//        }
//        settingsBtremote.BTAddressEdit.doOnTextChanged { text, _, _, _ ->
//            viewModel.setBTDeviceAddr(text.toString())
//        }
//        settingsBtremote.BTFormatEdit.doOnTextChanged { text, _, _, _ ->
//            viewModel.setBTFormat(text.toString())
//        }
//    }
//}
//
@Preview(showBackground = true)
@Composable
private fun OtherCardPreview() = MainTheme {
    val values = OtherSettings(
        stateOfAutoUpdate = true,
        stateOfSensors = true,
        stateOfSweep = true,
        stateOfUtc = false,
        stateOfLightTheme = false
    )
    OtherCard(settings = values, {}, {}, {}, {}, {})
}

@Composable
private fun OtherCard(
    settings: OtherSettings,
    toggleUtc: (Boolean) -> Unit,
    toggleUpdate: (Boolean) -> Unit,
    toggleSweep: (Boolean) -> Unit,
    toggleSensor: (Boolean) -> Unit,
    toggleLightTheme: (Boolean) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.other_switch_utc))
                Switch(checked = settings.stateOfUtc, onCheckedChange = { toggleUtc(it) })
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.other_switch_update))
                Switch(checked = settings.stateOfAutoUpdate, onCheckedChange = { toggleUpdate(it) })
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.other_switch_sweep))
                Switch(checked = settings.stateOfSweep, onCheckedChange = { toggleSweep(it) })
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.other_switch_sensors))
                Switch(checked = settings.stateOfSensors, onCheckedChange = { toggleSensor(it) })
            }
            if (BuildConfig.DEBUG) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(id = R.string.other_switch_light_theme))
                    Switch(
                        checked = settings.stateOfLightTheme,
                        onCheckedChange = { toggleLightTheme(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun setUpdateTime(updateTime: Long): String {
    val updateDate = if (updateTime != 0L) {
        val timePattern = stringResource(id = R.string.last_updated_pattern)
        SimpleDateFormat(timePattern, Locale.getDefault()).format(Date(updateTime))
    } else {
        stringResource(id = R.string.pass_placeholder)
    }
    return stringResource(id = R.string.data_update, updateDate)
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
