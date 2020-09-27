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
import com.rtbishop.look4sat.data.PassPrefs
import com.rtbishop.look4sat.utility.Utilities.getDouble
import com.rtbishop.look4sat.utility.Utilities.putDouble
import com.rtbishop.look4sat.utility.Utilities.round
import javax.inject.Inject

class PrefsManager @Inject constructor(
    private val preferences: SharedPreferences,
    private val locationManager: LocationManager
) {
    companion object {
        const val keyHoursAhead = "hoursAhead"
        const val keyMinElevation = "minEl"
        const val keyLatitude = "latitude"
        const val keyLongitude = "longitude"
        const val keyAltitude = "altitude"
        const val keyCompass = "compass"
        const val keyTimeUtc = "timeUTC"
        const val defaultHoursAhead = 8
        const val defaultMinEl = 16.0
        const val defaultGSP = "0.0"
        const val keyIsFirstLaunch = "keyIsFirstLaunch"
    }

    fun getPassPrefs(): PassPrefs {
        val hoursAhead = preferences.getInt(keyHoursAhead, defaultHoursAhead)
        val minEl = preferences.getDouble(keyMinElevation, defaultMinEl)
        return PassPrefs(hoursAhead, minEl)
    }

    fun setPassPrefs(hoursAhead: Int, minEl: Double) {
        preferences.edit {
            putInt(keyHoursAhead, hoursAhead)
            putDouble(keyMinElevation, minEl)
            apply()
        }
    }

    fun getStationPosition(): GroundStationPosition {
        val lat = preferences.getString(keyLatitude, defaultGSP)!!.toDouble()
        val lon = preferences.getString(keyLongitude, defaultGSP)!!.toDouble()
        val alt = preferences.getString(keyAltitude, defaultGSP)!!.toDouble()
        return GroundStationPosition(lat, lon, alt)
    }

    fun updateStationPosition() {
        try {
            locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)?.let {
                val lat = it.latitude.round(4)
                val lon = it.longitude.round(4)
                val alt = it.altitude.round(1)
                preferences.edit {
                    putString(keyLatitude, lat.toString())
                    putString(keyLongitude, lon.toString())
                    putString(keyAltitude, alt.toString())
                    apply()
                }
            }
        } catch (e: SecurityException) {
        }
    }

    fun shouldUseUTC(): Boolean {
        return preferences.getBoolean(keyTimeUtc, false)
    }

    fun shouldUseCompass(): Boolean {
        return preferences.getBoolean(keyCompass, true)
    }

    fun isFirstLaunch(): Boolean {
        val isFirstLaunch = preferences.getBoolean(keyIsFirstLaunch, true)
        return if (isFirstLaunch) {
            preferences.edit {
                putBoolean(keyIsFirstLaunch, false)
                apply()
            }
            isFirstLaunch
        } else isFirstLaunch
    }
}
