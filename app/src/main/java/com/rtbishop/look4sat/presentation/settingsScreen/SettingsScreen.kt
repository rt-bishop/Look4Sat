package com.rtbishop.look4sat.presentation.settingsScreen

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rtbishop.look4sat.BuildConfig
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.domain.model.OtherSettings
import com.rtbishop.look4sat.presentation.CardButton
import com.rtbishop.look4sat.presentation.MainTheme
import com.rtbishop.look4sat.presentation.gotoUrl
import com.rtbishop.look4sat.presentation.showToast

private const val POLICY_URL = "https://sites.google.com/view/look4sat-privacy-policy/home"
private const val LICENSE_URL = "https://www.gnu.org/licenses/gpl-3.0.html"
private const val GITHUB_URL = "https://github.com/rt-bishop/Look4Sat/"
private const val DONATE_URL = "https://ko-fi.com/rt_bishop"
private const val FDROID_URL = "https://f-droid.org/en/packages/com.rtbishop.look4sat/"

@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = hiltViewModel()

    // Permissions setup
    val bluetoothContract = ActivityResultContracts.RequestPermission()
    val bluetoothError = stringResource(R.string.BTremote_perm_error)
    val bluetoothPerm = when {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> Manifest.permission.BLUETOOTH
        else -> Manifest.permission.BLUETOOTH_CONNECT
    }
    val bluetoothRequest = rememberLauncherForActivityResult(bluetoothContract) { isGranted ->
        viewModel.setBTEnabled(isGranted)
        if (!isGranted) showToast(context, bluetoothError)
    }
    val locationContract = ActivityResultContracts.RequestMultiplePermissions()
    val locationError = stringResource(R.string.location_gps_error)
    val locationPermCoarse = Manifest.permission.ACCESS_COARSE_LOCATION
    val locationPermFine = Manifest.permission.ACCESS_FINE_LOCATION
    val locationRequest = rememberLauncherForActivityResult(locationContract) { permissions ->
        when {
            permissions[locationPermFine] == true -> viewModel.setPositionFromGps()
            permissions[locationPermCoarse] == true -> viewModel.setPositionFromNet()
            else -> showToast(context, locationError)
        }
    }
    val contentContract = ActivityResultContracts.GetContent()
    val contentRequest = rememberLauncherForActivityResult(contentContract) { uri ->
        uri?.let { viewModel.updateFromFile(uri.toString()) }
    }

    // Location settings
    val setGpsLoc = { locationRequest.launch(arrayOf(locationPermCoarse, locationPermFine)) }
    val geoPos = viewModel.getStationPosition()
    val showPosDialog = rememberSaveable { mutableStateOf(false) }
    val togglePosDialog = { showPosDialog.value = showPosDialog.value.not() }
    val savePos = { lat: Double, lon: Double -> viewModel.setStationPosition(lat, lon) }
    if (showPosDialog.value) {
        PositionDialog(lat = geoPos.lat, lon = geoPos.lon, hide = togglePosDialog, save = savePos)
    }
    val qthLocator = viewModel.getStationLocator()
    val showLocDialog = rememberSaveable { mutableStateOf(false) }
    val toggleLocDialog = { showLocDialog.value = showLocDialog.value.not() }
    val saveLocator = { locator: String -> viewModel.setPositionFromQth(locator) }
    if (showLocDialog.value) {
        LocatorDialog(qthLocator = qthLocator, hide = toggleLocDialog, save = saveLocator)
    }
    // Data settings
    val updateFromWeb = { viewModel.updateFromWeb() }
    val updateFromFile = { contentRequest.launch("*/*") }
    val clearAllData = { viewModel.clearAllData() }
    // Other settings
    val otherSettings = viewModel.otherSettings.collectAsState()
    val setUtc = { value: Boolean -> viewModel.setUtcState(value) }
    val setUpdate = { value: Boolean -> viewModel.setUpdateState(value) }
    val setSweep = { value: Boolean -> viewModel.setSweepState(value) }
    val setSensor = { value: Boolean -> viewModel.setSensorState(value) }

    // Screen setup
    LazyColumn(
        modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item { CardAbout(BuildConfig.VERSION_NAME) }
        item { LocationCard(setGpsLoc, togglePosDialog, toggleLocDialog) }
        item { DataCard(updateFromWeb, updateFromFile, clearAllData) }
        item { OtherCard(otherSettings.value, setUtc, setUpdate, setSweep, setSensor) }
        item { CardCredits() }
    }
}

