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
package com.rtbishop.look4sat.presentation.radar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rtbishop.look4sat.MainApplication
import com.rtbishop.look4sat.data.framework.WithoutExtParams
import com.rtbishop.look4sat.data.framework.BluetoothReporter
import com.rtbishop.look4sat.data.framework.ExtendedParams
import com.rtbishop.look4sat.data.framework.NetworkReporter
import com.rtbishop.look4sat.domain.model.SatRadio
import com.rtbishop.look4sat.domain.predict.OrbitalObject
import com.rtbishop.look4sat.domain.predict.OrbitalPass
import com.rtbishop.look4sat.domain.predict.OrbitalPos
import com.rtbishop.look4sat.domain.repository.ISatelliteRepo
import com.rtbishop.look4sat.domain.repository.ISensorsRepo
import com.rtbishop.look4sat.domain.repository.ISettingsRepo
import com.rtbishop.look4sat.domain.usecase.IAddToCalendar
import com.rtbishop.look4sat.domain.utility.round
import com.rtbishop.look4sat.domain.utility.toDegrees
import com.rtbishop.look4sat.domain.utility.toTimerString
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RadarViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val bluetoothReporter: BluetoothReporter,
    private val networkReporter: NetworkReporter,
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
        if (settingsRepo.otherSettings.value.stateOfSensors) {
            viewModelScope.launch {
                sensorsRepo.enableSensor()
                sensorsRepo.orientation.collect { data ->
                    val orientationValues = Pair(data.first + magDeclination, data.second)
                    _uiState.update { it.copy(orientationValues = orientationValues) }
                }
            }
        }
        viewModelScope.launch {
            val catNum = savedStateHandle.get<Int>("catNum") ?: 0
            val aosTime = savedStateHandle.get<Long>("aosTime") ?: 0L
            val passes = satelliteRepo.passes.value
            val pass = passes.find { pass -> pass.catNum == catNum && pass.aosTime == aosTime }
            val currentPass = pass ?: passes.firstOrNull()
            currentPass?.let { satPass ->
                _uiState.update { it.copy(currentPass = satPass) }
                val transmitters = satelliteRepo.getRadiosWithId(satPass.catNum)
                while (isActive) {
                    val timeNow = System.currentTimeMillis()
                    val pos = satelliteRepo.getPosition(satPass.orbitalObject, stationPos, timeNow)
                    when {
                        satPass.isDeepSpace -> {
                            val time = 0L.toTimerString()
                            _uiState.update { it.copy(currentTime = time, isCurrentTimeAos = false) }
                        }
                        satPass.aosTime > timeNow -> {
                            val time = satPass.aosTime.minus(timeNow).toTimerString()
                            _uiState.update { it.copy(currentTime = time, isCurrentTimeAos = true) }
                        }
                        else -> {
                            val time = satPass.losTime.minus(timeNow).toTimerString()
                            _uiState.update { it.copy(currentTime = time, isCurrentTimeAos = false) }
                        }
                    }
                    processRadios(transmitters, satPass.orbitalObject, timeNow)
                    sendPassData(satPass, pos, satPass.orbitalObject)
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

    private suspend fun sendPassData(orbitalPass: OrbitalPass, orbitalPos: OrbitalPos, orbitalObject: OrbitalObject) {
        var track: List<OrbitalPos> = emptyList()
        if (!orbitalPass.isDeepSpace) {
            track = satelliteRepo.getTrack(
                orbitalObject, stationPos, orbitalPass.aosTime, orbitalPass.losTime
            )
        }
        _uiState.update { it.copy(orbitalPos = orbitalPos, satTrack = track) }
        viewModelScope.launch {
            if (settingsRepo.rcSettings.value.rotatorState) {
                val server = settingsRepo.rcSettings.value.rotatorAddress
                val port = settingsRepo.rcSettings.value.rotatorPort.toInt()
                val format = settingsRepo.rcSettings.value.rotatorFormat
                val azimuth = orbitalPos.azimuth.toDegrees().round(2)
                val elevation = orbitalPos.elevation.toDegrees().round(2)
                networkReporter.reportRotation(format, azimuth, elevation, ExtendedParams(server, port))
            }
            if (settingsRepo.rcSettings.value.frequencyState) {
                _uiState.value.selectedFrequency?.let { freq ->
                    val server = settingsRepo.rcSettings.value.frequencyAddress
                    val port = settingsRepo.rcSettings.value.frequencyPort.toInt()
                    val format = settingsRepo.rcSettings.value.frequencyFormat
                    networkReporter.reportFrequency(format,freq, ExtendedParams(server, port))
                }
            }
        }
    }

    private fun sendPassDataBT(orbitalPos: OrbitalPos) {
        viewModelScope.launch {
            if (settingsRepo.rcSettings.value.bluetoothRotatorState) {
                val btRotatorDevice = settingsRepo.rcSettings.value.bluetoothRotatorAddress
                if (bluetoothReporter.isRotationConnected()) {
                    val format = settingsRepo.rcSettings.value.bluetoothRotatorFormat
                    val azimuth = orbitalPos.azimuth.toDegrees().round(0)
                    val elevation = orbitalPos.elevation.toDegrees().round(0)
                    bluetoothReporter.reportRotation(format, azimuth, elevation, WithoutExtParams(0))
                } else if (!bluetoothReporter.isRotationConnecting()) {
//                    Log.i("BTReporter", "BTReporter (rotator): Attempting to connect...")
                    bluetoothReporter.connectBTRotatorDevice(btRotatorDevice)
                }
            }
            if (settingsRepo.rcSettings.value.bluetoothFrequencyState) {
                val btFrequencyDevice = settingsRepo.rcSettings.value.bluetoothFrequencyAddress
                if (bluetoothReporter.isFrequencyConnected()) {
                    val format = settingsRepo.rcSettings.value.bluetoothFrequencyFormat
                    val frequency = _uiState.value.selectedFrequency
                    frequency?.let { freq ->
                        bluetoothReporter.reportFrequency(format, freq, WithoutExtParams(0))
                    }
                } else if (!bluetoothReporter.isFrequencyConnecting()) {
//                    Log.i("BTReporter", "BTReporter (frequency): Attempting to connect...")
                    bluetoothReporter.connectBTFrequencyDevice(btFrequencyDevice)
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
                val container = (this[applicationKey] as MainApplication).container
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
