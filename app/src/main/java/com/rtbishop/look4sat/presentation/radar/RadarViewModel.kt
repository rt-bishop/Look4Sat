/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2022 Arty Bishop (bishop.arty@gmail.com)
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

import android.hardware.GeomagneticField
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rtbishop.look4sat.MainApplication
import com.rtbishop.look4sat.domain.IDataRepository
import com.rtbishop.look4sat.domain.ISatelliteRepository
import com.rtbishop.look4sat.domain.ISensorRepository
import com.rtbishop.look4sat.domain.ISettingsRepository
import com.rtbishop.look4sat.domain.Satellite
import com.rtbishop.look4sat.framework.BluetoothReporter
import com.rtbishop.look4sat.framework.NetworkReporter
import com.rtbishop.look4sat.model.GeoPos
import com.rtbishop.look4sat.model.SatPass
import com.rtbishop.look4sat.model.SatPos
import com.rtbishop.look4sat.model.SatRadio
import com.rtbishop.look4sat.utility.round
import com.rtbishop.look4sat.utility.toDegrees
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RadarViewModel(
    private val bluetoothReporter: BluetoothReporter,
    private val networkReporter: NetworkReporter,
    private val savedStateHandle: SavedStateHandle,
    private val dataRepository: IDataRepository,
    private val satelliteRepository: ISatelliteRepository,
    private val sensorRepository: ISensorRepository,
    private val settingsRepository: ISettingsRepository
) : ViewModel() {

    private val stationPos = settingsRepository.stationPosition.value
    private val magDeclination = getMagDeclination(stationPos)
    val transmitters = mutableStateOf<List<SatRadio>>(emptyList())
    val radarData = mutableStateOf<RadarData?>(null)
    val orientation = mutableStateOf(sensorRepository.orientation.value)

    init {
        if (settingsRepository.isSensorEnabled()) {
            viewModelScope.launch {
                sensorRepository.enableSensor()
                sensorRepository.orientation.collect { data ->
                    orientation.value = Pair(data.first + magDeclination, data.second)
                }
            }
        }
    }

    override fun onCleared() {
        sensorRepository.disableSensor()
        super.onCleared()
    }

    fun getPass() = flow {
        val catNum = savedStateHandle.get<Int>("catNum") ?: 0
        val aosTime = savedStateHandle.get<Long>("aosTime") ?: 0L
        satelliteRepository.calculatedPasses.collect { passes ->
            val pass = passes.find { pass -> pass.catNum == catNum && pass.aosTime == aosTime }
            val currentPass = pass ?: passes.firstOrNull()
            currentPass?.let { satPass ->
                emit(satPass)
                val transmitters = dataRepository.getRadiosWithId(satPass.catNum)
                viewModelScope.launch {
                    while (isActive) {
                        val timeNow = System.currentTimeMillis()
                        val satPos =
                            satelliteRepository.getPosition(satPass.satellite, stationPos, timeNow)
                        sendPassData(satPass, satPos, satPass.satellite)
                        sendPassDataBT(satPos)
                        processRadios(transmitters, satPass.satellite, timeNow)
                        delay(1000)
                    }
                }
            }
        }
    }

    fun getUseCompass(): Boolean = settingsRepository.isSensorEnabled()

    fun getShowSweep(): Boolean = settingsRepository.isSweepEnabled()

    private fun getMagDeclination(geoPos: GeoPos, time: Long = System.currentTimeMillis()): Float {
        val latitude = geoPos.latitude.toFloat()
        val longitude = geoPos.longitude.toFloat()
        return GeomagneticField(latitude, longitude, 0f, time).declination
    }

    private fun sendPassData(satPass: SatPass, satPos: SatPos, satellite: Satellite) {
        viewModelScope.launch {
            var track: List<SatPos> = emptyList()
            if (!satPass.isDeepSpace) {
                track = satelliteRepository.getTrack(
                    satellite, stationPos, satPass.aosTime, satPass.losTime
                )
            }
            if (settingsRepository.getRotatorEnabled()) {
                val server = settingsRepository.getRotatorServer()
                val port = settingsRepository.getRotatorPort().toInt()
                val azimuth = satPos.azimuth.toDegrees().round(1)
                val elevation = satPos.elevation.toDegrees().round(1)
                networkReporter.reportRotation(server, port, azimuth, elevation)
            }
            radarData.value = RadarData(satPos, track)
        }
    }

    private fun sendPassDataBT(satPos: SatPos) {
        viewModelScope.launch {
            if (settingsRepository.getBTEnabled()) {
                val btDevice = settingsRepository.getBTDeviceAddr()
                if (bluetoothReporter.isConnected()) {
                    val format = settingsRepository.getBTFormat()
                    val azimuth = satPos.azimuth.toDegrees().round(0).toInt()
                    val elevation = satPos.elevation.toDegrees().round(0).toInt()
                    bluetoothReporter.reportRotation(format, azimuth, elevation)
                } else if (!bluetoothReporter.isConnecting()) {
                    Log.i("BTReporter", "BTReporter: Attempting to connect...")
                    bluetoothReporter.connectBTDevice(btDevice)
                }
            }
        }
    }

    private fun processRadios(radios: List<SatRadio>, satellite: Satellite, time: Long) {
        viewModelScope.launch {
            delay(125)
            val list = satelliteRepository.processRadios(satellite, stationPos, radios, time)
            transmitters.value = list
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            val applicationKey = ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY
            initializer {
                val savedStateHandle = createSavedStateHandle()
                val container = (this[applicationKey] as MainApplication).container
                RadarViewModel(
                    container.bluetoothReporter,
                    container.networkReporter,
                    savedStateHandle,
                    container.dataRepository,
                    container.satelliteRepository,
                    container.sensorRepository,
                    container.settingsRepository
                )
            }
        }
    }
}
