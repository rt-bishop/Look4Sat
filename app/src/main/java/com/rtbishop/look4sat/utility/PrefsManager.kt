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
package com.rtbishop.look4sat.utility

import android.content.SharedPreferences
import android.hardware.GeomagneticField
import androidx.core.content.edit
import com.rtbishop.look4sat.framework.model.TleSource
import com.rtbishop.look4sat.domain.predict4kotlin.GroundPos
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrefsManager @Inject constructor(val preferences: SharedPreferences, moshi: Moshi) {
    
    private val sourcesType = Types.newParameterizedType(List::class.java, TleSource::class.java)
    private val sourcesAdapter = moshi.adapter<List<TleSource>>(sourcesType)
    
    companion object {
        const val keySources = "sourcesListJson"
        const val keyModes = "satModes"
        const val keyLatitude = "latitude"
        const val keyLongitude = "longitude"
        const val keyAltitude = "altitude"
        const val keyCompass = "compass"
        const val keyTextLabels = "shouldUseTextLabels"
        const val keyTimeUTC = "timeUTC"
        const val keyHoursAhead = "hoursAhead"
        const val keyMinElevation = "minElevation"
        const val keyPositionGPS = "setPositionGPS"
        const val keyPositionQTH = "setPositionQTH"
        const val keyIsFirstLaunch = "shouldShowSplash"
        const val keyRotator = "isRotatorEnabled"
        const val keyRotatorAddress = "rotatorAddress"
        const val keyRotatorPort = "rotatorPort"
    }
    
    fun getHoursAhead(): Int {
        return preferences.getInt(keyHoursAhead, 8)
    }
    
    fun getMinElevation(): Double {
        return preferences.getInt(keyMinElevation, 16).toDouble()
    }
    
    fun getStationPosition(): GroundPos {
        val defaultGSP = "0.0"
        val latitude = preferences.getString(keyLatitude, null) ?: defaultGSP
        val longitude = preferences.getString(keyLongitude, null) ?: defaultGSP
        val altitude = preferences.getString(keyAltitude, null) ?: defaultGSP
        return GroundPos(latitude.toDouble(), longitude.toDouble(), altitude.toDouble())
    }

    fun setStationPosition(latitude: Double, longitude: Double, altitude: Double) {
        preferences.edit {
            putString(keyLatitude, latitude.toString())
            putString(keyLongitude, longitude.toString())
            putString(keyAltitude, altitude.toString())
        }
    }

    fun getMagDeclination(): Float {
        val stationPosition = getStationPosition()
        val lat = stationPosition.lat.toFloat()
        val lon = stationPosition.lon.toFloat()
        val alt = stationPosition.alt.toFloat()
        return GeomagneticField(lat, lon, alt, System.currentTimeMillis()).declination
    }

    fun shouldUseTextLabels(): Boolean {
        return preferences.getBoolean(keyTextLabels, false)
    }

    fun shouldUseUTC(): Boolean {
        return preferences.getBoolean(keyTimeUTC, false)
    }

    fun shouldUseCompass(): Boolean {
        return preferences.getBoolean(keyCompass, true)
    }
    
    fun isFirstLaunch(): Boolean {
        return preferences.getBoolean(keyIsFirstLaunch, true)
    }
    
    fun setFirstLaunchDone() {
        preferences.edit { putBoolean(keyIsFirstLaunch, false) }
    }
    
    fun loadTleSources(): List<TleSource> {
        return try {
            val sourcesString = preferences.getString(keySources, String())
            if (sourcesString.isNullOrEmpty()) {
                loadDefaultSources()
            } else {
                sourcesAdapter.fromJson(sourcesString) ?: loadDefaultSources()
            }
        } catch (exception: ClassCastException) {
            loadDefaultSources()
        }
    }
    
    fun saveTleSources(sources: List<TleSource>) {
        val sourcesJson = sourcesAdapter.toJson(sources)
        preferences.edit { putString(keySources, sourcesJson) }
    }

    fun loadDefaultSources(): List<TleSource> {
        return listOf(
            TleSource("https://celestrak.com/NORAD/elements/active.txt"),
            TleSource("https://amsat.org/tle/current/nasabare.txt"),
            TleSource("https://www.prismnet.com/~mmccants/tles/classfd.zip"),
            TleSource("https://www.prismnet.com/~mmccants/tles/inttles.zip")
        )
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

    fun getRotatorServer(): Pair<String, Int>? {
        return if (preferences.getBoolean(keyRotator, false)) {
            val address = preferences.getString(keyRotatorAddress, null) ?: "127.0.0.1"
            val port = preferences.getString(keyRotatorPort, null) ?: "4096"
            Pair(address, port.toInt())
        } else null
    }
}
