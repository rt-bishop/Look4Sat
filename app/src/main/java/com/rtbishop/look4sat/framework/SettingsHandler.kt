/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2021 Arty Bishop (bishop.arty@gmail.com)
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
package com.rtbishop.look4sat.framework

import android.content.SharedPreferences
import androidx.core.content.edit
import com.rtbishop.look4sat.data.ISettingsHandler
import com.rtbishop.look4sat.domain.predict.GeoPos
import com.rtbishop.look4sat.presentation.getDouble
import com.rtbishop.look4sat.presentation.putDouble
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsHandler @Inject constructor(private val prefs: SharedPreferences) : ISettingsHandler {

    companion object {
        const val keyDataSources = "dataSources"
        const val keyModes = "satModes"
        const val keyCompass = "compass"
        const val keyRadarSweep = "radarSweep"
        const val keyTimeUTC = "timeUTC"
        const val keyHoursAhead = "hoursAhead"
        const val keyMinElevation = "minElevation"
        const val keyRotator = "isRotatorEnabled"
        const val keyRotatorAddress = "rotatorAddress"
        const val keyRotatorPort = "rotatorPort"
        const val keyLatitude = "stationLat"
        const val keyLongitude = "stationLon"
//        const val keyPositionGPS = "setPositionGPS"
//        const val keyPositionQTH = "setPositionQTH"
        const val keySelection = "selection"
    }

    override fun loadStationPosition(): GeoPos {
        val defaultSP = "0.0"
        val latitude = prefs.getString(keyLatitude, null) ?: defaultSP
        val longitude = prefs.getString(keyLongitude, null) ?: defaultSP
        return GeoPos(latitude.toDouble(), longitude.toDouble())
    }

    override fun saveStationPosition(latitude: Double, longitude: Double) {
        prefs.edit {
            putString(keyLatitude, latitude.toString())
            putString(keyLongitude, longitude.toString())
        }
    }

    override fun saveEntriesSelection(catnums: List<Int>) {
        val stringList = catnums.map { catnum -> catnum.toString() }
        prefs.edit { putStringSet(keySelection, stringList.toSet()) }
    }

    override fun loadEntriesSelection(): List<Int> {
        val catnums = prefs.getStringSet(keySelection, emptySet())?.map { catnum -> catnum.toInt() }
        return catnums?.sorted() ?: emptyList()
    }

    override fun getHoursAhead(): Int {
        return prefs.getInt(keyHoursAhead, 8)
    }

    override fun setHoursAhead(hoursAhead: Int) {
        prefs.edit { putInt(keyHoursAhead, hoursAhead) }
    }

    override fun getMinElevation(): Double {
        return prefs.getDouble(keyMinElevation, 16.0)
    }

    override fun setMinElevation(minElevation: Double) {
        prefs.edit { putDouble(keyMinElevation, minElevation) }
    }

    override fun getUseUTC(): Boolean {
        return prefs.getBoolean(keyTimeUTC, false)
    }

    override fun setUseUTC(value: Boolean) {
        prefs.edit { putBoolean(keyTimeUTC, value) }
    }

    override fun getUseCompass(): Boolean {
        return prefs.getBoolean(keyCompass, true)
    }

    override fun setUseCompass(value: Boolean) {
        prefs.edit { putBoolean(keyCompass, value) }
    }

    override fun getShowSweep(): Boolean {
        return prefs.getBoolean(keyRadarSweep, true)
    }

    override fun setShowSweep(value: Boolean) {
        prefs.edit { putBoolean(keyRadarSweep, value) }
    }

    override fun saveModesSelection(modes: List<String>) {
        prefs.edit { putStringSet(keyModes, modes.toSet()) }
    }

    override fun loadModesSelection(): List<String> {
        return prefs.getStringSet(keyModes, null)?.toList()?.sorted() ?: emptyList()
    }

    override fun getRotatorEnabled(): Boolean {
        return prefs.getBoolean(keyRotator, false)
    }

    override fun setRotatorEnabled(value: Boolean) {
        prefs.edit { putBoolean(keyRotator, value) }
    }

    override fun getRotatorServer(): String {
        return prefs.getString(keyRotatorAddress, null) ?: "127.0.0.1"
    }

    override fun setRotatorServer(value: String) {
        prefs.edit { putString(keyRotatorAddress, value) }
    }

    override fun getRotatorPort(): String {
        return prefs.getString(keyRotatorPort, null) ?: "4533"
    }

    override fun setRotatorPort(value: String) {
        prefs.edit { putString(keyRotatorPort, value) }
    }

    override fun loadDataSources(): List<String> {
        val sourcesList = prefs.getStringSet(keyDataSources, null)?.toList()
        return if (sourcesList.isNullOrEmpty()) defaultSources else sourcesList.sortedDescending()
    }

    override fun saveDataSources(sources: List<String>) {
        if (sources.isNotEmpty()) prefs.edit { putStringSet(keyDataSources, sources.toSet()) }
    }
}
