package com.rtbishop.look4sat.data

import com.rtbishop.look4sat.domain.predict4kotlin.QthConverter
import com.rtbishop.look4sat.domain.predict4kotlin.StationPosition

class LocationRepo(
    private val locationSource: LocationSource,
    private val qthConverter: QthConverter
) {

    fun getStationPosition(): StationPosition {
        return locationSource.loadStationPosition()
    }

    fun getMagDeclination(): Float {
        return locationSource.getMagDeclination(getStationPosition())
    }

    fun positionToQTH(lat: Double, lon: Double): String? {
        return qthConverter.positionToQTH(lat, lon)
    }

    fun updatePositionFromGPS(): Boolean {
        val stationPosition = locationSource.getLastKnownLocation() ?: return false
        locationSource.saveStationPosition(stationPosition)
        return true
    }

    fun updatePositionFromQTH(qthString: String): Boolean {
        val position = qthConverter.qthToPosition(qthString) ?: return false
        val stationPosition = StationPosition(position.latitude, position.longitude, 0.0)
        locationSource.saveStationPosition(stationPosition)
        return true
    }
}
