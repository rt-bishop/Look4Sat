/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2026 Arty Bishop and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.rtbishop.look4sat.core.domain.utility

import com.rtbishop.look4sat.core.domain.model.SatRadio
import com.rtbishop.look4sat.core.domain.predict.OrbitalPos
import java.util.Locale

/**
 * Computes Doppler-corrected reciprocal frequencies for linear transponders.
 *
 * For a linear (passband) transponder, uplink and downlink frequencies are
 * related by a fixed passband offset. When the satellite moves, both are
 * Doppler-shifted. Given one, this computes the other:
 *
 *   downlink → uplink: mapDownlinkToUplink (passband) → getUplinkFreq (Doppler)
 *   uplink → downlink: mapUplinkToDownlink (passband) → getDownlinkFreq (Doppler)
 */
object DopplerFrequencyCalculator {

    /**
     * Given a downlink frequency, compute the Doppler-corrected uplink frequency.
     * Returns null if the transponder is not a linear passband type.
     */
    fun computeUplinkFromDownlink(
        downlinkHz: Long,
        transponder: SatRadio,
        orbitalPos: OrbitalPos
    ): Long? {
        if (!isLinearTransponder(transponder)) return null
        val baseUplink = TransponderMapper.mapDownlinkToUplink(downlinkHz, transponder) ?: return null
        return orbitalPos.getUplinkFreq(baseUplink)
    }

    /**
     * Given a downlink frequency, compute the Doppler-corrected uplink frequency
     * with an offset applied to the downlink (in Hz).
     * Returns null if the transponder is not a linear passband type.
     *
     * The user-entered downlink frequency already includes the offset, so subtract
     * it before mapping the downlink passband position back to the uplink.
     */
    fun computeUplinkFromDownlinkWithOffset(
        downlinkHz: Long,
        transponder: SatRadio,
        orbitalPos: OrbitalPos,
        offsetHz: Long
    ): Long? {
        if (!isLinearTransponder(transponder)) return null
        val baseUplink = TransponderMapper.mapDownlinkToUplink(downlinkHz - offsetHz, transponder) ?: return null
        return orbitalPos.getUplinkFreq(baseUplink)
    }

    /**
     * Given an uplink frequency, compute the Doppler-corrected downlink frequency.
     * Returns null if the transponder is not a linear passband type.
     */
    fun computeDownlinkFromUplink(
        uplinkHz: Long,
        transponder: SatRadio,
        orbitalPos: OrbitalPos
    ): Long? {
        if (!isLinearTransponder(transponder)) return null
        val baseDownlink = TransponderMapper.mapUplinkToDownlink(uplinkHz, transponder) ?: return null
        return orbitalPos.getDownlinkFreq(baseDownlink)
    }

    /**
     * Given an uplink frequency, compute the Doppler-corrected downlink frequency
     * with an offset applied to the downlink (in Hz).
     * Returns null if the transponder is not a linear passband type.
     */
    fun computeDownlinkFromUplinkWithOffset(
        uplinkHz: Long,
        transponder: SatRadio,
        orbitalPos: OrbitalPos,
        offsetHz: Long
    ): Long? {
        if (!isLinearTransponder(transponder)) return null
        val baseDownlink = TransponderMapper.mapUplinkToDownlink(uplinkHz, transponder) ?: return null
        return orbitalPos.getDownlinkFreq(baseDownlink + offsetHz)
    }

    /** True if this transponder supports linear passband mapping. */
    fun isLinearTransponder(transponder: SatRadio): Boolean {
        val upLow = transponder.uplinkLow
        val upHigh = transponder.uplinkHigh
        val downLow = transponder.downlinkLow
        val downHigh = transponder.downlinkHigh
        return upLow != null && upHigh != null && downLow != null && downHigh != null
                && upLow != upHigh && downLow != downHigh
    }

    /**
     * True for the radio entry that should drive the standalone Calculator page.
     *
     * A frequency range alone is not enough: some non-user-facing or drifting data entries
     * can also have low/high frequencies. The calculator is meant for named linear
     * transponders, e.g. "Linear Transponder", "Linear Transp.", "SSB Transponder".
     */
    fun isNamedLinearTransponder(transponder: SatRadio): Boolean {
        if (!isLinearTransponder(transponder)) return false

        val info = transponder.info.lowercase(Locale.ENGLISH)
        val modes = listOfNotNull(transponder.downlinkMode, transponder.uplinkMode)
            .joinToString(separator = " ")
            .lowercase(Locale.ENGLISH)
        val hasLinearName = info.contains("linear") || info.contains(" lin") || info.startsWith("lin")
        val hasTransponderName = info.contains("transponder") || info.contains("transp") ||
                info.contains("xponder") || info.contains("xpdr")
        val hasLinearMode = listOf("ssb", "usb", "lsb", "cw").any { modes.contains(it) }

        return (hasLinearName && hasTransponderName) || (hasTransponderName && hasLinearMode) ||
                (hasLinearName && hasLinearMode)
    }

    /**
     * Removes duplicate transponder entries that describe the same physical
     * transponder with different mode labels (e.g. SatNOGS lists AO-7's Mode A
     * as both "Lin SSB" and "Lin CW", and JO-97's U/V transponder as both
     * "CW Transponder" and "SSB Transponder").
     *
     * Entries sharing the same uplink/downlink frequency range are considered
     * the same transponder. The non-CW entry is preferred because its invert
     * flag is more reliable (e.g. JO-97's CW entry wrongly has invert=false).
     */
    fun deduplicateTransponders(radios: List<SatRadio>): List<SatRadio> {
        return radios.groupBy { radio ->
            listOf(radio.uplinkLow, radio.uplinkHigh, radio.downlinkLow, radio.downlinkHigh)
        }.values.map { group ->
            group.firstOrNull { it.downlinkMode?.equals("CW", ignoreCase = true) != true } ?: group.first()
        }
    }
}
