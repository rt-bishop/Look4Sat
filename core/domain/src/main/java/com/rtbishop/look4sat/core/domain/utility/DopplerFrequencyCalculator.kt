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
 * The full physical path:
 *
 * TX to RX (uplink -> downlink):
 *   1. Ground transmits f_tx
 *   2. Satellite receives f_tx * (c - v) / c (uplink Doppler)
 *   3. Satellite transmits the passband mapping of (2) (mapping happens on board)
 *   4. Ground hears (3) * (c - v) / c (downlink Doppler)
 *
 * RX to TX (downlink -> uplink), the same chain in reverse:
 *   4. Ground hears f_rx
 *   3. Satellite transmits f_rx * (c + v) / c (undo downlink Doppler)
 *   2. Satellite receives the inverse passband mapping of (3)
 *   1. Ground must transmit (2) * (c + v) / c (undo uplink Doppler)
 *
 * Both legs of the trip are Doppler shifted and by different amounts (uplink
 * and downlink frequencies differ), so the mapping must happen between the two
 * shifts on satellite-received frequencies. Addresses GitHub issue #91
 * (Custom frequency Doppler correction).
 */
object DopplerFrequencyCalculator {

    /**
     * Given a downlink frequency (what the user hears), compute the
     * uplink frequency the user should transmit.
     * Full path: 4 -> 3 -> 2 -> 1
     */
    fun computeUplinkFromDownlink(
        downlinkHz: Long,
        transponder: SatRadio,
        orbitalPos: OrbitalPos
    ): Long? {
        if (!isLinearTransponder(transponder)) return null
        // 4 -> 3 undo the downlink Doppler: the frequency the satellite transmits
        val satTx = orbitalPos.getUplinkFreq(downlinkHz)
        // 3 -> 2 inverse passband mapping
        val satRx = TransponderMapper.mapDownlinkToUplink(satTx, transponder) ?: return null
        // 2 -> 1 undo the uplink Doppler: the frequency the ground station transmits
        return orbitalPos.getUplinkFreq(satRx)
    }

    /**
     * Given a downlink frequency (what the user hears), compute the
     * uplink frequency the user should transmit, with an offset applied
     * to the downlink (in Hz).
     * Full path: 4 -> 3 -> 2 -> 1
     *
     * The user-entered downlink frequency already includes the offset, so subtract
     * it before the inverse passband mapping. The offset lives in the satellite
     * frequency domain, hence it is removed after undoing the downlink Doppler.
     */
    fun computeUplinkFromDownlinkWithOffset(
        downlinkHz: Long,
        transponder: SatRadio,
        orbitalPos: OrbitalPos,
        offsetHz: Long
    ): Long? {
        if (!isLinearTransponder(transponder)) return null
        // 4 -> 3 undo the downlink Doppler (the offset travels with it)
        val satTxWithOffset = orbitalPos.getUplinkFreq(downlinkHz)
        // 3 remove the offset (it lives in the satellite frequency domain)
        val satTx = satTxWithOffset - offsetHz
        // 3 -> 2 inverse passband mapping
        val satRx = TransponderMapper.mapDownlinkToUplink(satTx, transponder) ?: return null
        // 2 -> 1 undo the uplink Doppler
        return orbitalPos.getUplinkFreq(satRx)
    }

    /**
     * Given an uplink frequency (what the user transmits), compute the
     * downlink frequency the user will hear.
     * Full path: 1 -> 2 -> 3 -> 4
     */
    fun computeDownlinkFromUplink(
        uplinkHz: Long,
        transponder: SatRadio,
        orbitalPos: OrbitalPos
    ): Long? {
        if (!isLinearTransponder(transponder)) return null
        // 1 -> 2 uplink Doppler: the frequency the satellite receives
        val satRx = orbitalPos.getDownlinkFreq(uplinkHz)
        // 2 -> 3 passband mapping
        val satTx = TransponderMapper.mapUplinkToDownlink(satRx, transponder) ?: return null
        // 3 -> 4 downlink Doppler: what the ground station hears
        return orbitalPos.getDownlinkFreq(satTx)
    }

    /**
     * Given an uplink frequency (what the user transmits), compute the
     * downlink frequency the user will hear, with an offset applied
     * to the downlink (in Hz).
     * Full path: 1 -> 2 -> 3 -> 4
     */
    fun computeDownlinkFromUplinkWithOffset(
        uplinkHz: Long,
        transponder: SatRadio,
        orbitalPos: OrbitalPos,
        offsetHz: Long
    ): Long? {
        if (!isLinearTransponder(transponder)) return null
        // 1 -> 2 uplink Doppler: the frequency the satellite receives
        val satRx = orbitalPos.getDownlinkFreq(uplinkHz)
        // 2 -> 3 passband mapping
        val satTx = TransponderMapper.mapUplinkToDownlink(satRx, transponder) ?: return null
        // 3 apply the offset (satellite frequency domain), 3 -> 4 downlink Doppler
        return orbitalPos.getDownlinkFreq(satTx + offsetHz)
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