@Composable
private fun CardAbout(version: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.Center) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_entries),
                    tint = MaterialTheme.colorScheme.secondary,
                    contentDescription = null,
                    modifier = Modifier
                        .size(96.dp)
                        .padding(top = 8.dp, end = 8.dp)
                )
                Column {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        fontSize = 48.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = stringResource(id = R.string.app_version, version), fontSize = 20.sp
                    )
                }
            }
            Text(
                text = stringResource(id = R.string.app_subtitle),
                fontSize = 20.sp,
                modifier = modifier.padding(top = 4.dp)
            )
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.padding(4.dp)
            ) {
                CardButton(
                    onClick = { gotoUrl(context, GITHUB_URL) },
                    text = stringResource(id = R.string.btn_github),
                    modifier = Modifier.weight(1f)
                )
                CardButton(
                    onClick = { gotoUrl(context, DONATE_URL) },
                    text = stringResource(id = R.string.btn_donate),
                    modifier = Modifier.weight(1f)
                )
                CardButton(
                    onClick = { gotoUrl(context, FDROID_URL) },
                    text = stringResource(id = R.string.btn_fdroid),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LocationCardPreview() {
    MainTheme { LocationCard({}, {}, {}) }
}

@Composable
private fun LocationCard(
    setGpsLoc: () -> Unit, togglePosDialog: () -> Unit, toggleLocDialog: () -> Unit
) {
    val context = LocalContext.current
    // setPositionText(viewModel.getStationPosition())
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Station position")
                LinearProgressIndicator(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 6.dp, end = 6.dp)
                )
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Updated: 3 Mar 2023 - 15:51")
                Text(text = "QTH: IO91vl")
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Latitude: *")
                Text(text = "Longitude: *")
            }
            Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                CardButton(
                    onClick = setGpsLoc, // locationRequest.launch(arrayOf(locationFine, locationCoarse))
                    text = stringResource(id = R.string.btn_gps), modifier = Modifier.weight(1f)
                )
                CardButton(
                    onClick = togglePosDialog, // open Position dialog
                    text = stringResource(id = R.string.btn_manual), modifier = Modifier.weight(1f)
                ) // viewModel.setStationPosition(position.first, position.second)
                CardButton(
                    onClick = toggleLocDialog, // open Locator dialog
                    text = stringResource(id = R.string.btn_qth), modifier = Modifier.weight(1f)
                ) // viewModel.setPositionFromQth(locator)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DataCardPreview() {
    MainTheme { DataCard({}, {}, {}) }
}

@Composable
private fun DataCard(updateOnline: () -> Unit, updateFile: () -> Unit, clearData: () -> Unit) {
    val context = LocalContext.current
    // setUpdateTime(viewModel.getLastUpdateTime())
    // viewModel.entriesTotal
    // viewModel.radiosTotal
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Satellite data")
                LinearProgressIndicator(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 6.dp, end = 6.dp)
                )
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Updated: 3 Mar 2023 - 15:51")
                Text(text = "")
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Satellites: 7749")
                Text(text = "Transceivers: 2240")
            }
            Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                CardButton(
                    onClick = updateOnline, // viewModel.updateFromWeb()
                    text = stringResource(id = R.string.btn_web), modifier = Modifier.weight(1f)
                )
                CardButton(
                    onClick = updateFile, // contentRequest.launch("*/*")
                    text = stringResource(id = R.string.btn_file), modifier = Modifier.weight(1f)
                )
                CardButton(
                    onClick = clearData, // viewModel.clearAllData()
                    text = stringResource(id = R.string.btn_clear), modifier = Modifier.weight(1f)
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
private fun OtherCardPreview() {
    MainTheme { }
}

@Composable
private fun OtherCard(
    settings: OtherSettings,
    setUtc: (Boolean) -> Unit,
    setUpdate: (Boolean) -> Unit,
    setSweep: (Boolean) -> Unit,
    setSensor: (Boolean) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(4.dp)) {
            Text(text = stringResource(id = R.string.other_title))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.other_switch_utc))
                Switch(checked = settings.isUtcEnabled, onCheckedChange = { setUtc(it) })
            } // viewModel.setUseUTC(isChecked)
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.other_switch_update))
                Switch(checked = settings.isUpdateEnabled, onCheckedChange = { setUpdate(it) })
            } // AutoUpdateEnabled(isChecked)
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.other_switch_sweep))
                Switch(checked = settings.isSweepEnabled, onCheckedChange = { setSweep(it) })
            } // viewModel.setShowSweep(isChecked)
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.other_switch_sensors))
                Switch(checked = settings.isSensorEnabled, onCheckedChange = { setSensor(it) })
            } // viewModel.setUseCompass(isChecked)
        }
    }
}

