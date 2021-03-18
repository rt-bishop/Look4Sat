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
package com.rtbishop.look4sat.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json

@Entity(tableName = "transmitters")
class SatTrans(
    @PrimaryKey @field:Json(name = "uuid") val uuid: String,
    @field:Json(name = "description") val description: String,
    @field:Json(name = "alive") val isAlive: Boolean,
    @field:Json(name = "uplink_low") val uplinkLow: Long?,
    @field:Json(name = "uplink_high") val uplinkHigh: Long?,
    @field:Json(name = "downlink_low") val downlinkLow: Long?,
    @field:Json(name = "downlink_high") val downlinkHigh: Long?,
    @field:Json(name = "mode") val mode: String?,
    @field:Json(name = "invert") val isInverted: Boolean,
    @field:Json(name = "norad_cat_id") val catNum: Int
)
