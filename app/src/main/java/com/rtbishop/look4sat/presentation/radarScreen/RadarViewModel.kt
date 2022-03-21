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
import androidx.lifecycle.*
import com.rtbishop.look4sat.domain.IDataRepository
import com.rtbishop.look4sat.domain.ISatelliteManager
import com.rtbishop.look4sat.domain.ISettingsManager
import com.rtbishop.look4sat.domain.model.SatRadio
import com.rtbishop.look4sat.domain.predict.GeoPos
import com.rtbishop.look4sat.domain.predict.SatPass
import com.rtbishop.look4sat.domain.predict.SatPos
import com.rtbishop.look4sat.framework.OrientationManager
import com.rtbishop.look4sat.utility.DataReporter
import com.rtbishop.look4sat.utility.round
import com.rtbishop.look4sat.utility.toDegrees
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class RadarViewModel @Inject constructor(
    private val orientationManager: OrientationManager,
    private val reporter: DataReporter,
    private val BTreporter: BTReporter,
    private val satelliteManager: ISatelliteManager,
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
        satelliteManager.calculatedPasses.collect { passes ->
            val pass = passes.find { pass -> pass.catNum == catNum && pass.aosTime == aosTime }
            pass?.let { satPass ->
                emit(satPass)
                sendPassData(satPass)
                sendPassDataBT(satPass)
                processTransmitters(satPass)
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

    private fun sendPassData(satPass: SatPass) {
        viewModelScope.launch {
            var satTrack: List<SatPos> = emptyList()
            if (!satPass.isDeepSpace) {
                val startDate = satPass.aosTime
                val endDate = satPass.losTime
                satTrack = satelliteManager.getTrack(satPass.satellite, stationPos, startDate, endDate)
            }
            while (isActive) {
                val satPos = satelliteManager.getPosition(satPass.satellite, stationPos, Date().time)
                if (settings.getRotatorEnabled()) {
                    val server = settings.getRotatorServer()
                    val port = settings.getRotatorPort().toInt()
                    val azimuth = satPos.azimuth.toDegrees().round(1)
                    val elevation = satPos.elevation.toDegrees().round(1)
                    reporter.reportRotation(server, port, azimuth, elevation)
                }
                _passData.postValue(RadarData(satPos, satTrack))
                delay(1000)
            }
        }
    }

    private fun sendPassDataBT(satPass: SatPass) {
        viewModelScope.launch {
            while (isActive) {
                val satPos = satelliteManager.getPosition(satPass.satellite, stationPos, Date().time)
                if (settings.getBTEnabled()) {
                    val server = settings.getBTDeviceAddr()
                    if(BTreporter.isBTConnected()) {
                        val port = settings.getBTFormat()
                        val azimuth = satPos.azimuth.toDegrees().round(0).toInt()
                        val elevation = satPos.elevation.toDegrees().round(0).toInt()
                        BTreporter.reportRotationBT(server, port, azimuth, elevation)
                    }
                    else if(!BTreporter.connectInProg()) {
                        Log.i("look4satBT", "Attempting to connect...")
                        BTreporter.connectBTDevice(server)
                    }
                }
                delay(2000)
            }
        }
    }

    private fun processTransmitters(pass: SatPass) {
        viewModelScope.launch {
            delay(125)
            val transmitters = repository.getRadiosWithId(pass.catNum)
            while (isActive) {
                val time = System.currentTimeMillis()
                val list = satelliteManager.processRadios(pass.satellite, stationPos, transmitters, time)
                _transmitters.postValue(list)
                delay(1000)
            }
        }
    }
}
