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
import com.rtbishop.look4sat.BuildConfig
import com.rtbishop.look4sat.data.SettingsHandler
import com.rtbishop.look4sat.domain.predict.GeoPos
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsProvider @Inject constructor(private val prefs: SharedPreferences) : SettingsHandler {

    private val keyInitialSetup = "${BuildConfig.VERSION_NAME}update"

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
        const val keyPositionGPS = "setPositionGPS"
        const val keyPositionQTH = "setPositionQTH"
    }

    fun loadStationPosition(): GeoPos {
        val defaultSP = "0.0"
        val latitude = prefs.getString(keyLatitude, null) ?: defaultSP
        val longitude = prefs.getString(keyLongitude, null) ?: defaultSP
        return GeoPos(latitude.toDouble(), longitude.toDouble())
    }

    fun saveStationPosition(latitude: Double, longitude: Double) {
        prefs.edit {
            putString(keyLatitude, latitude.toString())
            putString(keyLongitude, longitude.toString())
        }
    }

    fun getHoursAhead(): Int {
        return prefs.getInt(keyHoursAhead, 8)
    }

    fun getMinElevation(): Double {
        return prefs.getInt(keyMinElevation, 16).toDouble()
    }

    fun getUseUTC(): Boolean {
        return prefs.getBoolean(keyTimeUTC, false)
    }

    fun setUseUTC(value: Boolean) {
        prefs.edit { putBoolean(keyTimeUTC, value) }
    }

    fun getUseCompass(): Boolean {
        return prefs.getBoolean(keyCompass, true)
    }

    fun setUseCompass(value: Boolean) {
        prefs.edit { putBoolean(keyCompass, value) }
    }

    fun getShowSweep(): Boolean {
        return prefs.getBoolean(keyRadarSweep, true)
    }

    fun setShowSweep(value: Boolean) {
        prefs.edit { putBoolean(keyRadarSweep, value) }
    }

    fun getSetupDone(): Boolean {
        return prefs.getBoolean(keyInitialSetup, false)
    }

    fun setSetupDone() {
        prefs.edit { putBoolean(keyInitialSetup, true) }
    }

    fun saveModesSelection(modes: List<String>) {
        val modesSet = modes.toSet()
        prefs.edit {
            putStringSet(keyModes, modesSet)
        }
    }

    fun loadModesSelection(): List<String> {
        prefs.getStringSet(keyModes, setOf())?.let { modesSet ->
            return modesSet.toList().sorted()
        }
        return emptyList()
    }

    fun getRotatorEnabled(): Boolean {
        return prefs.getBoolean(keyRotator, false)
    }

    fun setRotatorEnabled(value: Boolean) {
        prefs.edit { putBoolean(keyRotator, value) }
    }

    fun getRotatorServer(): Pair<String, Int> {
        val address = prefs.getString(keyRotatorAddress, null) ?: "127.0.0.1"
        val port = prefs.getString(keyRotatorPort, null) ?: "4533"
        return Pair(address, port.toInt())
    }

    fun getRotatorIp(): String {
        return prefs.getString(keyRotatorAddress, null) ?: "127.0.0.1"
    }

    fun setRotatorIp(value: String) {
        prefs.edit { putString(keyRotatorAddress, value) }
    }

    fun getRotatorPort(): String {
        return prefs.getString(keyRotatorPort, null) ?: "4533"
    }

    fun setRotatorPort(value: String) {
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