//
//private fun handleStationPosition(pos: DataState<GeoPos>) {
//    when (pos) {
//        is DataState.Success -> {
//            setPositionText(pos.data)
//            binding.settingsLocation.locationProgress.isIndeterminate = false
//            viewModel.setPositionHandled()
//            showToast(getString(R.string.location_success))
//        }
//        is DataState.Error -> {
//            binding.settingsLocation.locationProgress.isIndeterminate = false
//            viewModel.setPositionHandled()
//            showToast(pos.message.toString())
//        }
//        DataState.Loading -> {
//            binding.settingsLocation.locationProgress.isIndeterminate = true
//        }
//        DataState.Handled -> {}
//    }
//}
//
//private fun setPositionText(geoPos: GeoPos) {
//    binding.run {
//        val latFormat = getString(R.string.location_lat)
//        val lonFormat = getString(R.string.location_lon)
//        settingsLocation.locationLat.text = String.format(latFormat, geoPos.lat)
//        settingsLocation.locationLon.text = String.format(lonFormat, geoPos.lon)
//    }
//}
//
//private fun handleSatState(state: DataState<Long>) {
//    when (state) {
//        is DataState.Success -> {
//            binding.settingsData.dataProgress.isIndeterminate = false
//            setUpdateTime(state.data)
//            viewModel.setUpdateHandled()
//            if (state.data == 0L) {
//                showToast(getString(R.string.data_clear_success))
//            } else {
//                showToast(getString(R.string.data_success))
//            }
//        }
//        is DataState.Error -> {
//            binding.settingsData.dataProgress.isIndeterminate = false
//            viewModel.setUpdateHandled()
//            showToast(getString(R.string.data_error))
//        }
//        is DataState.Loading -> {
//            binding.settingsData.dataProgress.isIndeterminate = true
//        }
//        is DataState.Handled -> {}
//    }
//}
//
//private fun setUpdateTime(updateTime: Long) {
//    val updatePattern = getString(R.string.data_update)
//    val updateDate = if (updateTime == 0L) {
//        getString(R.string.pass_placeholder)
//    } else {
//        SimpleDateFormat("d MMM yyyy - HH:mm:ss", Locale.getDefault()).format(Date(updateTime))
//    }
//    binding.settingsData.dataUpdate.text = String.format(updatePattern, updateDate)
//}
//
@Composable
private fun CardCredits(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(id = R.string.outro_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = modifier.padding(8.dp)
            )
            Text(
                text = stringResource(id = R.string.outro_thanks),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            )
            Text(
                text = stringResource(id = R.string.outro_license),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = modifier.padding(8.dp)
            )
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.padding(4.dp)
            ) {
                CardButton(
                    onClick = { gotoUrl(context, LICENSE_URL) },
                    text = stringResource(id = R.string.btn_license),
                    modifier = Modifier.weight(1f)
                )
                CardButton(
                    onClick = { gotoUrl(context, POLICY_URL) },
                    text = stringResource(id = R.string.btn_privacy),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
