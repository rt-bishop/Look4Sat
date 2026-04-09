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

import com.rtbishop.look4sat.core.domain.model.SatRadio
import com.rtbishop.look4sat.core.domain.predict.OrbitalObject
import com.rtbishop.look4sat.core.domain.predict.OrbitalPass
import kotlinx.coroutines.flow.StateFlow

data class RadioTrackingState(
    val isActive: Boolean = false,
    val txConnected: Boolean = false,
    val rxConnected: Boolean = false,
    val txFrequencyHz: Long? = null,
    val rxFrequencyHz: Long? = null,
    val txMode: String? = null,
    val rxMode: String? = null,
    val ctcssTone: Double? = null,
    val txBaseFrequencyHz: Long? = null,
    val selectedTransponder: SatRadio? = null,
    val currentPass: OrbitalPass? = null,
    val azimuth: Double = 0.0,
    val elevation: Double = 0.0,
    val distance: Double = 0.0,
    val errorMessage: String? = null
)

interface IRadioTrackingService {
    val state: StateFlow<RadioTrackingState>

    suspend fun connectRadios()
    suspend fun disconnectRadios()
    fun startTracking(pass: OrbitalPass, transponder: SatRadio, txBaseFreqHz: Long?)
    fun stopTracking()
    fun setTransponder(transponder: SatRadio)
    fun setTxBaseFrequency(frequencyHz: Long)
    fun adjustTxBaseFrequency(deltaHz: Long)
    fun setCtcssTone(toneHz: Double?)
    fun setMode(txMode: String, rxMode: String)
}
