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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RadarViewModel(
    private val catNum: Int,
    private val aosTime: Long,
    private val bluetoothReporter: IReporter,
    private val networkReporter: IReporter,
    private val satelliteRepo: ISatelliteRepo,
    private val settingsRepo: ISettingsRepo,
    private val sensorsRepo: ISensorsRepo,
    private val addToCalendar: IAddToCalendar
) : ViewModel() {

    private val stationPos = settingsRepo.stationPosition.value
    private val magDeclination = sensorsRepo.getMagDeclination(stationPos)
    private val _uiState = MutableStateFlow(
        RadarState(
            isUtc = settingsRepo.otherSettings.value.stateOfUtc,
            orientationValues = sensorsRepo.orientation.value,
            shouldShowSweep = settingsRepo.otherSettings.value.stateOfSweep,
            shouldUseCompass = settingsRepo.otherSettings.value.stateOfSensors
        )
    )
    val uiState: StateFlow<RadarState> = _uiState

    init {
        // Compass sensor collection
        if (settingsRepo.otherSettings.value.stateOfSensors) {
            viewModelScope.launch {
                sensorsRepo.enableSensor()
                sensorsRepo.orientation.collect { data ->
                    val orientationValues = (data.first + magDeclination) to data.second
                    _uiState.update { it.copy(orientationValues = orientationValues) }
                }
            }
        }
        // React to UTC setting changes
        viewModelScope.launch {
            settingsRepo.otherSettings.collectLatest { settings ->
                _uiState.update { it.copy(isUtc = settings.stateOfUtc) }
            }
        }
        // Resolve which pass we're tracking and start the tick loop
        viewModelScope.launch {
            val passes = satelliteRepo.passes.value
            val currentPass = passes.find { it.catNum == catNum && it.aosTime == aosTime }
                ?: passes.firstOrNull()
            currentPass?.let { satPass ->
                _uiState.update { it.copy(currentPass = satPass) }
                val transmitters = satelliteRepo.getRadiosWithId(satPass.catNum)
                // Compute track once (it doesn't change for a given pass)
                if (!satPass.isDeepSpace) {
                    val track = satelliteRepo.getTrack(
                        satPass.orbitalObject, stationPos, satPass.aosTime, satPass.losTime
                    )
                    _uiState.update { it.copy(satTrack = track) }
                }
                // Tick loop — position, timer, and radio updates every second
                while (isActive) {
                    val timeNow = System.currentTimeMillis()
                    val pos = satelliteRepo.getPosition(satPass.orbitalObject, stationPos, timeNow)
                    val (time, isAos) = computeTimer(satPass.isDeepSpace, satPass.aosTime, satPass.losTime, timeNow)
                    val isLos = !satPass.isDeepSpace && timeNow > satPass.losTime
                    _uiState.update { it.copy(currentTime = time, isTimeAos = isAos, isLos = isLos, orbitalPos = pos) }
                    processRadios(transmitters, satPass.orbitalObject, timeNow)
                    sendPassData(pos)
                    delay(1000)
                }
            }
        }
    }

    override fun onCleared() {
        sensorsRepo.disableSensor()
        super.onCleared()
    }

    fun onAction(action: RadarAction) {
        when (action) {
            is RadarAction.AddToCalendar -> addToCalendar(action.name, action.aosTime, action.losTime)
            is RadarAction.SelectTransmitter -> _uiState.update { it.copy(selectedTransmitterUuid = action.uuid) }
        }
    }

    private fun computeTimer(isDeepSpace: Boolean, aosTime: Long, losTime: Long, timeNow: Long): Pair<String, Boolean> {
        return when {
            isDeepSpace -> 0L.toTimerString() to false
            aosTime > timeNow -> (aosTime - timeNow).toTimerString() to true
            else -> (losTime - timeNow).toTimerString() to false
        }
    }

    private fun sendPassData(orbitalPos: OrbitalPos) {
        val rc = settingsRepo.rcSettings.value
        sendReporterData(
            networkReporter, orbitalPos,
            rc.rotatorState, rc.rotatorFormat,
            rc.frequencyState, rc.frequencyFormat
        )
        sendReporterData(
            bluetoothReporter, orbitalPos,
            rc.bluetoothRotatorState, rc.bluetoothRotatorFormat,
            rc.bluetoothFrequencyState, rc.bluetoothFrequencyFormat
        )
    }

    private fun sendReporterData(
        reporter: IReporter,
        orbitalPos: OrbitalPos,
        rotatorEnabled: Boolean,
        rotatorFormat: String,
        frequencyEnabled: Boolean,
        frequencyFormat: String
    ) {
        if (rotatorEnabled) {
            val azimuth = orbitalPos.azimuth.toDegrees().round(2)
            val elevation = orbitalPos.elevation.toDegrees().round(2)
            reporter.reportRotation(rotatorFormat, azimuth, elevation)
        }
        if (frequencyEnabled) {
            _uiState.value.selectedFrequency?.let { freq ->
                reporter.reportFrequency(frequencyFormat, freq)
            }
        }
    }

    private suspend fun processRadios(radios: List<SatRadio>, orbitalObject: OrbitalObject, time: Long) {
        val transmitters = satelliteRepo.getRadios(orbitalObject, stationPos, radios, time)
        val isFreqEnabled =
            settingsRepo.rcSettings.value.frequencyState || settingsRepo.rcSettings.value.bluetoothFrequencyState
        _uiState.update { state ->
            if (!isFreqEnabled) {
                // Skip update if nothing changed
                if (state.transmitters == transmitters && state.selectedTransmitterUuid == null && state.selectedFrequency == null) {
                    return@update state
                }
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
                    low != null && high != null -> (low + high) / 2
                    low != null -> low
                    else -> null
                }
            }
            // Skip update if nothing changed
            if (state.transmitters == transmitters && state.selectedTransmitterUuid == selectedUuid && state.selectedFrequency == freq) {
                return@update state
            }
            state.copy(transmitters = transmitters, selectedTransmitterUuid = selectedUuid, selectedFrequency = freq)
        }
    }

    companion object {
        fun factory(catNum: Int, aosTime: Long): ViewModelProvider.Factory = viewModelFactory {
            val applicationKey = ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY
            initializer {
                val container = (this[applicationKey] as IContainerProvider).getMainContainer()
                RadarViewModel(
                    catNum = catNum,
                    aosTime = aosTime,
                    bluetoothReporter = container.provideBluetoothReporter(),
                    networkReporter = container.provideNetworkReporter(),
                    satelliteRepo = container.satelliteRepo,
                    settingsRepo = container.settingsRepo,
                    sensorsRepo = container.provideSensorsRepo(),
                    addToCalendar = container.provideAddToCalendar()
                )
            }
        }
    }
}
