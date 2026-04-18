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

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

// ── Shared orbital math utilities ──
// Used by both CelestialComputer (sun/moon/celestial) and OrbitalObject (SGP4/SDP4).
// Package-internal — not part of the public API.

/**
 * Greenwich Mean Sidereal Time from Julian Date, in radians [0, 2π).
 * Identical algorithm used in PREDICT v2.2.5 for both solar and satellite calculations.
 */
internal fun thetaGJD(jd: Double): Double {
    val ut = fraction(jd + 0.5)
    val aJD = jd - ut
    val tu = (aJD - 2451545.0) / 36525.0
    var gmst = 24110.54841 + tu * (8640184.812866 + tu * (0.093104 - tu * 6.2E-6))
    gmst = modulus(gmst + SEC_PER_DAY * EARTH_ROT_PER_SID_DAY * ut, SEC_PER_DAY)
    return TWO_PI * gmst / SEC_PER_DAY
}

/** Fractional part of [arg]. */
internal fun fraction(arg: Double): Double = arg - floor(arg)

/** Modulo: returns [arg1] mod [arg2], result always in [0, arg2). */
internal fun modulus(arg1: Double, arg2: Double): Double {
    var r = arg1
    val i = floor(r / arg2).toInt()
    r -= i * arg2
    if (r < 0.0) r += arg2
    return r
}

/** Reduce [value] to [0, 2π). */
internal fun mod2PI(value: Double): Double {
    var r = value
    val i = (r / TWO_PI).toInt()
    r -= i * TWO_PI
    if (r < 0.0) r += TWO_PI
    return r
}

/**
 * Delta-ET: difference between Universal Time and Ephemeris Time (seconds).
 * Based on least-squares fit from 1950 to 1991 (PREDICT v2.2.5).
 */
internal fun deltaET(year: Double): Double =
    26.465 + 0.747622 * (year - 1950) + 1.886913 * sin(TWO_PI * (year - 1975) / 33)

/**
 * Convert Unix epoch milliseconds to daynum (days since 31 Dec 1979 00:00:00 UTC).
 */
internal fun millisToDaynum(timeMillis: Long): Double =
    (timeMillis - 315446400000L) / 86400000.0

/** Convert daynum back to Unix epoch milliseconds. */
internal fun daynumToMillis(daynum: Double): Long =
    ((daynum + 3651.0) * 86400000.0).toLong()

/**
 * Compute the Sun's ECI position vector at [julUtc] (Julian UTC).
 * Returns [x, y, z, magnitude] in km.
 * Based on Calculate_Solar_Position() / FindSun() from PREDICT v2.2.5.
 */
internal fun solarPositionECI(julUtc: Double): DoubleArray {
    val mjd = julUtc - 2415020.0
    val year = 1900 + mjd / 365.25
    val t = (mjd + deltaET(year) / SEC_PER_DAY) / 36525.0
    val mDeg = mod360(358.47583 + mod360(35999.04975 * t) - (0.000150 + 0.0000033 * t) * t * t)
    val m = mDeg * DEG2RAD
    val lDeg = mod360(279.69668 + mod360(36000.76892 * t) + 0.0003025 * t * t)
    val l = lDeg * DEG2RAD
    val e = 0.01675104 - (0.0000418 + 0.000000126 * t) * t
    val cDeg = (1.919460 - (0.004789 + 0.000014 * t) * t) * sin(m) +
        (0.020094 - 0.000100 * t) * sin(2 * m) + 0.000293 * sin(3 * m)
    val c = cDeg * DEG2RAD
    val oDeg = mod360(259.18 - 1934.142 * t)
    val o = oDeg * DEG2RAD
    val lsa = mod2PI(l + c - (0.00569 - 0.00479 * sin(o)) * DEG2RAD)
    val nu = mod2PI(m + c)
    var r = 1.0000002 * (1.0 - e * e) / (1.0 + e * cos(nu))
    val epsDeg = 23.452294 - (0.0130125 + (0.00000164 - 0.000000503 * t) * t) * t + 0.00256 * cos(o)
    val eps = epsDeg * DEG2RAD
    r *= ASTRONOMICAL_UNIT
    return doubleArrayOf(r * cos(lsa), r * sin(lsa) * cos(eps), r * sin(lsa) * sin(eps), r)
}

/**
 * Convert ECI position [eciPos] = [x, y, z] (km) to geodetic [lat_rad, lon_rad, alt_km].
 * Based on Calculate_LatLonAlt() from PREDICT v2.2.5.
 */
internal fun eciToGeodetic(julUtc: Double, eciPos: DoubleArray): DoubleArray {
    val thetaPos = atan2(eciPos[1], eciPos[0])
    val lon = mod2PI(thetaPos - thetaGJD(julUtc))
    val r = sqrt(eciPos[0] * eciPos[0] + eciPos[1] * eciPos[1])
    val e2 = FLAT_FACT * (2.0 - FLAT_FACT)
    var lat = atan2(eciPos[2], r)
    var phi: Double
    var c: Double
    var i = 0
    do {
        phi = lat
        c = 1.0 / sqrt(1.0 - e2 * sin(phi) * sin(phi))
        lat = atan2(eciPos[2] + EARTH_RADIUS * c * e2 * sin(phi), r)
    } while (i++ < 10 && abs(lat - phi) >= 1E-10)
    val alt = r / cos(lat) - EARTH_RADIUS * c
    if (lat > PI_2) lat -= TWO_PI
    return doubleArrayOf(lat, lon, alt)
}

// Private helpers

private fun mod360(x: Double): Double {
    var r = x
    val i = (r / 360.0).toInt()
    r -= i * 360.0
    if (r < 0.0) r += 360.0
    return r
}
