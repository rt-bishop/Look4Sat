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

import com.rtbishop.look4sat.core.domain.model.SatRadio

object TransponderMapper {

    fun mapUplinkToDownlink(txFreqHz: Long, transponder: SatRadio): Long? {
        val uplinkLow = transponder.uplinkLow ?: return null
        val downlinkLow = transponder.downlinkLow ?: return null

        // Single-frequency transponder (FM repeater) - just return the downlink freq
        val uplinkHigh = transponder.uplinkHigh
        val downlinkHigh = transponder.downlinkHigh
        if (uplinkHigh == null || downlinkHigh == null
            || uplinkLow == uplinkHigh || downlinkLow == downlinkHigh
        ) {
            return downlinkLow
        }

        // Passband transponder (linear) - map within the band
        val offset = txFreqHz - uplinkLow
        return if (transponder.isInverted) {
            downlinkHigh - offset
        } else {
            downlinkLow + offset
        }
    }

    fun mapDownlinkToUplink(rxFreqHz: Long, transponder: SatRadio): Long? {
        val uplinkLow = transponder.uplinkLow ?: return null
        val downlinkLow = transponder.downlinkLow ?: return null

        val uplinkHigh = transponder.uplinkHigh
        val downlinkHigh = transponder.downlinkHigh
        if (uplinkHigh == null || downlinkHigh == null
            || uplinkLow == uplinkHigh || downlinkLow == downlinkHigh
        ) {
            return uplinkLow
        }

        return if (transponder.isInverted) {
            uplinkLow + (downlinkHigh - rxFreqHz)
        } else {
            uplinkLow + (rxFreqHz - downlinkLow)
        }
    }

    fun mapUplinkModeToDownlinkMode(txMode: String, isInverted: Boolean): String {
        if (!isInverted) return txMode
        return when (txMode.uppercase()) {
            "USB" -> "LSB"
            "LSB" -> "USB"
            else -> txMode
        }
    }
}
