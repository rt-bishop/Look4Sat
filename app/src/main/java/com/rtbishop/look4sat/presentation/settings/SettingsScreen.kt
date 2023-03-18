package com.rtbishop.look4sat.presentation.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.rtbishop.look4sat.BuildConfig
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.model.GeoPos
import com.rtbishop.look4sat.presentation.CardButton
import com.rtbishop.look4sat.presentation.MainTheme
import com.rtbishop.look4sat.presentation.dialogs.LocatorDialog
import com.rtbishop.look4sat.presentation.dialogs.PositionDialog
import com.rtbishop.look4sat.presentation.gotoUrl
import com.rtbishop.look4sat.presentation.showToast
import java.text.SimpleDateFormat
import java.util.*

private const val POLICY_URL = "https://sites.google.com/view/look4sat-privacy-policy/home"
private const val LICENSE_URL = "https://www.gnu.org/licenses/gpl-3.0.html"
private const val GITHUB_URL = "https://github.com/rt-bishop/Look4Sat/"
private const val DONATE_URL = "https://ko-fi.com/rt_bishop"
private const val FDROID_URL = "https://f-droid.org/en/packages/com.rtbishop.look4sat/"

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current

    // Permissions setup
    val bluetoothContract = ActivityResultContracts.RequestPermission()
    val bluetoothError = stringResource(R.string.BTremote_perm_error)
    val bluetoothPerm = when {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> Manifest.permission.BLUETOOTH
        else -> Manifest.permission.BLUETOOTH_CONNECT
    }
    val bluetoothRequest = rememberLauncherForActivityResult(bluetoothContract) { isGranted ->
//        viewModel.setBTEnabled(isGranted)
        if (!isGranted) showToast(context, bluetoothError)
    }
    val locationContract = ActivityResultContracts.RequestMultiplePermissions()
    val locationError = stringResource(R.string.location_gps_error)
    val locationPermCoarse = Manifest.permission.ACCESS_COARSE_LOCATION
    val locationPermFine = Manifest.permission.ACCESS_FINE_LOCATION
    val locationRequest = rememberLauncherForActivityResult(locationContract) { permissions ->
        when {
            permissions[locationPermFine] == true -> viewModel.locationSettings.value.setGpsLoc()
            permissions[locationPermCoarse] == true -> viewModel.locationSettings.value.setGpsLoc()
            else -> showToast(context, locationError)
        }
    }
    val contentContract = ActivityResultContracts.GetContent()
    val contentRequest = rememberLauncherForActivityResult(contentContract) { uri ->
        uri?.let { viewModel.dataSettings.value.updateFromFile(uri.toString()) }
    }

    // Location settings
    val locSettings = viewModel.locationSettings.value
    val setGpsLoc = { locationRequest.launch(arrayOf(locationPermCoarse, locationPermFine)) }
    val showPosDialog = rememberSaveable { mutableStateOf(false) }
    val togglePosDialog = { showPosDialog.value = showPosDialog.value.not() }
    if (showPosDialog.value) {
        PositionDialog(
            locSettings.stationPos.latitude,
            locSettings.stationPos.longitude,
            togglePosDialog,
            locSettings.setManualLoc
        )
    }
    val showLocDialog = rememberSaveable { mutableStateOf(false) }
    val toggleLocDialog = { showLocDialog.value = showLocDialog.value.not() }
    if (showLocDialog.value) {
        LocatorDialog(locSettings.stationPos.qthLocator, toggleLocDialog, locSettings.setQthLoc)
    }

    // Screen setup
    LazyColumn(
        modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item { CardAbout(BuildConfig.VERSION_NAME) }
        item { LocationCard(locSettings, setGpsLoc, togglePosDialog, toggleLocDialog) }
        item { DataCard(viewModel.dataSettings.value) { contentRequest.launch("*/*") } }
        item { OtherCard(viewModel.otherSettings.value) }
        item { CardCredits() }
    }
}

@Preview(showBackground = true)
@Composable
private fun CardAboutPreview() = MainTheme { CardAbout(version = "4.0.0") }

