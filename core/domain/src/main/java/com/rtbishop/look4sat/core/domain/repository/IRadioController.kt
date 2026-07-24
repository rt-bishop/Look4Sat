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
package com.rtbishop.look4sat.core.domain.repository

interface IRadioController {

    val isConnected: Boolean

    suspend fun connect(): Boolean

    suspend fun disconnect()

    suspend fun setFrequency(frequencyHz: Long): Boolean

    suspend fun setMode(mode: String): Boolean

    suspend fun setCtcssMode(enabled: Boolean): Boolean

    suspend fun setCtcssTone(toneHz: Double): Boolean

    suspend fun readFrequencyAndMode(): Pair<Long, String>?

    suspend fun pttOn(): Boolean

    suspend fun pttOff(): Boolean

    // ── Extended operations (IC-705 / CI-V) ──────────────────────────────

    /**
     * Read the current PTT status from the radio.
     * Returns true if transmitting, false if receiving, null on error.
     * Default: not supported.
     */
    suspend fun readPttStatus(): Boolean? = null

    /**
     * Select the active VFO.
     * @param vfoA true → VFO-A (main/RX), false → VFO-B (sub/TX in split).
     * Default: no-op.
     */
    suspend fun setVfo(vfoA: Boolean): Boolean = false

    /**
     * Enable or disable SPLIT mode (TX on sub-VFO, RX on main VFO).
     * Default: not supported.
     */
    suspend fun setSplitMode(enabled: Boolean): Boolean = false

    /**
     * Set the frequency of the currently active VFO.
     * On radios that support it (IC-705: CMD 0x25 sub 0x00) this updates
     * the active VFO without switching.
     * Default: delegates to [setFrequency].
     */
    suspend fun setWorkingFrequency(frequencyHz: Long): Boolean = setFrequency(frequencyHz)
}
