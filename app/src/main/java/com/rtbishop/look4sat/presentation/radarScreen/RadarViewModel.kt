/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2021 Arty Bishop (bishop.arty@gmail.com)
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

import androidx.lifecycle.*
import com.rtbishop.look4sat.domain.DataReporter
import com.rtbishop.look4sat.domain.Repository
import com.rtbishop.look4sat.predict4kotlin.Predictor
import com.rtbishop.look4sat.predict4kotlin.SatPass
import com.rtbishop.look4sat.predict4kotlin.SatPos
import com.rtbishop.look4sat.model.Transmitter
import com.rtbishop.look4sat.framework.OrientationSource
import com.rtbishop.look4sat.framework.PreferencesSource
import com.rtbishop.look4sat.utility.round
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class RadarViewModel @Inject constructor(
    private val orientationSource: OrientationSource,
    private val preferences: PreferencesSource,
    private val predictor: Predictor,
    private val satelliteRepo: Repository,
    private val dataReporter: DataReporter
) : ViewModel(), OrientationSource.OrientationListener {

    private val stationPos = preferences.loadStationPosition()
    private val _passData = MutableLiveData<RadarData>()
    private val _transmitters = MutableLiveData<List<Transmitter>>()
    private val _orientation = MutableLiveData<Triple<Float, Float, Float>>()
    val radarData: LiveData<RadarData> = _passData
    val transmitters: LiveData<List<Transmitter>> = _transmitters
    val orientation: LiveData<Triple<Float, Float, Float>> = _orientation

    fun getPass(catNum: Int, aosTime: Long) = liveData {
        predictor.passes.collect { passes ->
            val pass = passes.find { it.catNum == catNum && it.aosTime == aosTime }
            pass?.let { satPass ->
                emit(satPass)
                sendPassData(satPass)
                processTransmitters(satPass)
            }
        }
    }

    fun enableSensor() {
        if (preferences.shouldUseCompass()) orientationSource.startListening(this)
    }

    fun disableSensor() {
        if (preferences.shouldUseCompass()) orientationSource.stopListening()
    }

    override fun onOrientationChanged(azimuth: Float, pitch: Float, roll: Float) {
        _orientation.value = Triple(azimuth + preferences.getMagDeclination(), pitch, roll)
    }

    private fun sendPassData(satPass: SatPass) {
        viewModelScope.launch {
            var satTrack: List<SatPos> = emptyList()
            if (!satPass.isDeepSpace) {
                val startDate = Date(satPass.aosTime)
                val endDate = Date(satPass.losTime)
                satTrack = predictor.getSatTrack(satPass.satellite, stationPos, startDate, endDate)
            }
            while (isActive) {
                val satPos = predictor.getSatPos(satPass.satellite, stationPos, Date())
                if (preferences.isRotatorEnabled()) {
                    val server = preferences.getRotatorServer().first
                    val port = preferences.getRotatorServer().second
                    val azimuth = Math.toDegrees(satPos.azimuth).round(1)
                    val elevation = Math.toDegrees(satPos.elevation).round(1)
                    dataReporter.reportRotation(server, port, azimuth, elevation)
                }
                _passData.postValue(RadarData(satPos, satTrack))
                delay(1000)
            }
        }
    }

    private fun processTransmitters(satPass: SatPass) {
        viewModelScope.launch {
            satelliteRepo.getSatTransmitters(satPass.catNum).collect { transList ->
                while (isActive) {
                    val satPos = predictor.getSatPos(satPass.satellite, stationPos, Date())
                    val copiedList = transList.map { it.copy() }
                    copiedList.forEach { transmitter ->
                        transmitter.downlink?.let {
                            transmitter.downlink = satPos.getDownlinkFreq(it)
                        }
                        transmitter.uplink?.let {
                            transmitter.uplink = satPos.getUplinkFreq(it)
                        }
                    }
                    _transmitters.postValue(copiedList.map { it.copy() })
                    delay(1000)
                }
            }
        }
    }
}
