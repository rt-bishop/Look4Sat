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
package com.rtbishop.look4sat.domain.repository

import com.rtbishop.look4sat.domain.model.DatabaseState
import com.rtbishop.look4sat.domain.model.OtherSettings
import com.rtbishop.look4sat.domain.model.PassesSettings
import com.rtbishop.look4sat.domain.model.RCSettings
import com.rtbishop.look4sat.domain.model.DataSourcesSettings
import com.rtbishop.look4sat.domain.predict.GeoPos
import kotlinx.coroutines.flow.StateFlow

interface ISettingsRepo {

    val appVersionName: String

    //region # Satellites selection settings
    val selectedIds: StateFlow<List<Int>>
    val selectedTypes: StateFlow<List<String>>
    fun setSelectedIds(ids: List<Int>)
    fun setSelectedTypes(types: List<String>)
    //endregion

    //region # Passes filter settings
    val passesSettings: StateFlow<PassesSettings>
    fun setPassesSettings(settings: PassesSettings)
    //endregion

    //region # Station position settings
    val stationPosition: StateFlow<GeoPos>
    fun setStationPosition(latitude: Double, longitude: Double, altitude: Double): Boolean
    fun setStationPosition(): Boolean
    fun setStationPosition(locator: String): Boolean
    //endregion

    //region # Database update settings
    val databaseState: StateFlow<DatabaseState>
    fun getSatelliteTypesIds(types: List<String>): List<Int>
    fun setSatelliteTypeIds(type: String, ids: List<Int>)
    fun updateDatabaseState(state: DatabaseState)
    //endregion

    //region # RC settings
    val rcSettings: StateFlow<RCSettings>
    fun setBluetoothRotatorAddress(value: String)
    fun setBluetoothRotatorFormat(value: String)
    fun setBluetoothRotatorName(value: String)
    fun setBluetoothRotatorState(value: Boolean)
    fun setBluetoothFrequencyAddress(value: String)
    fun setBluetoothFrequencyFormat(value: String)
    fun setBluetoothFrequencyState(value: Boolean)
    fun setRotatorAddress(value: String)
    fun setRotatorPort(value: String)
    fun setRotatorState(value: Boolean)
    fun setRotatorFormat(value: String)
    fun setFrequencyAddress(value: String)
    fun setFrequencyPort(value: String)
    fun setFrequencyState(value: Boolean)
    fun setFrequencyFormat(value: String)
    //endregion

    //region # Other settings
    val otherSettings: StateFlow<OtherSettings>
    fun setStateOfAutoUpdate(value: Boolean)
    fun setStateOfSensors(value: Boolean)
    fun setStateOfSweep(value: Boolean)
    fun setStateOfUtc(value: Boolean)
    fun setStateOfLightTheme(value: Boolean)
    fun setWarningDismissed()
    fun setWhatsNewDismissed()
    //endregion

    //region # Transceivers settings
    val dataSourcesSettings: StateFlow<DataSourcesSettings>
    fun setUseCustomTle(value: Boolean)
    fun setUseCustomTransceivers(value: Boolean)
    fun setTleUrl(value: String)
    fun setTransceiversUrl(value: String)
    //endregion
}
