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
package com.rtbishop.look4sat.presentation.satMapScreen

import androidx.lifecycle.*
import com.rtbishop.look4sat.data.PreferencesSource
import com.rtbishop.look4sat.data.SatelliteRepo
import com.rtbishop.look4sat.data.PassPredictor
import com.rtbishop.look4sat.domain.GeoPos
import com.rtbishop.look4sat.domain.Satellite
import com.rtbishop.look4sat.domain.StationPos
import com.rtbishop.look4sat.domain.SatData
import com.rtbishop.look4sat.injection.DefaultDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import org.osmdroid.views.MapView
import java.util.*
import javax.inject.Inject
import kotlin.math.pow
import kotlin.math.sqrt

@HiltViewModel
class SatMapViewModel @Inject constructor(
    private val satelliteRepo: SatelliteRepo,
    private val passPredictor: PassPredictor,
    private val preferences: PreferencesSource,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val stationPos = preferences.loadStationPosition()
    private var dataUpdateJob: Job? = null
    private var allSatList = listOf<Satellite>()
    private lateinit var selectedSat: Satellite

    val stationPosLiveData = liveData {
        val osmLat = clipLat(stationPos.latitude)
        val osmLon = clipLon(stationPos.longitude)
        emit(GeoPos(osmLat, osmLon))
    }

    private val _satTrack = MutableLiveData<List<List<GeoPos>>>()
    val satTrack: LiveData<List<List<GeoPos>>> = _satTrack

    private val _satFootprint = MutableLiveData<List<GeoPos>>()
    val satFootprint: LiveData<List<GeoPos>> = _satFootprint

    private val _satData = MutableLiveData<SatData>()
    val satData: LiveData<SatData> = this._satData

    private val _satPositions = MutableLiveData<Map<Satellite, GeoPos>>()
    val satPositions: LiveData<Map<Satellite, GeoPos>> = _satPositions

    init {
        viewModelScope.launch {
            satelliteRepo.getSelectedSatellites().also { selectedSatellites ->
                if (selectedSatellites.isNotEmpty()) {
                    allSatList = selectedSatellites
                    selectSatellite(selectedSatellites.first())
                }
            }
        }
    }

    fun shouldUseTextLabels(): Boolean {
        return preferences.shouldUseTextLabels()
    }

    fun scrollSelection(decrement: Boolean) {
        if (allSatList.isNotEmpty()) {
            val index = allSatList.indexOf(selectedSat)
            if (decrement) {
                if (index > 0) selectSatellite(allSatList[index - 1])
                else selectSatellite(allSatList[allSatList.size - 1])
            } else {
                if (index < allSatList.size - 1) selectSatellite(allSatList[index + 1])
                else selectSatellite(allSatList[0])
            }
        }
    }

    fun selectSatellite(satellite: Satellite, updateFreq: Long = 2000) {
        selectedSat = satellite
        viewModelScope.launch {
            dataUpdateJob?.cancelAndJoin()
            dataUpdateJob = launch {
                val dateNow = Date()
                setSelectedSatTrack(selectedSat, stationPos, dateNow)
                while (isActive) {
                    dateNow.time = System.currentTimeMillis()
                    setSatPositions(allSatList, stationPos, dateNow)
                    setSelectedSatFootprint(selectedSat, stationPos, dateNow)
                    setSelectedSatData(selectedSat, stationPos, dateNow)
                    delay(updateFreq)
                }
            }
        }
    }

    private suspend fun setSatPositions(list: List<Satellite>, gsp: StationPos, date: Date) {
        withContext(defaultDispatcher) {
            val satPositions = mutableMapOf<Satellite, GeoPos>()
            list.forEach { satellite ->
                val satPos = satellite.getPosition(gsp, date.time)
                val osmLat = clipLat(Math.toDegrees(satPos.latitude))
                val osmLon = clipLon(Math.toDegrees(satPos.longitude))
                satPositions[satellite] = GeoPos(osmLat, osmLon)
            }
            _satPositions.postValue(satPositions)
        }
    }

    private suspend fun setSelectedSatTrack(sat: Satellite, gsp: StationPos, date: Date) {
        withContext(defaultDispatcher) {
            val satTracks = mutableListOf<List<GeoPos>>()
            val currentTrack = mutableListOf<GeoPos>()
            var oldLongitude = 0.0
            passPredictor.getPositions(sat, gsp, date, 15, 0, 2.4).forEach { satPos ->
                val osmLat = clipLat(Math.toDegrees(satPos.latitude))
                val osmLon = clipLon(Math.toDegrees(satPos.longitude))
                val currentPosition = GeoPos(osmLat, osmLon)
                if (oldLongitude < -170.0 && currentPosition.longitude > 170.0) {
                    // adding left terminal position
                    currentTrack.add(GeoPos(osmLat, -180.0))
                    val finishedTrack = mutableListOf<GeoPos>().apply { addAll(currentTrack) }
                    satTracks.add(finishedTrack)
                    currentTrack.clear()
                } else if (oldLongitude > 170.0 && currentPosition.longitude < -170.0) {
                    // adding right terminal position
                    currentTrack.add(GeoPos(osmLat, 180.0))
                    val finishedTrack = mutableListOf<GeoPos>().apply { addAll(currentTrack) }
                    satTracks.add(finishedTrack)
                    currentTrack.clear()
                }
                oldLongitude = currentPosition.longitude
                currentTrack.add(currentPosition)
            }
            satTracks.add(currentTrack)
            _satTrack.postValue(satTracks)
        }
    }

    private suspend fun setSelectedSatFootprint(sat: Satellite, gsp: StationPos, date: Date) {
        withContext(defaultDispatcher) {
            val satFootprint = sat.getPosition(gsp, date.time).getRangeCircle().map { rangePos ->
                val osmLat = clipLat(rangePos.latitude)
                val osmLon = clipLon(rangePos.longitude)
                GeoPos(osmLat, osmLon)
            }
            _satFootprint.postValue(satFootprint)
        }
    }

    private suspend fun setSelectedSatData(sat: Satellite, gsp: StationPos, date: Date) {
        withContext(defaultDispatcher) {
            val satPos = sat.getPosition(gsp, date.time)
            val osmLat = clipLat(Math.toDegrees(satPos.latitude))
            val osmLon = clipLon(Math.toDegrees(satPos.longitude))
            val osmPos = GeoPos(osmLat, osmLon)
            val qthLoc = preferences.positionToQTH(osmPos.latitude, osmPos.longitude) ?: "-- --"
            val velocity = getOrbitalVelocity(satPos.altitude)
            val satData = SatData(
                sat, sat.params.catnum, sat.params.name, satPos.range,
                satPos.altitude, velocity, qthLoc, osmPos
            )
            this@SatMapViewModel._satData.postValue(satData)
        }
    }

    private fun clipLat(latitude: Double): Double {
        return MapView.getTileSystem().cleanLatitude(latitude)
    }

    private fun clipLon(longitude: Double): Double {
        return MapView.getTileSystem().cleanLongitude(longitude)
    }

    private fun getOrbitalVelocity(altitude: Double): Double {
        val earthG = 6.674 * 10.0.pow(-11)
        val earthM = 5.98 * 10.0.pow(24)
        val radius = 6.37 * 10.0.pow(6) + altitude * 10.0.pow(3)
        return sqrt(earthG * earthM / radius) / 1000
    }
}
