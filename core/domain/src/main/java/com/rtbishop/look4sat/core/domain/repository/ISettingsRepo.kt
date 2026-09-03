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
package com.rtbishop.look4sat.core.domain.repository

import com.rtbishop.look4sat.core.domain.model.DataSourcesSettings
import com.rtbishop.look4sat.core.domain.model.DatabaseState
import com.rtbishop.look4sat.core.domain.model.OtherSettings
import com.rtbishop.look4sat.core.domain.model.PassesSettings
import com.rtbishop.look4sat.core.domain.model.RCSettings
import com.rtbishop.look4sat.core.domain.model.RadioControlSettings
import com.rtbishop.look4sat.core.domain.predict.GeoPos
import kotlinx.coroutines.flow.StateFlow

interface ISettingsRepo {

    val appVersionName: String

    //region # Satellites selection settings
    val selectedIds: StateFlow<List<Int>>
    val selectedSatModes: StateFlow<List<String>>
    fun setSelectedIds(ids: List<Int>)
    fun setSelectedSatModes(modes: List<String>)
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
    fun updateDatabaseState(state: DatabaseState)
    //endregion

    //region # RC settings
    val rcSettings: StateFlow<RCSettings>
    fun updateRCSettings(settings: RCSettings)
    //endregion

    //region # Other settings
    val otherSettings: StateFlow<OtherSettings>
    fun updateOtherSettings(transform: (OtherSettings) -> OtherSettings)
    fun setWarningDismissed() = updateOtherSettings { it.copy(shouldSeeWarning = false) }
    fun setWhatsNewDismissed() = updateOtherSettings { it.copy(shouldSeeWhatsNew = false) }
    //endregion

    //region # Transceivers settings
    val dataSourcesSettings: StateFlow<DataSourcesSettings>
    fun updateDataSourcesSettings(settings: DataSourcesSettings)
    //endregion

    //region # Data sources status
    val dataSourcesStatus: StateFlow<Map<String, Int>>
    fun updateDataSourcesStatus(status: Map<String, Int>)
    //endregion

    //region # Radio control settings
    val radioControlSettings: StateFlow<RadioControlSettings>
    fun updateRadioControlSettings(settings: RadioControlSettings)
    //endregion

    //region # Per-satellite calculator offset settings
    fun getSatelliteOffset(catnum: Int): String
    fun setSatelliteOffset(catnum: Int, offset: String)
    //endregion

    //region # AMSAT status report settings
    fun getAmSatCallsign(): String
    fun setAmSatCallsign(callsign: String)
    //endregion
}
