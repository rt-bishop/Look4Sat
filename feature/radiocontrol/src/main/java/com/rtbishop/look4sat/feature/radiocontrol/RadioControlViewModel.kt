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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rtbishop.look4sat.core.domain.model.SatRadio
import com.rtbishop.look4sat.core.domain.predict.OrbitalPass
import com.rtbishop.look4sat.core.domain.repository.IContainerProvider
import com.rtbishop.look4sat.core.domain.repository.IRadioTrackingService
import com.rtbishop.look4sat.core.domain.repository.ISatelliteRepo
import com.rtbishop.look4sat.core.domain.repository.ISettingsRepo
import com.rtbishop.look4sat.core.domain.utility.toDegrees
import com.rtbishop.look4sat.core.domain.utility.toTimerString
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class RadioControlViewModel(
    savedStateHandle: SavedStateHandle,
    private val trackingService: IRadioTrackingService,
    private val satelliteRepo: ISatelliteRepo,
    settingsRepo: ISettingsRepo
) : ViewModel() {

    private val stationPos = settingsRepo.stationPosition.value
    private var currentPass: OrbitalPass? = null
    private var transponders: List<SatRadio> = emptyList()

    private val _uiState = MutableStateFlow(
        RadioControlState(
            currentPass = null,
            currentTime = "00:00:00",
            isCurrentTimeAos = true,
            azimuth = "0.0",
            elevation = "0.0",
            distance = "0.0",
            txPanel = RadioPanelState("TX (Uplink)", false, null, "---", null),
            rxPanel = RadioPanelState("RX (Downlink)", false, null, "---", null),
            transponders = emptyList(),
            selectedTransponderUuid = null,
            txBaseFrequencyHz = null,
            ctcssTone = null,
            isTracking = false,
            errorMessage = null
        )
    )
    val uiState: StateFlow<RadioControlState> = _uiState

    init {
        val catNum = savedStateHandle.get<Int>("catNum") ?: 0
        val aosTime = savedStateHandle.get<Long>("aosTime") ?: 0L

        // Resolve pass and load transponders
        viewModelScope.launch {
            val passes = satelliteRepo.passes.value
            val pass = passes.find { it.catNum == catNum && it.aosTime == aosTime }
                ?: passes.firstOrNull()
            currentPass = pass
            pass?.let { satPass ->
                val allRadios = satelliteRepo.getRadiosWithId(satPass.catNum)
                transponders = allRadios.filter { it.downlinkLow != null }
                _uiState.update {
                    it.copy(currentPass = satPass, transponders = transponders)
                }
                // If service is already tracking this pass, sync the selected transponder
                val svcState = trackingService.state.value
                if (svcState.isActive && svcState.currentPass?.catNum == satPass.catNum) {
                    _uiState.update {
                        it.copy(selectedTransponderUuid = svcState.selectedTransponder?.uuid)
                    }
                }
                // Tick loop — timer and satellite position updates every second
                while (isActive) {
                    val timeNow = System.currentTimeMillis()
                    val pos = satelliteRepo.getPosition(satPass.orbitalObject, stationPos, timeNow)
                    val (timeStr, isAos) = computeTimer(satPass.isDeepSpace, satPass.aosTime, satPass.losTime, timeNow)
                    _uiState.update { state ->
                        state.copy(
                            currentTime = timeStr,
                            isCurrentTimeAos = isAos,
                            azimuth = String.format(Locale.ENGLISH, "%.1f", pos.azimuth.toDegrees()),
                            elevation = String.format(Locale.ENGLISH, "%.1f", pos.elevation.toDegrees()),
                            distance = String.format(Locale.ENGLISH, "%.0f", pos.distance)
                        )
                    }
                    delay(1000)
                }
            }
        }

        // Observe service state for radio-specific updates (panels, frequencies, tracking status)
        viewModelScope.launch {
            trackingService.state.collect { svc ->
                _uiState.update { state ->
                    state.copy(
                        txPanel = RadioPanelState(
                            label = "TX (Uplink)",
                            isConnected = svc.txConnected,
                            frequencyHz = svc.txFrequencyHz,
                            frequencyDisplay = svc.txFrequencyHz?.let { formatFrequency(it) } ?: "---",
                            mode = svc.txMode
                        ),
                        rxPanel = RadioPanelState(
                            label = "RX (Downlink)",
                            isConnected = svc.rxConnected,
                            frequencyHz = svc.rxFrequencyHz,
                            frequencyDisplay = svc.rxFrequencyHz?.let { formatFrequency(it) } ?: "---",
                            mode = svc.rxMode
                        ),
                        txBaseFrequencyHz = svc.txBaseFrequencyHz,
                        ctcssTone = svc.ctcssTone,
                        isTracking = svc.isActive,
                        selectedTransponderUuid = svc.selectedTransponder?.uuid,
                        errorMessage = svc.errorMessage
                    )
                }
            }
        }
    }

    private fun computeTimer(isDeepSpace: Boolean, aosTime: Long, losTime: Long, timeNow: Long): Pair<String, Boolean> {
        return when {
            isDeepSpace -> 0L.toTimerString() to false
            aosTime > timeNow -> (aosTime - timeNow).toTimerString() to true
            else -> (losTime - timeNow).toTimerString() to false
        }
    }

    fun onAction(action: RadioControlAction) {
        when (action) {
            is RadioControlAction.SelectTransponder -> {
                val transponder = transponders.find { it.uuid == action.uuid } ?: return
                trackingService.setTransponder(transponder)
            }
            is RadioControlAction.SetTxFrequency -> trackingService.setTxBaseFrequency(action.frequencyHz)
            is RadioControlAction.AdjustTxFrequency -> trackingService.adjustTxBaseFrequency(action.deltaHz)
            is RadioControlAction.SetCtcssTone -> trackingService.setCtcssTone(action.toneHz)
            RadioControlAction.ToggleTracking -> {
                val svc = trackingService.state.value
                if (svc.isActive) {
                    trackingService.stopTracking()
                } else {
                    val pass = currentPass ?: return
                    val transponder = svc.selectedTransponder ?: return
                    trackingService.startTracking(pass, transponder, svc.txBaseFrequencyHz)
                }
            }
            RadioControlAction.ConnectRadios -> viewModelScope.launch { trackingService.connectRadios() }
            RadioControlAction.DisconnectRadios -> viewModelScope.launch { trackingService.disconnectRadios() }
        }
    }

    companion object {

        val CTCSS_TONES = listOf(67.0, 69.3, 71.9, 74.4, 77.0, 79.7, 82.5, 85.4, 88.5, 91.5,
            94.8, 97.4, 100.0, 103.5, 107.2, 110.9, 114.8, 118.8, 123.0, 127.3, 131.8, 136.5,
            141.3, 146.2, 151.4, 156.7, 162.2, 167.9, 173.8, 179.9, 186.2, 192.8, 203.5, 210.7,
            218.1, 225.7, 233.6, 241.8, 250.3)

        fun formatFrequency(frequencyHz: Long): String {
            if (frequencyHz <= 0) return "---"
            val mhz = frequencyHz / 1_000_000
            val khz = (frequencyHz % 1_000_000) / 1_000
            val hz = frequencyHz % 1_000
            return String.format(Locale.ENGLISH, "%d.%03d.%03d", mhz, khz, hz)
        }

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            val applicationKey = ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY
            initializer {
                val container = (this[applicationKey] as IContainerProvider).getMainContainer()
                RadioControlViewModel(
                    createSavedStateHandle(),
                    container.radioTrackingService,
                    container.satelliteRepo,
                    container.settingsRepo
                )
            }
        }
    }
}
