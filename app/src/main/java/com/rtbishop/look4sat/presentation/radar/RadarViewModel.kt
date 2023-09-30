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
import com.rtbishop.look4sat.data.framework.BluetoothReporter
import com.rtbishop.look4sat.data.framework.NetworkReporter
import com.rtbishop.look4sat.domain.model.SatRadio
import com.rtbishop.look4sat.domain.predict.GeoPos
import com.rtbishop.look4sat.domain.predict.OrbitalObject
import com.rtbishop.look4sat.domain.predict.OrbitalPass
import com.rtbishop.look4sat.domain.predict.OrbitalPos
import com.rtbishop.look4sat.domain.repository.ISatelliteRepo
import com.rtbishop.look4sat.domain.repository.ISensorsRepo
import com.rtbishop.look4sat.domain.repository.ISettingsRepo
import com.rtbishop.look4sat.domain.utility.round
import com.rtbishop.look4sat.domain.utility.toDegrees
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RadarViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val bluetoothReporter: BluetoothReporter,
    private val networkReporter: NetworkReporter,
    private val satelliteRepo: ISatelliteRepo,
    private val settingsRepo: ISettingsRepo,
    private val sensorsRepo: ISensorsRepo
) : ViewModel() {

    private val stationPos = settingsRepo.stationPosition.value
    private val magDeclination = getMagDeclination(stationPos)
    val transmitters = mutableStateOf<List<SatRadio>>(emptyList())
    val radarData = mutableStateOf<RadarData?>(null)
    val orientation = mutableStateOf(sensorsRepo.orientation.value)

    init {
        if (settingsRepo.otherSettings.value.stateOfSensors) {
            viewModelScope.launch {
                sensorsRepo.enableSensor()
                sensorsRepo.orientation.collect { data ->
                    orientation.value = Pair(data.first + magDeclination, data.second)
                }
            }
        }
    }

    override fun onCleared() {
        sensorsRepo.disableSensor()
        super.onCleared()
    }

    fun getPass() = flow {
        val catNum = savedStateHandle.get<Int>("catNum") ?: 0
        val aosTime = savedStateHandle.get<Long>("aosTime") ?: 0L
        val passes = satelliteRepo.passes.value
        val pass = passes.find { pass -> pass.catNum == catNum && pass.aosTime == aosTime }
        val currentPass = pass ?: passes.firstOrNull()
        currentPass?.let { satPass ->
            emit(satPass)
            val transmitters = satelliteRepo.getRadiosWithId(satPass.catNum)
            viewModelScope.launch {
                while (isActive) {
                    val timeNow = System.currentTimeMillis()
                    val satPos =
                        satelliteRepo.getPosition(satPass.orbitalObject, stationPos, timeNow)
                    sendPassData(satPass, satPos, satPass.orbitalObject)
                    sendPassDataBT(satPos)
                    processRadios(transmitters, satPass.orbitalObject, timeNow)
                    delay(1000)
                }
            }
        }
    }

//    fun getUseCompass(): Boolean = settingsRepo.otherSettings.value.sensorState
//
//    fun getShowSweep(): Boolean = settingsRepo.otherSettings.value.sweepState

    private fun getMagDeclination(geoPos: GeoPos, time: Long = System.currentTimeMillis()): Float {
        val latitude = geoPos.latitude.toFloat()
        val longitude = geoPos.longitude.toFloat()
        return GeomagneticField(latitude, longitude, geoPos.altitude.toFloat(), time).declination
    }

    private fun sendPassData(orbitalPass: OrbitalPass, orbitalPos: OrbitalPos, orbitalObject: OrbitalObject) {
        viewModelScope.launch {
            var track: List<OrbitalPos> = emptyList()
            if (!orbitalPass.isDeepSpace) {
                track = satelliteRepo.getTrack(
                    orbitalObject, stationPos, orbitalPass.aosTime, orbitalPass.losTime
                )
            }
            if (settingsRepo.getRotatorState()) {
                val server = settingsRepo.getRotatorAddress()
                val port = settingsRepo.getRotatorPort().toInt()
                val azimuth = orbitalPos.azimuth.toDegrees().round(1)
                val elevation = orbitalPos.elevation.toDegrees().round(1)
                networkReporter.reportRotation(server, port, azimuth, elevation)
            }
            radarData.value = RadarData(orbitalPass, orbitalPos, track)
        }
    }

    private fun sendPassDataBT(orbitalPos: OrbitalPos) {
        viewModelScope.launch {
            if (settingsRepo.getBluetoothState()) {
                val btDevice = settingsRepo.getBluetoothAddress()
                if (bluetoothReporter.isConnected()) {
                    val format = settingsRepo.getBluetoothFormat()
                    val azimuth = orbitalPos.azimuth.toDegrees().round(0).toInt()
                    val elevation = orbitalPos.elevation.toDegrees().round(0).toInt()
                    bluetoothReporter.reportRotation(format, azimuth, elevation)
                } else if (!bluetoothReporter.isConnecting()) {
                    Log.i("BTReporter", "BTReporter: Attempting to connect...")
                    bluetoothReporter.connectBTDevice(btDevice)
                }
            }
        }
    }

    private fun processRadios(radios: List<SatRadio>, orbitalObject: OrbitalObject, time: Long) {
        viewModelScope.launch {
            delay(125)
            val list = satelliteRepo.getRadios(orbitalObject, stationPos, radios, time)
            transmitters.value = list
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
                    container.provideSensorsRepo()
                )
            }
        }
    }
}
