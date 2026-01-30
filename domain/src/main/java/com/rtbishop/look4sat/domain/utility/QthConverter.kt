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
package com.rtbishop.look4sat.domain.utility

import com.rtbishop.look4sat.domain.predict.GeoPos

fun qthToPosition(locator: String): GeoPos? {
    val trimmedQth = locator.take(6)
    if (!isValidLocator(trimmedQth)) return null
    val lonFirst = (trimmedQth[0].uppercaseChar().code - 65) * 20
    val latFirst = (trimmedQth[1].uppercaseChar().code - 65) * 10
    val lonSecond = trimmedQth[2].toString().toInt() * 2
    val latSecond = trimmedQth[3].toString().toInt()
    val lonThird = (((trimmedQth[4].lowercaseChar().code - 97) / 12.0) + (1.0 / 24.0)) - 180
    val latThird = (((trimmedQth[5].lowercaseChar().code - 97) / 24.0) + (1.0 / 48.0)) - 90
    val longitude = (lonFirst + lonSecond + lonThird).round(4)
    val latitude = (latFirst + latSecond + latThird).round(4)
    return GeoPos(latitude, longitude)
}

fun positionToQth(latitude: Double, longitude: Double): String? {
    if (!isValidPosition(latitude, longitude)) return null
    val newLongitude = if (longitude > 180.0) longitude else longitude + 180
    val newLatitude = latitude + 90
    val lonFirst = (65 + (newLongitude / 20)).toInt().toChar()
    val latFirst = (65 + (newLatitude / 10)).toInt().toChar()
    val lonSecond = ((newLongitude / 2) % 10).toInt()
    val latSecond = (newLatitude % 10).toInt()
    val lonThird = (65 + (newLongitude % 2) * 12).toInt().toChar().lowercaseChar()
    val latThird = (65 + (newLatitude % 1) * 24).toInt().toChar().lowercaseChar()
    return "$lonFirst$latFirst$lonSecond$latSecond$lonThird$latThird"
}

private fun isValidPosition(lat: Double, lon: Double): Boolean {
    return (lat >= -90.0 && lat <= 90.0) && (lon >= -180.0 && lon <= 360.0)
}

private fun isValidLocator(locator: String): Boolean {
    return locator.matches("[a-xA-X][a-xA-X]\\d\\d[a-xA-X][a-xA-X]".toRegex())
}
