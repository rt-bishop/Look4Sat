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
package com.rtbishop.look4sat.feature.radiocontrol

import com.rtbishop.look4sat.core.domain.model.SatRadio
import com.rtbishop.look4sat.core.domain.predict.OrbitalPass

data class RadioPanelState(
    val label: String,
    val isConnected: Boolean,
    val frequencyHz: Long?,
    val frequencyDisplay: String,
    val mode: String?
)

data class RadioControlState(
    val currentPass: OrbitalPass?,
    val currentTime: String,
    val isCurrentTimeAos: Boolean,
    val azimuth: String,
    val elevation: String,
    val distance: String,
    val txPanel: RadioPanelState,
    val rxPanel: RadioPanelState,
    val transponders: List<SatRadio>,
    val selectedTransponderUuid: String?,
    val txBaseFrequencyHz: Long?,
    val ctcssTone: Double?,
    val isTracking: Boolean,
    val errorMessage: String?
)

sealed interface RadioControlAction {
    data class SelectTransponder(val uuid: String) : RadioControlAction
    data class SetTxFrequency(val frequencyHz: Long) : RadioControlAction
    data class AdjustTxFrequency(val deltaHz: Long) : RadioControlAction
    data class SetCtcssTone(val toneHz: Double?) : RadioControlAction
    data object ToggleTracking : RadioControlAction
    data object ConnectRadios : RadioControlAction
    data object DisconnectRadios : RadioControlAction
}
