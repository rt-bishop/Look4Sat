/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2026 Arty Bishop and contributors.
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
package com.rtbishop.look4sat.core.domain.predict

import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class OrbitalPos(
    var azimuth: Double = 0.0,
    var elevation: Double = 0.0,
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var altitude: Double = 0.0,
    var distance: Double = 0.0,
    var distanceRate: Double = 0.0,
    var theta: Double = 0.0,
    var time: Long = 0L,
    var phase: Double = 0.0,
    var eclipseDepth: Double = 0.0,
    var eclipsed: Boolean = false,
    var aboveHorizon: Boolean = false
) {

    fun getDownlinkFreq(freq: Long): Long {
        return (freq.toDouble() * (SPEED_OF_LIGHT - distanceRate * 1000.0) / SPEED_OF_LIGHT).toLong()
    }

    fun getUplinkFreq(freq: Long): Long {
        return (freq.toDouble() * (SPEED_OF_LIGHT + distanceRate * 1000.0) / SPEED_OF_LIGHT).toLong()
    }

    fun getOrbitalVelocity(): Double {
        val radius = EARTH_RADIUS_M + altitude * 1000.0
        return sqrt(GM_EARTH / radius) / 1000.0
    }

    fun getRangeCircle(): List<GeoPos> {
        val pointCount = 721
        val rangeCirclePoints = ArrayList<GeoPos>(pointCount)
        val beta = acos(EARTH_RADIUS / (EARTH_RADIUS + altitude))
        val sinLat = sin(latitude)
        val cosLat = cos(latitude)
        val cosBeta = cos(beta)
        val sinBeta = sin(beta)
        for (azimuth in 0..720) {
            val rads = azimuth * DEG2RAD
            val sinRads = sin(rads)
            val cosRads = cos(rads)
            val lat = asin(sinLat * cosBeta + cosLat * sinBeta * cosRads)
            val lon = longitude + atan2(sinRads * sinBeta * cosLat, cosBeta - sinLat * sin(lat))
            rangeCirclePoints.add(GeoPos(lat * RAD2DEG, lon * RAD2DEG))
        }
        return rangeCirclePoints
    }

    companion object {
        // Pre-computed constants for orbital velocity calculation
        private const val GM_EARTH = 3.986004418E14 // m^3/s^2
        private const val EARTH_RADIUS_M = 6.37E6 // meters
    }
}
