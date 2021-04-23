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
package com.rtbishop.look4sat.domain.predict4kotlin

import java.util.*
import kotlin.math.*

class SatPos {

    private val earthRadiusKm = 6378.16

    // Radians
    var azimuth = 0.0
    var elevation = 0.0
    var latitude = 0.0
    var longitude = 0.0
    var altitude = 0.0
    var range = 0.0
    var rangeRate = 0.0
    var theta = 0.0
    var time = Date()

    fun getRangeCircleRadiusKm(): Double {
        return earthRadiusKm * acos(earthRadiusKm / (earthRadiusKm + altitude))
    }

    fun getRangeCircle(incrementDegrees: Double = 1.0): List<Position> {
        val positions = mutableListOf<Position>()
        val lat = this.latitude
        val lon = this.longitude
        val beta = getRangeCircleRadiusKm() / earthRadiusKm
        var tempAzimuth = 0
        while (tempAzimuth < 360) {
            val azimuth = tempAzimuth / 360.0 * 2.0 * Math.PI
            var rangelat = asin(sin(lat) * cos(beta) + cos(azimuth) * sin(beta) * cos(lat))
            val num = (cos(beta) - (sin(lat) * sin(rangelat)))
            val den = cos(lat) * cos(rangelat)
            var rangelon = if (tempAzimuth == 0 && (beta > ((Math.PI / 2.0) - lat))) {
                lon + Math.PI
            } else if (tempAzimuth == 180 && (beta > ((Math.PI / 2.0) - lat))) {
                lon + Math.PI
            } else if (abs(num / den) > 1.0) {
                lon
            } else {
                if ((180 - tempAzimuth) >= 0) {
                    lon - acos(num / den)
                } else {
                    lon + acos(num / den)
                }
            }
            while (rangelon < 0.0) rangelon += Math.PI * 2.0
            while (rangelon > Math.PI * 2.0) rangelon -= Math.PI * 2.0
            rangelat = Math.toDegrees(rangelat)
            rangelon = Math.toDegrees(rangelon)
            positions.add(Position(rangelat, rangelon))
            tempAzimuth += incrementDegrees.toInt()
        }
        return positions
    }
}
