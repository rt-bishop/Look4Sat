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
package com.rtbishop.look4sat.domain.predict

import kotlin.math.*

data class SatPos(
    var azimuth: Double = 0.0,
    var elevation: Double = 0.0,
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var altitude: Double = 0.0,
    var distance: Double = 0.0,
    var distanceRate: Double = 0.0,
    var theta: Double = 0.0,
    var time: Long = 0L
) {
    private val earthRadiusKm = 6378.16
    private val speedOfLight = 2.99792458E8

    fun getDownlinkFreq(freq: Long): Long {
        return (freq.toDouble() * (speedOfLight - distanceRate * 1000.0) / speedOfLight).toLong()
    }

    fun getUplinkFreq(freq: Long): Long {
        return (freq.toDouble() * (speedOfLight + distanceRate * 1000.0) / speedOfLight).toLong()
    }

    fun getOrbitalVelocity(): Double {
        val earthG = 6.674 * 10.0.pow(-11)
        val earthM = 5.98 * 10.0.pow(24)
        val radius = 6.37 * 10.0.pow(6) + altitude * 10.0.pow(3)
        return sqrt(earthG * earthM / radius) / 1000
    }

    fun getRangeCircle(): List<GeoPos> {
        val positions = mutableListOf<GeoPos>()
        val beta = acos(earthRadiusKm / (earthRadiusKm + altitude))
        var tempAzimuth = 0
        while (tempAzimuth < 360) {
            val azimuth = tempAzimuth / 360.0 * 2.0 * Math.PI
            var lat = asin(sin(latitude) * cos(beta) + cos(azimuth) * sin(beta) * cos(latitude))
            val num = (cos(beta) - (sin(latitude) * sin(lat)))
            val den = cos(latitude) * cos(lat)
            var lon = if (tempAzimuth == 0 && (beta > ((Math.PI / 2.0) - latitude))) {
                longitude + Math.PI
            } else if (tempAzimuth == 180 && (beta > ((Math.PI / 2.0) - latitude))) {
                longitude + Math.PI
            } else if (abs(num / den) > 1.0) {
                longitude
            } else {
                if ((180 - tempAzimuth) >= 0) {
                    longitude - acos(num / den)
                } else {
                    longitude + acos(num / den)
                }
            }
            while (lon < 0.0) lon += Math.PI * 2.0
            while (lon > Math.PI * 2.0) lon -= Math.PI * 2.0
            lat = Math.toDegrees(lat)
            lon = Math.toDegrees(lon)
            positions.add(GeoPos(lat, lon))
            tempAzimuth += 1
        }
        return positions
    }

    fun getRangeCircleRadiusKm(): Double {
        return earthRadiusKm * acos(earthRadiusKm / (earthRadiusKm + altitude))
    }
}
