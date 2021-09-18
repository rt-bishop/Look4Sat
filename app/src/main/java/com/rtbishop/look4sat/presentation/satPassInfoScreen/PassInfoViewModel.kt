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
package com.rtbishop.look4sat.presentation.satPassInfoScreen

import androidx.lifecycle.*
import androidx.navigation.fragment.findNavController
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.data.PreferencesSource
import com.rtbishop.look4sat.data.SatelliteRepo
import com.rtbishop.look4sat.domain.Predictor
import com.rtbishop.look4sat.domain.SatPass
import com.rtbishop.look4sat.domain.SatPos
import com.rtbishop.look4sat.domain.Transmitter
import com.rtbishop.look4sat.framework.OrientationProvider
import com.rtbishop.look4sat.injection.IoDispatcher
import com.rtbishop.look4sat.utility.navigateSafe
import com.rtbishop.look4sat.utility.round
import com.rtbishop.look4sat.utility.toTimerString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import java.net.Socket
import java.util.*
import javax.inject.Inject

@HiltViewModel
class PassInfoViewModel @Inject constructor(
    private val orientationProvider: OrientationProvider,
    private val preferences: PreferencesSource,
    private val predictor: Predictor,
    private val satelliteRepo: SatelliteRepo,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel(), OrientationProvider.OrientationListener {

    private val stationPos = preferences.loadStationPosition()
    private val _passData = MutableLiveData<PassData>()
    private val _transmitters = MutableLiveData<List<Transmitter>>()
    private val _orientation = MutableLiveData<Triple<Float, Float, Float>>()
    val passData: LiveData<PassData> = _passData
    val transmitters: LiveData<List<Transmitter>> = _transmitters
    val orientation: LiveData<Triple<Float, Float, Float>> = _orientation

    fun getPass(catNum: Int, aosTime: Long) = liveData {
        predictor.passes.collect { passes ->
            val pass = passes.find { it.catNum == catNum && it.aosTime == aosTime }
            pass?.let { satPass ->
                emit(satPass)
                sendPassData(satPass)
                processTransmitters(satPass)
                initRotatorControl(satPass)
            }
        }
    }

    fun enableSensor() {
        if (preferences.shouldUseCompass()) orientationProvider.startListening(this)
    }

    fun disableSensor() {
        if (preferences.shouldUseCompass()) orientationProvider.stopListening()
    }

    override fun onOrientationChanged(azimuth: Float, pitch: Float, roll: Float) {
        _orientation.value = Triple(azimuth + preferences.getMagDeclination(), pitch, roll)
    }

    private fun sendPassData(satPass: SatPass) {
        viewModelScope.launch {
            var track: List<SatPos> = emptyList()
            if (!satPass.isDeepSpace) {
                val startDate = Date(satPass.aosTime)
                val endDate = Date(satPass.losTime)
                track = predictor.getSatTrack(satPass.satellite, stationPos, startDate, endDate)
            }
            while (isActive) {
                val pos = predictor.getSatPos(satPass.satellite, stationPos, Date())
                _passData.postValue(PassData(pos, track))
                delay(1000)
            }
        }
    }

    private fun initRotatorControl(satPass: SatPass) {
        viewModelScope.launch {
            val rotatorPrefs = preferences.getRotatorServer()
            if (rotatorPrefs != null) {
                runCatching {
                    withContext(ioDispatcher) {
                        val socket = Socket(rotatorPrefs.first, rotatorPrefs.second)
                        val writer = socket.getOutputStream().bufferedWriter()
                        while (isActive) {
                            val satPos = predictor.getSatPos(satPass.satellite, stationPos, Date())
                            val azimuth = Math.toDegrees(satPos.azimuth).round(1)
                            val elevation = Math.toDegrees(satPos.elevation).round(1)
                            writer.write("\\set_pos $azimuth $elevation")
                            writer.newLine()
                            writer.flush()
                            delay(1000)
                        }
                        writer.close()
                    }
                }.onFailure { Timber.d(it) }
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
