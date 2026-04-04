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

import com.rtbishop.look4sat.core.domain.model.DataSourcesSettings
import com.rtbishop.look4sat.core.domain.model.OtherSettings
import com.rtbishop.look4sat.core.domain.model.RCSettings
import com.rtbishop.look4sat.core.domain.predict.GeoPos

data class PositionSettings(
    val isUpdating: Boolean, val stationPos: GeoPos, val messageResId: Int
)

data class DataSettings(
    val isUpdating: Boolean, val entriesTotal: Int, val radiosTotal: Int, val timestamp: Long
)

data class SettingsState(
    val appVersionName: String,
    val positionSettings: PositionSettings,
    val dataSettings: DataSettings,
    val otherSettings: OtherSettings,
    val rcSettings: RCSettings,
    val dataSourcesSettings: DataSourcesSettings,
    val sendAction: (SettingsAction) -> Unit,
    val sendRCAction: (RCAction) -> Unit,
    val sendSystemAction: (SystemAction) -> Unit,
    val sendDataSourcesAction: (DataSourcesAction) -> Unit
)

sealed class SettingsAction {
    data object SetGpsPosition : SettingsAction()
    data class SetGeoPosition(val latitude: Double, val longitude: Double) : SettingsAction()
    data class SetQthPosition(val locator: String) : SettingsAction()
    data object DismissPosMessages : SettingsAction()
    data object UpdateFromWeb : SettingsAction()
    data class UpdateTLEFromFile(val uri: String) : SettingsAction()
    data class UpdateTransceiversFromFile(val uri: String) : SettingsAction()
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
    data class Update(val settings: RCSettings) : RCAction()
}

sealed class DataSourcesAction {
    data class Update(val settings: DataSourcesSettings) : DataSourcesAction()
}
