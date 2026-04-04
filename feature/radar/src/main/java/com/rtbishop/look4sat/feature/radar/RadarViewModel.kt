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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rtbishop.look4sat.core.domain.model.SatRadio
import com.rtbishop.look4sat.core.domain.predict.OrbitalObject
import com.rtbishop.look4sat.core.domain.predict.OrbitalPos
import com.rtbishop.look4sat.core.domain.repository.IContainerProvider
import com.rtbishop.look4sat.core.domain.repository.IReporter
import com.rtbishop.look4sat.core.domain.repository.ISatelliteRepo
import com.rtbishop.look4sat.core.domain.repository.ISensorsRepo
import com.rtbishop.look4sat.core.domain.repository.ISettingsRepo
import com.rtbishop.look4sat.core.domain.usecase.IAddToCalendar
import com.rtbishop.look4sat.core.domain.utility.round
import com.rtbishop.look4sat.core.domain.utility.toDegrees
import com.rtbishop.look4sat.core.domain.utility.toTimerString
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RadarViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val bluetoothReporter: IReporter,
    private val networkReporter: IReporter,
    private val satelliteRepo: ISatelliteRepo,
    private val settingsRepo: ISettingsRepo,
    private val sensorsRepo: ISensorsRepo,
    private val addToCalendar: IAddToCalendar
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        RadarState(
            currentPass = null,
            currentTime = "00:00:00",
            isCurrentTimeAos = true,
            orientationValues = sensorsRepo.orientation.value,
            orbitalPos = null,
            satTrack = emptyList(),
            shouldShowSweep = settingsRepo.otherSettings.value.stateOfSweep,
            shouldUseCompass = settingsRepo.otherSettings.value.stateOfSensors,
            selectedTransmitterUuid = null,
            selectedFrequency = null,
            transmitters = emptyList(),
            sendAction = ::handleAction,
        )
    )
    private val stationPos = settingsRepo.stationPosition.value
    private val magDeclination = sensorsRepo.getMagDeclination(stationPos)
    val uiState: StateFlow<RadarState> = _uiState

    init {
        // Compass sensor collection
        if (settingsRepo.otherSettings.value.stateOfSensors) {
            viewModelScope.launch {
                sensorsRepo.enableSensor()
                sensorsRepo.orientation.collect { data ->
                    val orientationValues = Pair(data.first + magDeclination, data.second)
                    _uiState.update { it.copy(orientationValues = orientationValues) }
                }
            }
        }
        // Resolve which pass we're tracking
        viewModelScope.launch {
            val catNum = savedStateHandle.get<Int>("catNum") ?: 0
            val aosTime = savedStateHandle.get<Long>("aosTime") ?: 0L
            val passes = satelliteRepo.passes.value
            val pass = passes.find { pass -> pass.catNum == catNum && pass.aosTime == aosTime }
            val currentPass = pass ?: passes.firstOrNull()
            currentPass?.let { satPass ->
                _uiState.update { it.copy(currentPass = satPass) }
                val transmitters = satelliteRepo.getRadiosWithId(satPass.catNum)
                // Compute track once (it doesn't change)
                if (!satPass.isDeepSpace) {
                    val track = satelliteRepo.getTrack(
                        satPass.orbitalObject, stationPos, satPass.aosTime, satPass.losTime
                    )
                    _uiState.update { it.copy(satTrack = track) }
                }
                // Local tick loop — computes position only while the radar screen is alive
                while (isActive) {
                    val timeNow = System.currentTimeMillis()
                    val pos = satelliteRepo.getPosition(satPass.orbitalObject, stationPos, timeNow)
                    when {
                        satPass.isDeepSpace -> {
                            val time = 0L.toTimerString()
                            _uiState.update { it.copy(currentTime = time, isCurrentTimeAos = false, orbitalPos = pos) }
                        }
                        satPass.aosTime > timeNow -> {
                            val time = satPass.aosTime.minus(timeNow).toTimerString()
                            _uiState.update { it.copy(currentTime = time, isCurrentTimeAos = true, orbitalPos = pos) }
                        }
                        else -> {
                            val time = satPass.losTime.minus(timeNow).toTimerString()
                            _uiState.update { it.copy(currentTime = time, isCurrentTimeAos = false, orbitalPos = pos) }
                        }
                    }
                    processRadios(transmitters, satPass.orbitalObject, timeNow)
                    sendPassData(pos)
                    sendPassDataBT(pos)
                    delay(1000)
                }
            }
        }
    }

    override fun onCleared() {
        sensorsRepo.disableSensor()
        super.onCleared()
    }

    private fun handleAction(action: RadarAction) {
        when (action) {
            is RadarAction.AddToCalendar -> addToCalendar(action.name, action.aosTime, action.losTime)
            is RadarAction.SelectTransmitter -> { _uiState.update { it.copy(selectedTransmitterUuid = action.uuid) } }
        }
    }

    private fun sendPassData(orbitalPos: OrbitalPos) {
        viewModelScope.launch {
            val rc = settingsRepo.rcSettings.value
            if (rc.rotatorState) {
                val azimuth = orbitalPos.azimuth.toDegrees().round(2)
                val elevation = orbitalPos.elevation.toDegrees().round(2)
                networkReporter.reportRotation(rc.rotatorFormat, azimuth, elevation)
            }
            if (rc.frequencyState) {
                _uiState.value.selectedFrequency?.let { freq ->
                    networkReporter.reportFrequency(rc.frequencyFormat, freq)
                }
            }
        }
    }

    private fun sendPassDataBT(orbitalPos: OrbitalPos) {
        viewModelScope.launch {
            val rc = settingsRepo.rcSettings.value
            if (rc.bluetoothRotatorState) {
                val azimuth = orbitalPos.azimuth.toDegrees().round(0)
                val elevation = orbitalPos.elevation.toDegrees().round(0)
                bluetoothReporter.reportRotation(rc.bluetoothRotatorFormat, azimuth, elevation)
            }
            if (rc.bluetoothFrequencyState) {
                _uiState.value.selectedFrequency?.let { freq ->
                    bluetoothReporter.reportFrequency(rc.bluetoothFrequencyFormat, freq)
                }
            }
        }
    }

    private suspend fun processRadios(radios: List<SatRadio>, orbitalObject: OrbitalObject, time: Long) {
        val transmitters = satelliteRepo.getRadios(orbitalObject, stationPos, radios, time)
        _uiState.update { state ->
            if (!settingsRepo.rcSettings.value.frequencyState && !settingsRepo.rcSettings.value.bluetoothFrequencyState) {
                return@update state.copy(
                    transmitters = transmitters,
                    selectedTransmitterUuid = null,
                    selectedFrequency = null
                )
            }
            val selectedUuid = state.selectedTransmitterUuid ?: transmitters.firstOrNull()?.uuid
            val selectedRadio = transmitters.firstOrNull { it.uuid == selectedUuid }
            val freq = selectedRadio?.let { radio ->
                val low = radio.downlinkLow
                val high = radio.downlinkHigh
                when {
                    low != null && high != null ->
                        (low + high) / 2
                    low != null ->
                        low
                    else -> null
                }
            }
            state.copy(
                transmitters = transmitters,
                selectedTransmitterUuid = selectedUuid,
                selectedFrequency = freq
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            val applicationKey = ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY
            initializer {
                val container = (this[applicationKey] as IContainerProvider).getMainContainer()
                RadarViewModel(
                    createSavedStateHandle(),
                    container.provideBluetoothReporter(),
                    container.provideNetworkReporter(),
                    container.satelliteRepo,
                    container.settingsRepo,
                    container.provideSensorsRepo(),
                    container.provideAddToCalendar()
                )
            }
        }
    }
}
