package com.rtbishop.look4sat.presentation.settings

import com.rtbishop.look4sat.domain.model.OtherSettings
import com.rtbishop.look4sat.domain.model.RCSettings
import com.rtbishop.look4sat.domain.predict.GeoPos
import com.rtbishop.look4sat.domain.predict.OrbitalPass

data class PositionSettings(
    val isUpdating: Boolean, val stationPos: GeoPos, val messageResId: Int
)

data class DataSettings(
    val isUpdating: Boolean, val entriesTotal: Int, val radiosTotal: Int, val timestamp: Long
)

data class SettingsState(
    val nextPass: OrbitalPass,
    val nextTime: String,
    val isNextTimeAos: Boolean,
    val appVersionName: String,
    val positionSettings: PositionSettings,
    val dataSettings: DataSettings,
    val otherSettings: OtherSettings,
    val rcSettings: RCSettings,
    val sendAction: (SettingsAction) -> Unit,
    val sendRCAction: (RCAction) -> Unit,
    val sendSystemAction: (SystemAction) -> Unit
)

sealed class SettingsAction {
    data object SetGpsPosition : SettingsAction()
    data class SetGeoPosition(val latitude: Double, val longitude: Double) : SettingsAction()
    data class SetQthPosition(val locator: String) : SettingsAction()
    data object DismissPosMessages : SettingsAction()
    data object UpdateFromWeb : SettingsAction()
    data class UpdateFromFile(val uri: String) : SettingsAction()
    data object ClearAllData : SettingsAction()
    data class ToggleUtc(val value: Boolean) : SettingsAction()
    data class ToggleUpdate(val value: Boolean) : SettingsAction()
    data class ToggleSweep(val value: Boolean) : SettingsAction()
    data class ToggleSensor(val value: Boolean) : SettingsAction()
    data class ToggleLightTheme(val value: Boolean) : SettingsAction()
}

sealed class SystemAction {
    data class ShowToast(val message: String) : SystemAction()
}

sealed class RCAction {
    data class SetRotatorState(val value: Boolean) : RCAction()
    data class SetRotatorAddress(val value: String) : RCAction()
    data class SetRotatorPort(val value: String) : RCAction()
    data class SetBluetoothState(val value: Boolean) : RCAction()
    data class SetBluetoothFormat(val value: String) : RCAction()
    data class SetBluetoothName(val value: String) : RCAction()
    data class SetBluetoothAddress(val value: String) : RCAction()
}
