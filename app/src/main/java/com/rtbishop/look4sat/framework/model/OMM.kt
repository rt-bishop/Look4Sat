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
package com.rtbishop.look4sat.framework.model

import com.rtbishop.look4sat.domain.predict.TLE
import com.squareup.moshi.Json
import java.text.SimpleDateFormat
import java.util.*

data class OMM(
    @field:Json(name = "OBJECT_NAME") val name: String,
    @field:Json(name = "EPOCH") val epochString: String,
    @field:Json(name = "MEAN_MOTION") val meanmo: Double,
    @field:Json(name = "ECCENTRICITY") val eccn: Double,
    @field:Json(name = "INCLINATION") val incl: Double,
    @field:Json(name = "RA_OF_ASC_NODE") val raan: Double,
    @field:Json(name = "ARG_OF_PERICENTER") val argper: Double,
    @field:Json(name = "MEAN_ANOMALY") val meanan: Double,
    @field:Json(name = "NORAD_CAT_ID") val catnum: Int,
    @field:Json(name = "BSTAR") val bstar: Double,
) {

    fun toTLE(): TLE {
        calendar.time = sdf.parse(epochString) ?: Date()
        val year = calendar.get(Calendar.YEAR).toString().substring(2, 4)
        val day = calendar.get(Calendar.DAY_OF_YEAR)
        val hours = calendar.get(Calendar.HOUR_OF_DAY)
        val minutes = calendar.get(Calendar.MINUTE)
        val seconds = calendar.get(Calendar.SECOND)
        val fraction = ((hours * 60 * 60 + minutes * 60 + seconds) / secondsInDay).toString()
        val epoch = "$year$day${fraction.substring(1, fraction.length)}".toDouble()
        return TLE(name, epoch, meanmo, eccn, incl, raan, argper, meanan, catnum, bstar)
    }

    companion object {
        private const val secondsInDay = 86400.0
        private const val pattern = "yyyy-MM-dd'T'HH:mm:ss"
        private val calendar = Calendar.getInstance()
        private val sdf = SimpleDateFormat(pattern, Locale.US)
    }
}
