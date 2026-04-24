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
package com.rtbishop.look4sat.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SatRadio(
    @SerialName("uuid") val uuid: String,
    @SerialName("description") val info: String,
    @SerialName("alive") val isAlive: Boolean,
    @SerialName("downlink_low") val downlinkLow: Long?,
    @SerialName("downlink_high") val downlinkHigh: Long?,
    @SerialName("mode") val downlinkMode: String?,
    @SerialName("uplink_low") val uplinkLow: Long?,
    @SerialName("uplink_high") val uplinkHigh: Long?,
    @SerialName("uplink_mode") val uplinkMode: String?,
    @SerialName("invert") val isInverted: Boolean,
    @SerialName("norad_cat_id") val catnum: Int?
)
