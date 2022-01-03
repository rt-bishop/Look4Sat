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
import android.hardware.GeomagneticField
import android.location.LocationManager
import androidx.core.content.edit
import com.rtbishop.look4sat.BuildConfig
import com.rtbishop.look4sat.data.PreferencesHandler
import com.rtbishop.look4sat.domain.QthConverter
import com.rtbishop.look4sat.domain.predict.GeoPos
import com.rtbishop.look4sat.presentation.round
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesSource @Inject constructor(
    private val locationManager: LocationManager,
    private val preferences: SharedPreferences
) : PreferencesHandler {

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
        val latitude = preferences.getString(keyLatitude, null) ?: defaultSP
        val longitude = preferences.getString(keyLongitude, null) ?: defaultSP
        return GeoPos(latitude.toDouble(), longitude.toDouble())
    }

    private fun saveStationPosition(pos: GeoPos) {
        preferences.edit {
            putString(keyLatitude, pos.latitude.toString())
            putString(keyLongitude, pos.longitude.toString())
        }
    }

    fun updatePosition(latitude: Double, longitude: Double) {
        val stationPosition = GeoPos(latitude, longitude)
        saveStationPosition(stationPosition)
    }

    fun updatePositionFromGPS(): Boolean {
        return try {
            val location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            if (location == null) false
            else {
                val latitude = location.latitude.round(4)
                val longitude = location.longitude.round(4)
                val stationPosition = GeoPos(latitude, longitude)
                saveStationPosition(stationPosition)
                return true
            }
        } catch (exception: SecurityException) {
            false
        }
    }

    fun updatePositionFromQTH(qthString: String): Boolean {
        val position = QthConverter.qthToPosition(qthString) ?: return false
        val stationPosition = GeoPos(position.latitude, position.longitude)
        saveStationPosition(stationPosition)
        return true
    }

    fun getMagDeclination(): Float {
        val stationPosition = loadStationPosition()
        val lat = stationPosition.latitude.toFloat()
        val lon = stationPosition.longitude.toFloat()
        return GeomagneticField(lat, lon, 0f, System.currentTimeMillis()).declination
    }

    fun getHoursAhead(): Int {
        return preferences.getInt(keyHoursAhead, 8)
    }

    fun getMinElevation(): Double {
        return preferences.getInt(keyMinElevation, 16).toDouble()
    }

    fun getUseUTC(): Boolean {
        return preferences.getBoolean(keyTimeUTC, false)
    }

    fun setUseUTC(value: Boolean) {
        preferences.edit { putBoolean(keyTimeUTC, value) }
    }

    fun getUseCompass(): Boolean {
        return preferences.getBoolean(keyCompass, true)
    }

    fun setUseCompass(value: Boolean) {
        preferences.edit { putBoolean(keyCompass, value) }
    }

    fun getShowSweep(): Boolean {
        return preferences.getBoolean(keyRadarSweep, true)
    }

    fun setShowSweep(value: Boolean) {
        preferences.edit { putBoolean(keyRadarSweep, value) }
    }

    fun getSetupDone(): Boolean {
        return preferences.getBoolean(keyInitialSetup, false)
    }

    fun setSetupDone() {
        preferences.edit { putBoolean(keyInitialSetup, true) }
    }

    fun saveModesSelection(modes: List<String>) {
        val modesSet = modes.toSet()
        preferences.edit {
            putStringSet(keyModes, modesSet)
        }
    }

    fun loadModesSelection(): List<String> {
        preferences.getStringSet(keyModes, setOf())?.let { modesSet ->
            return modesSet.toList().sorted()
        }
        return emptyList()
    }

    fun getRotatorEnabled(): Boolean {
        return preferences.getBoolean(keyRotator, false)
    }

    fun setRotatorEnabled(value: Boolean) {
        preferences.edit { putBoolean(keyRotator, value) }
    }

    fun getRotatorServer(): Pair<String, Int> {
        val address = preferences.getString(keyRotatorAddress, null) ?: "127.0.0.1"
        val port = preferences.getString(keyRotatorPort, null) ?: "4533"
        return Pair(address, port.toInt())
    }

    fun getRotatorIp(): String {
        return preferences.getString(keyRotatorAddress, null) ?: "127.0.0.1"
    }

    fun setRotatorIp(value: String) {
        preferences.edit { putString(keyRotatorAddress, value) }
    }

    fun getRotatorPort(): String {
        return preferences.getString(keyRotatorPort, null) ?: "4533"
    }

    fun setRotatorPort(value: String) {
        preferences.edit { putString(keyRotatorPort, value) }
    }

    override fun loadDataSources(): List<String> {
        val sourcesList = preferences.getStringSet(keyDataSources, null)?.toList()
        return if (sourcesList.isNullOrEmpty()) defaultSources else sourcesList.sortedDescending()
    }

    override fun saveDataSources(sources: List<String>) {
        if (sources.isNotEmpty()) preferences.edit { putStringSet(keyDataSources, sources.toSet()) }
    }
}
