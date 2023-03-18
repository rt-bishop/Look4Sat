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
import androidx.lifecycle.viewModelScope
import com.rtbishop.look4sat.domain.*
import com.rtbishop.look4sat.framework.BluetoothReporter
import com.rtbishop.look4sat.framework.NetworkReporter
import com.rtbishop.look4sat.framework.OrientationSource
import com.rtbishop.look4sat.model.GeoPos
import com.rtbishop.look4sat.model.SatPass
import com.rtbishop.look4sat.model.SatPos
import com.rtbishop.look4sat.model.SatRadio
import com.rtbishop.look4sat.utility.round
import com.rtbishop.look4sat.utility.toDegrees
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RadarViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val orientationSource: OrientationSource,
    private val reporter: NetworkReporter,
    private val bluetoothReporter: BluetoothReporter,
    private val satManager: ISatelliteManager,
    private val repository: IDataRepository,
    private val settings: ISettingsSource,
    private val locationSource: ILocationSource
) : ViewModel() {

    private val stationPos = locationSource.stationPosition.value
    private val magDeclination = getMagDeclination(stationPos)
    val transmitters = mutableStateOf<List<SatRadio>>(emptyList())
    val radarData = mutableStateOf<RadarData?>(null)
    val orientation = mutableStateOf(orientationSource.orientation.value)

    init {
        if (settings.isSensorEnabled()) {
            viewModelScope.launch {
                orientationSource.startListening()
                orientationSource.orientation.collect { data ->
                    orientation.value = Pair(data.first + magDeclination, data.second)
                }
            }
        }
    }

    override fun onCleared() {
        orientationSource.stopListening()
        super.onCleared()
    }

    fun getPass() = flow {
        val catNum = savedStateHandle.get<Int>("catNum") ?: 0
        val aosTime = savedStateHandle.get<Long>("aosTime") ?: 0L
        satManager.calculatedPasses.collect { passes ->
            val pass = passes.find { pass -> pass.catNum == catNum && pass.aosTime == aosTime }
            val currentPass = pass ?: passes.firstOrNull()
            currentPass?.let { satPass ->
                emit(satPass)
                val transmitters = repository.getRadiosWithId(satPass.catNum)
                viewModelScope.launch {
                    while (isActive) {
                        val timeNow = System.currentTimeMillis()
                        val satPos = satManager.getPosition(satPass.satellite, stationPos, timeNow)
                        sendPassData(satPass, satPos, satPass.satellite)
                        sendPassDataBT(satPos)
                        processRadios(transmitters, satPass.satellite, timeNow)
                        delay(1000)
                    }
                }
            }
        }
    }

    fun getUseCompass(): Boolean = settings.isSensorEnabled()

    fun getShowSweep(): Boolean = settings.isSweepEnabled()

    private fun getMagDeclination(geoPos: GeoPos, time: Long = System.currentTimeMillis()): Float {
        val latitude = geoPos.latitude.toFloat()
        val longitude = geoPos.longitude.toFloat()
        return GeomagneticField(latitude, longitude, 0f, time).declination
    }

    private fun sendPassData(satPass: SatPass, satPos: SatPos, satellite: Satellite) {
        viewModelScope.launch {
            var track: List<SatPos> = emptyList()
            if (!satPass.isDeepSpace) {
                track = satManager.getTrack(satellite, stationPos, satPass.aosTime, satPass.losTime)
            }
            if (settings.getRotatorEnabled()) {
                val server = settings.getRotatorServer()
                val port = settings.getRotatorPort().toInt()
                val azimuth = satPos.azimuth.toDegrees().round(1)
                val elevation = satPos.elevation.toDegrees().round(1)
                reporter.reportRotation(server, port, azimuth, elevation)
            }
            radarData.value = RadarData(satPos, track)
        }
    }

    private fun sendPassDataBT(satPos: SatPos) {
        viewModelScope.launch {
            if (settings.getBTEnabled()) {
                val btDevice = settings.getBTDeviceAddr()
                if (bluetoothReporter.isConnected()) {
                    val format = settings.getBTFormat()
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
            val list = satManager.processRadios(satellite, stationPos, radios, time)
            transmitters.value = list
        }
    }
}
