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

import com.squareup.moshi.Json

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
)
