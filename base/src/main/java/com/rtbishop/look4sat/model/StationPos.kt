package com.rtbishop.look4sat.model

data class StationPos(
    val latitude: Double, val longitude: Double, val qthLocator: String, val timestamp: Long
) {
    fun toGeoPos(): GeoPos = GeoPos(latitude, longitude)
}
