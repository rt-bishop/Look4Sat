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
package com.rtbishop.look4sat.domain

import com.rtbishop.look4sat.model.DatabaseState
import com.rtbishop.look4sat.model.GeoPos
import com.rtbishop.look4sat.model.OtherSettings
import com.rtbishop.look4sat.model.PassesSettings
import kotlinx.coroutines.flow.StateFlow

interface ISettingsRepo {

    val radioSourceUrl: String get() = "https://db.satnogs.org/api/transmitters/?format=json"
    val satelliteSourcesMap: Map<String, String>
        get() = mapOf(
            "All" to "https://celestrak.com/NORAD/elements/gp.php?GROUP=active&FORMAT=csv",
            "Amsat" to "https://amsat.org/tle/current/nasabare.txt",
            "Amateur" to "https://celestrak.com/NORAD/elements/gp.php?GROUP=amateur&FORMAT=csv",
            "Classified" to "https://www.prismnet.com/~mmccants/tles/classfd.zip",
            "Cubesat" to "https://celestrak.com/NORAD/elements/gp.php?GROUP=cubesat&FORMAT=csv",
            "Education" to "https://celestrak.com/NORAD/elements/gp.php?GROUP=education&FORMAT=csv",
            "Engineer" to "https://celestrak.com/NORAD/elements/gp.php?GROUP=engineering&FORMAT=csv",
            "Geostationary" to "https://celestrak.com/NORAD/elements/gp.php?GROUP=geo&FORMAT=csv",
            "Globalstar" to "https://celestrak.com/NORAD/elements/gp.php?GROUP=globalstar&FORMAT=csv",
            "GNSS" to "https://celestrak.com/NORAD/elements/gp.php?GROUP=gnss&FORMAT=csv",
            "Intelsat" to "https://celestrak.com/NORAD/elements/gp.php?GROUP=intelsat&FORMAT=csv",
            "Iridium" to "https://celestrak.com/NORAD/elements/gp.php?GROUP=iridium-NEXT&FORMAT=csv",
            "McCants" to "https://www.prismnet.com/~mmccants/tles/inttles.zip",
            "Military" to "https://celestrak.com/NORAD/elements/gp.php?GROUP=military&FORMAT=csv",
            "New" to "https://celestrak.com/NORAD/elements/gp.php?GROUP=last-30-days&FORMAT=csv",
            "OneWeb" to "https://celestrak.com/NORAD/elements/gp.php?GROUP=oneweb&FORMAT=csv",
            "Orbcomm" to "https://celestrak.com/NORAD/elements/gp.php?GROUP=orbcomm&FORMAT=csv",
            "R4UAB" to "https://r4uab.ru/satonline.txt",
            "Resource" to "https://celestrak.com/NORAD/elements/gp.php?GROUP=resource&FORMAT=csv",
            "SatNOGS" to "https://celestrak.com/NORAD/elements/gp.php?GROUP=satnogs&FORMAT=csv",
            "Science" to "https://celestrak.com/NORAD/elements/gp.php?GROUP=science&FORMAT=csv",
            "Spire" to "https://celestrak.com/NORAD/elements/gp.php?GROUP=spire&FORMAT=csv",
            "Starlink" to "https://celestrak.com/NORAD/elements/gp.php?GROUP=starlink&FORMAT=csv",
            "Swarm" to "https://celestrak.com/NORAD/elements/gp.php?GROUP=swarm&FORMAT=csv",
            "Weather" to "https://celestrak.com/NORAD/elements/gp.php?GROUP=weather&FORMAT=csv",
            "X-Comm" to "https://celestrak.com/NORAD/elements/gp.php?GROUP=x-comm&FORMAT=csv"
        )

    //region # Station position settings

    val stationPosition: StateFlow<GeoPos>

    fun setGpsPosition(): Boolean

    fun setGeoPosition(latitude: Double, longitude: Double, altitude: Double = 0.0): Boolean

    fun setQthPosition(locator: String): Boolean

    //endregion

    //region # Database update settings

    val databaseState: StateFlow<DatabaseState>

    fun saveDatabaseState(state: DatabaseState)

    fun saveSatType(type: String, catnums: List<Int>)

    fun loadSatType(type: String): List<Int>

    //endregion

    //region # Entries selection settings

    val satelliteSelection: StateFlow<List<Int>>

    fun saveEntriesSelection(catnums: List<Int>)

    //endregion

    //region # Passes filter settings

    val passesSettings: StateFlow<PassesSettings>

    fun savePassesSettings(settings: PassesSettings)

    fun saveModesSelection(modes: List<String>)

    fun loadModesSelection(): List<String>

    //endregion

    //region # Other settings

    val otherSettings: StateFlow<OtherSettings>

    fun toggleUtc(value: Boolean)

    fun toggleUpdate(value: Boolean)

    fun toggleSweep(value: Boolean)

    fun toggleSensor(value: Boolean)

    //endregion

    //region # Undefined settings

    fun getRotatorEnabled(): Boolean

    fun setRotatorEnabled(value: Boolean)

    fun getRotatorServer(): String

    fun setRotatorServer(value: String)

    fun getRotatorPort(): String

    fun setRotatorPort(value: String)

    fun getBTEnabled(): Boolean

    fun setBTEnabled(value: Boolean)

    fun getBTDeviceAddr(): String

    fun setBTDeviceAddr(value: String)

    fun getBTDeviceName(): String

    fun setBTDeviceName(value: String)

    fun getBTFormat(): String

    fun setBTFormat(value: String)

    //endregion
}
