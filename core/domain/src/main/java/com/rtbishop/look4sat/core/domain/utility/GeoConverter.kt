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
package com.rtbishop.look4sat.core.domain.utility

import com.rtbishop.look4sat.core.domain.predict.DEG2RAD
import com.rtbishop.look4sat.core.domain.predict.RAD2DEG
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private const val AVG_EARTH_RADIUS_KM = 6371.009
private const val MIN_LATITUDE = -85.05112877980658
private const val MAX_LATITUDE = 85.05112877980658
private const val MIN_LONGITUDE = -180.0
private const val MAX_LONGITUDE = 180.0

fun Double.toDegrees(): Double = this * RAD2DEG

fun Double.toRadians(): Double = this * DEG2RAD

//fun Double.latToY01(): Double {
//    val sinus = sin(clipLat(this) * PI / MAX_LONGITUDE)
//    return 0.5 - ln((1 + sinus) / (1 - sinus)) / (4 * PI)
//}

//fun Double.lonToX01(): Double {
//    return (clipLon(this) - MIN_LONGITUDE) / (MAX_LONGITUDE - MIN_LONGITUDE)
//}

//fun Double.y01ToLat(): Double {
//    return 90 - 360 * atan(exp((this - 0.5) * 2 * PI)) / PI
//}

//fun Double.x01ToLon(): Double {
//    return MIN_LONGITUDE + (MAX_LONGITUDE - MIN_LONGITUDE) * this
//}

// Great-circle distance between two positions in kilometers using the spherical law of cosines.
fun greatCircleDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val lat1R = lat1.toRadians()
    val lat2R = lat2.toRadians()
    val lon1R = lon1.toRadians()
    val lon2R = lon2.toRadians()
    return acos(
        sin(lat1R) * sin(lat2R) + cos(lat1R) * cos(lat2R) * cos(lon2R - lon1R)
    ) * AVG_EARTH_RADIUS_KM
}

// Initial bearing (azimuth) from position 1 to position 2, in degrees (0-360).
fun bearingDeg(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val lat1R = lat1.toRadians()
    val lat2R = lat2.toRadians()
    val dLon = (lon2 - lon1).toRadians()
    val y = sin(dLon) * cos(lat2R)
    val x = cos(lat1R) * sin(lat2R) - sin(lat1R) * cos(lat2R) * cos(dLon)
    return (atan2(y, x).toDegrees() + 360) % 360
}

fun clipLat(latitude: Double): Double {
    return clip(latitude, MIN_LATITUDE, MAX_LATITUDE)
}

fun clipLon(longitude: Double): Double {
    var result = longitude
    while (result < MIN_LONGITUDE) result += 360.0
    while (result > MAX_LONGITUDE) result -= 360.0
    return clip(result, MIN_LONGITUDE, MAX_LONGITUDE)
}

private fun clip(currentValue: Double, minValue: Double, maxValue: Double): Double {
    return min(max(currentValue, minValue), maxValue)
}
