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
package com.rtbishop.look4sat.presentation.radarScreen

import android.hardware.GeomagneticField
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.rtbishop.look4sat.domain.IDataRepository
import com.rtbishop.look4sat.domain.ISatelliteManager
import com.rtbishop.look4sat.domain.ISettingsManager
import com.rtbishop.look4sat.domain.model.SatRadio
import com.rtbishop.look4sat.domain.predict.GeoPos
import com.rtbishop.look4sat.domain.predict.SatPass
import com.rtbishop.look4sat.domain.predict.SatPos
import com.rtbishop.look4sat.domain.predict.Satellite
import com.rtbishop.look4sat.framework.OrientationManager
import com.rtbishop.look4sat.utility.DataReporter
import com.rtbishop.look4sat.utility.round
import com.rtbishop.look4sat.utility.toDegrees
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel
class RadarViewModel @Inject constructor(
    private val orientationManager: OrientationManager,
    private val reporter: DataReporter,
    private val btReporter: BTReporter,
    private val satManager: ISatelliteManager,
    private val repository: IDataRepository,
    private val settings: ISettingsManager
) : ViewModel(), OrientationManager.OrientationListener {

    private val stationPos = settings.loadStationPosition()
    private val _passData = MutableLiveData<RadarData>()
    private val _transmitters = MutableLiveData<List<SatRadio>>()
    private val _orientation = MutableLiveData<Triple<Float, Float, Float>>()
    val radarData: LiveData<RadarData> = _passData
    val transmitters: LiveData<List<SatRadio>> = _transmitters
    val orientation: LiveData<Triple<Float, Float, Float>> = _orientation

    fun getPass(catNum: Int, aosTime: Long) = liveData {
        satManager.calculatedPasses.collect { passes ->
            val pass = passes.find { pass -> pass.catNum == catNum && pass.aosTime == aosTime }
            pass?.let { satPass ->
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

    fun enableSensor() {
        if (settings.getUseCompass()) orientationManager.startListening(this)
    }

    fun disableSensor() {
        if (settings.getUseCompass()) orientationManager.stopListening()
    }

    fun getUseCompass(): Boolean = settings.getUseCompass()

    fun getShowSweep(): Boolean = settings.getShowSweep()

    override fun onOrientationChanged(azimuth: Float, pitch: Float, roll: Float) {
        _orientation.value = Triple(azimuth + getMagDeclination(stationPos), pitch, roll)
    }

    private fun getMagDeclination(geoPos: GeoPos, time: Long = System.currentTimeMillis()): Float {
        val latitude = geoPos.lat.toFloat()
        val longitude = geoPos.lon.toFloat()
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
                val azimuth = satPos.azimuth.toDegrees().round(2)
                val elevation = satPos.elevation.toDegrees().round(2)
                reporter.reportRotation(server, port, azimuth, elevation)
            }
            _passData.postValue(RadarData(satPos, track))
        }
    }

    private fun sendPassDataBT(satPos: SatPos) {
        viewModelScope.launch {
            if (settings.getBTEnabled()) {
                val btDevice = settings.getBTDeviceAddr()
                if (btReporter.isConnected()) {
                    val format = settings.getBTFormat()
                    val azimuth = satPos.azimuth.toDegrees().round(0).toInt()
                    val elevation = satPos.elevation.toDegrees().round(0).toInt()
                    btReporter.reportRotation(format, azimuth, elevation)
                } else if (!btReporter.isConnecting()) {
                    Log.i("BTReporter", "BTReporter: Attempting to connect...")
                    btReporter.connectBTDevice(btDevice)
                }
            }
        }
    }

    private fun processRadios(radios: List<SatRadio>, satellite: Satellite, time: Long) {
        viewModelScope.launch {
            delay(125)
            val list = satManager.processRadios(satellite, stationPos, radios, time)
            _transmitters.postValue(list)
        }
    }
}
