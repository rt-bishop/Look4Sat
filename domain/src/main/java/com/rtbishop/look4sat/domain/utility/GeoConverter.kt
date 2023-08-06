/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2022 Arty Bishop (bishop.arty@gmail.com)
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
package com.rtbishop.look4sat.domain.utility

import com.rtbishop.look4sat.domain.predict.DEG2RAD
import com.rtbishop.look4sat.domain.predict.RAD2DEG
import kotlin.math.max
import kotlin.math.min

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
