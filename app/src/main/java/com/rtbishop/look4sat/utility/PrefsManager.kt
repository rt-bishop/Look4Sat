/*******************************************************************************
 Look4Sat. Amateur radio satellite tracker and pass predictor.
 Copyright (C) 2019, 2020 Arty Bishop (bishop.arty@gmail.com)

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along
 with this program; if not, write to the Free Software Foundation, Inc.,
 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 ******************************************************************************/

package com.rtbishop.look4sat.utility

import android.content.SharedPreferences
import android.hardware.GeomagneticField
import androidx.core.content.edit
import com.github.amsacode.predict4java.GroundStationPosition
import javax.inject.Inject

class PrefsManager @Inject constructor(val preferences: SharedPreferences) {
    companion object {
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
            apply()
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
        preferences.edit {
            putBoolean(keyIsFirstLaunch, false)
            apply()
        }
    }
}
