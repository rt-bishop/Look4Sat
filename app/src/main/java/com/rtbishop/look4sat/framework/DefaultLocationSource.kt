package com.rtbishop.look4sat.framework

import android.content.SharedPreferences
import android.hardware.GeomagneticField
import android.location.LocationManager
import androidx.core.content.edit
import com.rtbishop.look4sat.data.LocationSource
import com.rtbishop.look4sat.domain.predict4kotlin.StationPosition
import com.rtbishop.look4sat.utility.round
import javax.inject.Inject

class DefaultLocationSource @Inject constructor(
    private val locationManager: LocationManager,
    private val preferences: SharedPreferences
) : LocationSource {

    companion object {
        const val keyLatitude = "stationLat"
        const val keyLongitude = "stationLon"
        const val keyAltitude = "stationAlt"
        const val keyPositionGPS = "setPositionGPS"
        const val keyPositionQTH = "setPositionQTH"
    }

    override fun getLastKnownLocation(): StationPosition? {
        return try {
            val location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            if (location == null) null
            else {
                val latitude = location.latitude.round(4)
                val longitude = location.longitude.round(4)
                val altitude = location.altitude.round(1)
                return StationPosition(latitude, longitude, altitude)
            }
        } catch (exception: SecurityException) {
            null
        }
    }

    override fun getMagDeclination(position: StationPosition): Float {
        val lat = position.latitude.toFloat()
        val lon = position.longitude.toFloat()
        val alt = position.altitude.toFloat()
        return GeomagneticField(lat, lon, alt, System.currentTimeMillis()).declination
    }

    override fun loadStationPosition(): StationPosition {
        val defaultSP = "0.0"
        val latitude = preferences.getString(keyLatitude, null) ?: defaultSP
        val longitude = preferences.getString(keyLongitude, null) ?: defaultSP
        val altitude = preferences.getString(keyAltitude, null) ?: defaultSP
        return StationPosition(latitude.toDouble(), longitude.toDouble(), altitude.toDouble())
    }

    override fun saveStationPosition(position: StationPosition) {
        preferences.edit {
            putString(keyLatitude, position.latitude.toString())
            putString(keyLongitude, position.longitude.toString())
            putString(keyAltitude, position.altitude.toString())
        }
    }
}
