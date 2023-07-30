package com.rtbishop.look4sat.domain.utility

import com.rtbishop.look4sat.domain.predict.DEG2RAD
import com.rtbishop.look4sat.domain.predict.PI
import com.rtbishop.look4sat.domain.predict.RAD2DEG
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private const val MIN_LATITUDE = -85.05112877980658
private const val MAX_LATITUDE = 85.05112877980658
private const val MIN_LONGITUDE = -180.0
private const val MAX_LONGITUDE = 180.0

fun Double.toDegrees(): Double = this * RAD2DEG

fun Double.toRadians(): Double = this * DEG2RAD

fun Double.latToY01(): Double {
    val sinus = sin(clipLat(this) * PI / MAX_LONGITUDE)
    return 0.5 - ln((1 + sinus) / (1 - sinus)) / (4 * PI)
}

fun Double.lonToX01(): Double {
    return (clipLon(this) - MIN_LONGITUDE) / (MAX_LONGITUDE - MIN_LONGITUDE)
}

//fun Double.y01ToLat(): Double {
//    return 90 - 360 * atan(exp((this - 0.5) * 2 * PI)) / PI
//}

//fun Double.x01ToLon(): Double {
//    return MIN_LONGITUDE + (MAX_LONGITUDE - MIN_LONGITUDE) * this
//}

private fun clipLat(latitude: Double): Double {
    return clip(latitude, MIN_LATITUDE, MAX_LATITUDE)
}

private fun clipLon(longitude: Double): Double {
    var result = longitude
    while (result < MIN_LONGITUDE) result += 360.0
    while (result > MAX_LONGITUDE) result -= 360.0
    return clip(result, MIN_LONGITUDE, MAX_LONGITUDE)
}

private fun clip(currentValue: Double, minValue: Double, maxValue: Double): Double {
    return min(max(currentValue, minValue), maxValue)
}
