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
package com.rtbishop.look4sat.repository

import android.content.SharedPreferences
import android.hardware.GeomagneticField
import androidx.core.content.edit
import com.github.amsacode.predict4java.GroundStationPosition
import com.rtbishop.look4sat.data.TleSource
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import javax.inject.Inject

class PrefsRepo @Inject constructor(val preferences: SharedPreferences, val moshi: Moshi) {
    
    private val sourcesType = Types.newParameterizedType(List::class.java, TleSource::class.java)
    private val sourcesAdapter = moshi.adapter<List<TleSource>>(sourcesType)
    
    companion object {
        const val keySources = "tleSources"
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
    }
    
    fun getHoursAhead(): Int {
        return preferences.getInt(keyHoursAhead, 8)
    }
    
    fun getMinElevation(): Double {
        return preferences.getInt(keyMinElevation, 16).toDouble()
    }
    
    fun getStationPosition(): GroundStationPosition {
        val defaultGSP = "0.0"
        val lat = preferences.getString(keyLatitude, defaultGSP)!!.toDouble()
        val lon = preferences.getString(keyLongitude, defaultGSP)!!.toDouble()
        val alt = preferences.getString(keyAltitude, defaultGSP)!!.toDouble()
        return GroundStationPosition(lat, lon, alt)
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
        val lat = stationPosition.latitude.toFloat()
        val lon = stationPosition.longitude.toFloat()
        val alt = stationPosition.heightAMSL.toFloat()
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
        preferences.getString(keySources, null)?.let { sourcesJson ->
            sourcesAdapter.fromJson(sourcesJson)?.let { loadedSources ->
                return if (loadedSources.isNotEmpty()) {
                    loadedSources
                } else {
                    loadDefaultSources()
                }
            }
        }
        return loadDefaultSources()
    }
    
    fun saveTleSources(sources: List<TleSource>) {
        val sourcesJson = sourcesAdapter.toJson(sources)
        preferences.edit { putString(keySources, sourcesJson) }
    }
    
    private fun loadDefaultSources(): List<TleSource> {
        return listOf(
            TleSource("https://celestrak.com/NORAD/elements/active.txt"),
            TleSource("https://amsat.org/tle/current/nasabare.txt"),
            TleSource("https://www.prismnet.com/~mmccants/tles/classfd.zip"),
            TleSource("https://www.prismnet.com/~mmccants/tles/inttles.zip")
        )
    }
}