@Composable
private fun CardAbout(version: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
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
                modifier = modifier.padding(top = 4.dp, bottom = 2.dp)
            )
            Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                CardButton(
                    onClick = { gotoUrl(context, GITHUB_URL) },
                    text = stringResource(id = R.string.btn_github),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                CardButton(
                    onClick = { gotoUrl(context, DONATE_URL) },
                    text = stringResource(id = R.string.btn_donate),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
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
private fun LocationCardPreview() = MainTheme {
    val stationPos = GeoPos(0.0, 0.0, 0.0, "IO91vl", 0L)
    LocationCard(
        settings = LocationSettings(true, stationPos, {}, { _, _ -> }, { }),
        setGpsLoc = {}, togglePosDialog = {}) {}
}

@Composable
private fun LocationCard(
    settings: LocationSettings,
    setGpsLoc: () -> Unit,
    togglePosDialog: () -> Unit,
    toggleLocDialog: () -> Unit
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
                    onClick = setGpsLoc,
                    text = stringResource(id = R.string.btn_gps),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                CardButton(
                    onClick = togglePosDialog,
                    text = stringResource(id = R.string.btn_manual),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                CardButton(
                    onClick = toggleLocDialog,
                    text = stringResource(id = R.string.btn_qth),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DataCardPreview() = MainTheme {
    DataCard(settings = DataSettings(
        true, 0L, 5000, 2500, {}, { }, {})
    ) {}
}

@Composable
private fun DataCard(settings: DataSettings, updateFromFile: () -> Unit) {
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
            Text(text = setUpdateTime(updateTime = settings.lastUpdated))
            Spacer(modifier = Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Satellites: ${settings.satsTotal}")
                Spacer(modifier = Modifier.weight(1f))
                Text(text = "Transceivers: ${settings.radiosTotal}")
            }
            Spacer(modifier = Modifier.height(1.dp))
            Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                CardButton(
                    onClick = settings.updateFromWeb,
                    text = stringResource(id = R.string.btn_web),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                CardButton(
                    onClick = updateFromFile,
                    text = stringResource(id = R.string.btn_file),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                CardButton(
                    onClick = settings.clearAllData,
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
    OtherCard(settings = OtherSettings(
        getUtc = false, getUpdate = true, getSweep = true, getSensor = true,
        setUtc = {}, setUpdate = {}, setSweep = {}, setSensor = {})
    )
}

@Composable
private fun OtherCard(settings: OtherSettings) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.other_switch_utc))
                Switch(checked = settings.getUtc, onCheckedChange = { settings.setUtc(it) })
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.other_switch_update))
                Switch(checked = settings.getUpdate, onCheckedChange = { settings.setUpdate(it) })
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.other_switch_sweep))
                Switch(checked = settings.getSweep, onCheckedChange = { settings.setSweep(it) })
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.other_switch_sensors))
                Switch(checked = settings.getSensor, onCheckedChange = { settings.setSensor(it) })
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CardCreditsPreview() = MainTheme { CardCredits() }

@Composable
private fun CardCredits(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = stringResource(id = R.string.outro_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = modifier.padding(6.dp)
            )
            Text(
                text = stringResource(id = R.string.outro_thanks),
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(id = R.string.outro_license),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = modifier.padding(6.dp)
            )
            Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                CardButton(
                    onClick = { gotoUrl(context, LICENSE_URL) },
                    text = stringResource(id = R.string.btn_license),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                CardButton(
                    onClick = { gotoUrl(context, POLICY_URL) },
                    text = stringResource(id = R.string.btn_privacy),
                    modifier = Modifier.weight(1f)
                )
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
    LinearProgressIndicator(
        modifier = modifier.padding(start = 6.dp),
        trackColor = MaterialTheme.colorScheme.inverseSurface
    )
} else {
    LinearProgressIndicator(
        progress = 0f,
        modifier = modifier.padding(start = 6.dp),
        trackColor = MaterialTheme.colorScheme.inverseSurface
    )
}
