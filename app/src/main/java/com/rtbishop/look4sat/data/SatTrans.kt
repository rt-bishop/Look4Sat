/*
 * Look4Sat. Amateur radio and weather satellite tracker and passes predictor for Android.
 * Copyright (C) 2019, 2020 Arty Bishop (bishop.arty@gmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.rtbishop.look4sat.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json

@Entity(tableName = "transmitters")
data class SatTrans(
    @PrimaryKey @Json(name = "uuid") val uuid: String,
    @Json(name = "description") val description: String,
    @Json(name = "alive") val isAlive: Boolean,
    @Json(name = "uplink_low") val uplinkLow: Long?,
    @Json(name = "uplink_high") val uplinkHigh: Long?,
    @Json(name = "downlink_low") val downlinkLow: Long?,
    @Json(name = "downlink_high") val downlinkHigh: Long?,
    @Json(name = "mode") val mode: String?,
    @Json(name = "invert") val isInverted: Boolean,
    @Json(name = "norad_cat_id") val catNum: Int
)
