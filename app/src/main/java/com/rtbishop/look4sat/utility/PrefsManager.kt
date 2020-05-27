/*
 * Look4Sat. Amateur radio and weather satellite tracker and passes predictor for Android.
 * Copyright (C) 2019, 2020 Arty Bishop (bishop.arty@gmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.rtbishop.look4sat.utility

import android.content.SharedPreferences
import android.location.LocationManager
import androidx.core.content.edit
import com.github.amsacode.predict4java.GroundStationPosition
import javax.inject.Inject

class PrefsManager @Inject constructor(
    private val preferences: SharedPreferences,
    val locationManager: LocationManager
) {
    private val keyHoursAhead = "hoursAhead"
    private val keyMinElevation = "minEl"
    private val keyLatitude = "latitude"
    private val keyLongitude = "longitude"
    private val keyAltitude = "altitude"
    private val keyRefreshRate = "rate"
    private val keyCompass = "compass"
    private val keyTleSources = "tleSources"
    private val defaultHoursAhead = 8
    private val defaultMinEl = 16.0
    private val defaultRefreshRate = "3000"
    private val defaultGSP = "0.0"
    private val defaultTleUrl = "https://celestrak.com/NORAD/elements/active.txt"

    fun getHoursAhead(): Int {
        return preferences.getInt(keyHoursAhead, defaultHoursAhead)
    }

    fun getMinElevation(): Double {
        return preferences.getDouble(keyMinElevation, defaultMinEl)
    }

    fun getRefreshRate(): Long {
        return preferences.getString(keyRefreshRate, defaultRefreshRate)!!.toLong()
    }

    fun getCompass(): Boolean {
        return preferences.getBoolean(keyCompass, true)
    }

    fun getPosition(): GroundStationPosition {
        val lat = preferences.getString(keyLatitude, defaultGSP)!!.toDouble()
        val lon = preferences.getString(keyLongitude, defaultGSP)!!.toDouble()
        val alt = preferences.getString(keyAltitude, defaultGSP)!!.toDouble()
        return GroundStationPosition(lat, lon, alt)
    }

    fun setHoursAhead(hours: Int) {
        preferences.edit {
            putInt(keyHoursAhead, hours)
            apply()
        }
    }

    fun setMinElevation(minEl: Double) {
        preferences.edit {
            putDouble(keyMinElevation, minEl)
            apply()
        }
    }

    fun setPosition(gsp: GroundStationPosition) {
        preferences.edit {
            putString(keyLatitude, gsp.latitude.toString())
            putString(keyLongitude, gsp.longitude.toString())
            putString(keyAltitude, gsp.heightAMSL.toString())
            apply()
        }
    }

    fun getTleSources(): Set<String> {
        return preferences.getStringSet(keyTleSources, null) ?: setOf(defaultTleUrl)
    }

    fun setTleSources(sources: Set<String>) {
        preferences.edit {
            putStringSet(keyTleSources, sources)
            apply()
        }
    }

    private fun SharedPreferences.Editor.putDouble(key: String, double: Double) {
        putLong(key, double.toRawBits())
    }

    private fun SharedPreferences.getDouble(key: String, default: Double): Double {
        return Double.fromBits(getLong(key, default.toRawBits()))
    }
}
