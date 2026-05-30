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
package com.rtbishop.look4sat.feature.radar

import com.rtbishop.look4sat.core.domain.model.SatRadio
import com.rtbishop.look4sat.core.domain.predict.CelestialComputer
import com.rtbishop.look4sat.core.domain.predict.OrbitalPass
import com.rtbishop.look4sat.core.domain.predict.OrbitalPos

data class RadioPanelState(
    val label: String = "",
    val isConnected: Boolean = false,
    val frequencyHz: Long? = null,
    val frequencyDisplay: String = "---",
    val mode: String? = null
)

data class RadioControlSubState(
    val txPanel: RadioPanelState = RadioPanelState("TX (Uplink)"),
    val rxPanel: RadioPanelState = RadioPanelState("RX (Downlink)"),
    val transponders: List<SatRadio> = emptyList(),
    val selectedTransponderUuid: String? = null,
    val txBaseFrequencyHz: Long? = null,
    val ctcssTone: Double? = null,
    val isTracking: Boolean = false,
    val errorMessage: String? = null
)

data class RadarState(
    val currentPass: OrbitalPass? = null,
    val currentTime: String = "00:00:00",
    val isTimeAos: Boolean = true,
    val isLos: Boolean = false,
    val isUtc: Boolean = false,
    val orientationValues: Pair<Float, Float> = 0f to 0f,
    val orbitalPos: OrbitalPos? = null,
    val satTrack: List<OrbitalPos> = emptyList(),
    val shouldShowSweep: Boolean = false,
    val shouldUseCompass: Boolean = false,
    val sunPosition: CelestialComputer.SunPosition? = null,
    val moonPosition: CelestialComputer.MoonPosition? = null,
    val transmitters: List<SatRadio> = emptyList(),
    val selectedTransmitterUuid: String? = null,
    val selectedFrequency: Long? = null,
    val radioControl: RadioControlSubState = RadioControlSubState()
)

sealed interface RadarAction {
    data class AddToCalendar(val name: String, val aosTime: Long, val losTime: Long) : RadarAction
    data class SelectTransmitter(val uuid: String) : RadarAction

    // Radio control actions
    data class SelectTransponder(val uuid: String) : RadarAction
    data class SetTxFrequency(val frequencyHz: Long) : RadarAction
    data class AdjustTxFrequency(val deltaHz: Long) : RadarAction
    data class SetCtcssTone(val toneHz: Double?) : RadarAction
    data object ToggleTracking : RadarAction
    data object ConnectRadios : RadarAction
    data object DisconnectRadios : RadarAction
}
