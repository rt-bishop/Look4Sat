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
import androidx.lifecycle.*
import com.rtbishop.look4sat.domain.DataReporter
import com.rtbishop.look4sat.domain.IRepository
import com.rtbishop.look4sat.domain.ISettings
import com.rtbishop.look4sat.domain.model.SatRadio
import com.rtbishop.look4sat.domain.predict.GeoPos
import com.rtbishop.look4sat.domain.predict.Predictor
import com.rtbishop.look4sat.domain.predict.SatPass
import com.rtbishop.look4sat.domain.predict.SatPos
import com.rtbishop.look4sat.domain.round
import com.rtbishop.look4sat.framework.OrientationHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class RadarViewModel @Inject constructor(
    private val orientationHandler: OrientationHandler,
    private val reporter: DataReporter,
    private val predictor: Predictor,
    private val repository: IRepository,
    private val settings: ISettings
) : ViewModel(), OrientationHandler.OrientationListener {

    private val stationPos = settings.loadStationPosition()
    private val _passData = MutableLiveData<RadarData>()
    private val _transmitters = MutableLiveData<List<SatRadio>>()
    private val _orientation = MutableLiveData<Triple<Float, Float, Float>>()
    val radarData: LiveData<RadarData> = _passData
    val transmitters: LiveData<List<SatRadio>> = _transmitters
    val orientation: LiveData<Triple<Float, Float, Float>> = _orientation

    fun getPass(catNum: Int, aosTime: Long) = liveData {
        predictor.calculatedPasses.collect { passes ->
            val pass = passes.find { pass -> pass.catNum == catNum && pass.aosTime == aosTime }
            pass?.let { satPass ->
                emit(satPass)
                sendPassData(satPass)
                processTransmitters(satPass)
            }
        }
    }

    fun enableSensor() {
        if (settings.getUseCompass()) orientationHandler.startListening(this)
    }

    fun disableSensor() {
        if (settings.getUseCompass()) orientationHandler.stopListening()
    }

    fun getUseCompass(): Boolean = settings.getUseCompass()

    fun getShowSweep(): Boolean = settings.getShowSweep()

    override fun onOrientationChanged(azimuth: Float, pitch: Float, roll: Float) {
        _orientation.value = Triple(azimuth + getMagDeclination(stationPos), pitch, roll)
    }

    private fun getMagDeclination(geoPos: GeoPos, time: Long = System.currentTimeMillis()): Float {
        val latitude = geoPos.latitude.toFloat()
        val longitude = geoPos.longitude.toFloat()
        return GeomagneticField(latitude, longitude, 0f, time).declination
    }

    private fun sendPassData(satPass: SatPass) {
        viewModelScope.launch {
            var satTrack: List<SatPos> = emptyList()
            if (!satPass.isDeepSpace) {
                val startDate = satPass.aosTime
                val endDate = satPass.losTime
                satTrack = predictor.getSatTrack(satPass.satellite, stationPos, startDate, endDate)
            }
            while (isActive) {
                val satPos = predictor.getSatPos(satPass.satellite, stationPos, Date().time)
                if (settings.getRotatorEnabled()) {
                    val server = settings.getRotatorServer()
                    val port = settings.getRotatorPort().toInt()
                    val azimuth = Math.toDegrees(satPos.azimuth).round(1)
                    val elevation = Math.toDegrees(satPos.elevation).round(1)
                    reporter.reportRotation(server, port, azimuth, elevation)
                }
                _passData.postValue(RadarData(satPos, satTrack))
                delay(1000)
            }
        }
    }

    private fun processTransmitters(pass: SatPass) {
        viewModelScope.launch {
            delay(125)
            val transmitters = repository.getRadiosWithId(pass.catNum)
            while (isActive) {
                val time = System.currentTimeMillis()
                val list = predictor.processRadios(pass.satellite, stationPos, transmitters, time)
                _transmitters.postValue(list)
                delay(1000)
            }
        }
    }
}
