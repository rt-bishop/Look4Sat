/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2022 Arty Bishop (bishop.arty@gmail.com)
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
import com.rtbishop.look4sat.domain.predict.GeoPos
import kotlinx.coroutines.flow.StateFlow

interface ISettingsRepo {

    val radioSourceUrl: String get() = "https://db.satnogs.org/api/transmitters/?format=json"
    val satelliteSourcesMap: Map<String, String>
        get() = mapOf(
            "All" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=active&FORMAT=csv",
            "Amsat" to "https://amsat.org/tle/current/nasabare.txt",
            "Amateur" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=amateur&FORMAT=csv",
            "Classified" to "https://www.mmccants.org/tles/classfd.zip",
            "Cubesat" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=cubesat&FORMAT=csv",
            "Education" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=education&FORMAT=csv",
            "Engineer" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=engineering&FORMAT=csv",
            "Geostationary" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=geo&FORMAT=csv",
            "Globalstar" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=globalstar&FORMAT=csv",
            "GNSS" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=gnss&FORMAT=csv",
            "Intelsat" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=intelsat&FORMAT=csv",
            "Iridium" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=iridium-NEXT&FORMAT=csv",
            "McCants" to "https://www.mmccants.org/tles/inttles.zip",
            "Military" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=military&FORMAT=csv",
            "New" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=last-30-days&FORMAT=csv",
            "OneWeb" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=oneweb&FORMAT=csv",
            "Orbcomm" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=orbcomm&FORMAT=csv",
            "R4UAB" to "https://r4uab.ru/satonline.txt",
            "Resource" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=resource&FORMAT=csv",
            "SatNOGS" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=satnogs&FORMAT=csv",
            "Science" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=science&FORMAT=csv",
            "Spire" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=spire&FORMAT=csv",
            "Starlink" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=starlink&FORMAT=csv",
            "Swarm" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=swarm&FORMAT=csv",
            "Weather" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=weather&FORMAT=csv",
            "X-Comm" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=x-comm&FORMAT=csv"
        )

    //region # Satellites selection settings
    val selectedIds: StateFlow<List<Int>>
    fun setSelectedIds(ids: List<Int>)
    //endregion

    //region # Passes filter settings
    val passesSettings: StateFlow<PassesSettings>
    fun setPassesSettings(settings: PassesSettings)
    //endregion

    //region # Station position settings
    val stationPosition: StateFlow<GeoPos>
    fun setStationPositionGeo(latitude: Double, longitude: Double, altitude: Double): Boolean
    fun setStationPositionGps(): Boolean
    fun setStationPositionQth(locator: String): Boolean
    //endregion

    //region # Database update settings
    val databaseState: StateFlow<DatabaseState>
    fun getSatelliteTypeIds(type: String): List<Int>
    fun setSatelliteTypeIds(type: String, ids: List<Int>)
    fun updateDatabaseState(state: DatabaseState)
    //endregion

    //region # Other settings
    val otherSettings: StateFlow<OtherSettings>
    fun setStateOfAutoUpdate(value: Boolean)
    fun setStateOfSensors(value: Boolean)
    fun setStateOfSweep(value: Boolean)
    fun setStateOfUtc(value: Boolean)
    //endregion

    //region # Undefined settings
    fun getBluetoothAddress(): String
    fun setBluetoothAddress(value: String)
    fun getBluetoothFormat(): String
    fun setBluetoothFormat(value: String)
    fun getBluetoothName(): String
    fun setBluetoothName(value: String)
    fun getBluetoothState(): Boolean
    fun setBluetoothState(value: Boolean)
    fun getRotatorAddress(): String
    fun setRotatorAddress(value: String)
    fun getRotatorPort(): String
    fun setRotatorPort(value: String)
    fun getRotatorState(): Boolean
    fun setRotatorState(value: Boolean)
    //endregion
}
