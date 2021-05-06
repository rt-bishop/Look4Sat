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
package com.rtbishop.look4sat.presentation.mapScreen

import androidx.lifecycle.*
import com.rtbishop.look4sat.data.PreferenceSource
import com.rtbishop.look4sat.di.DefaultDispatcher
import com.rtbishop.look4sat.domain.predict4kotlin.Position
import com.rtbishop.look4sat.domain.predict4kotlin.Satellite
import com.rtbishop.look4sat.domain.predict4kotlin.StationPosition
import com.rtbishop.look4sat.framework.model.SatData
import com.rtbishop.look4sat.interactors.GetSelectedSatellites
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

@HiltViewModel
class MapViewModel @Inject constructor(
    private val getSelectedSatellites: GetSelectedSatellites,
    private val preferenceSource: PreferenceSource,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val gsp = preferenceSource.loadStationPosition()
    private var dataUpdateJob: Job? = null
    private var allSatList = listOf<Satellite>()
    private lateinit var selectedSat: Satellite

    val stationPos = liveData {
        val osmLat = getOsmLat(gsp.latitude)
        val osmLon = getOsmLon(gsp.longitude)
        emit(Position(osmLat, osmLon))
    }

    private val _satTrack = MutableLiveData<List<List<Position>>>()
    val satTrack: LiveData<List<List<Position>>> = _satTrack

    private val _satFootprint = MutableLiveData<List<Position>>()
    val satFootprint: LiveData<List<Position>> = _satFootprint

    private val _satData = MutableLiveData<SatData>()
    val satData: LiveData<SatData> = this._satData

    private val _allSatPositions = MutableLiveData<Map<Satellite, Position>>()
    val allSatPositions: LiveData<Map<Satellite, Position>> = _allSatPositions

    init {
        viewModelScope.launch {
            getSelectedSatellites().also { selectedSatellites ->
                if (selectedSatellites.isNotEmpty()) {
                    allSatList = selectedSatellites
                    selectSatellite(selectedSatellites.first())
                }
            }
        }
    }

    fun shouldUseTextLabels(): Boolean {
        return preferenceSource.shouldUseTextLabels()
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
                setSelectedSatTrack(selectedSat, gsp, dateNow)
                while (isActive) {
                    dateNow.time = System.currentTimeMillis()
                    setSatPositions(allSatList, gsp, dateNow)
                    setSelectedSatFootprint(selectedSat, gsp, dateNow)
                    setSelectedSatData(selectedSat, gsp, dateNow)
                    delay(updateFreq)
                }
            }
        }
    }

    private suspend fun setSatPositions(list: List<Satellite>, gsp: StationPosition, date: Date) {
        withContext(defaultDispatcher) {
            val satPositions = mutableMapOf<Satellite, Position>()
            list.forEach { satellite ->
                val satPos = satellite.getPredictor(gsp).getSatPos(date)
                val osmLat = getOsmLat(Math.toDegrees(satPos.latitude))
                val osmLon = getOsmLon(Math.toDegrees(satPos.longitude))
                satPositions[satellite] = Position(osmLat, osmLon)
            }
            _allSatPositions.postValue(satPositions)
        }
    }

    private suspend fun setSelectedSatTrack(sat: Satellite, gsp: StationPosition, date: Date) {
        withContext(defaultDispatcher) {
            val satTracks = mutableListOf<List<Position>>()
            val currentTrack = mutableListOf<Position>()
            var oldLongitude = 0.0
            sat.getPredictor(gsp).getPositions(date, 15, 0, 2.4).forEach { satPos ->
                val osmLat = getOsmLat(Math.toDegrees(satPos.latitude))
                val osmLon = getOsmLon(Math.toDegrees(satPos.longitude))
                val currentPosition = Position(osmLat, osmLon)
                if (oldLongitude < -170.0 && currentPosition.longitude > 170.0) {
                    // adding left terminal position
                    currentTrack.add(Position(osmLat, -180.0))
                    val finishedTrack = mutableListOf<Position>().apply { addAll(currentTrack) }
                    satTracks.add(finishedTrack)
                    currentTrack.clear()
                } else if (oldLongitude > 170.0 && currentPosition.longitude < -170.0) {
                    // adding right terminal position
                    currentTrack.add(Position(osmLat, 180.0))
                    val finishedTrack = mutableListOf<Position>().apply { addAll(currentTrack) }
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

    private suspend fun setSelectedSatFootprint(sat: Satellite, gsp: StationPosition, date: Date) {
        withContext(defaultDispatcher) {
            val satFootprint = sat.getPosition(gsp, date).getRangeCircle().map { rangePos ->
                val osmLat = getOsmLat(rangePos.latitude)
                val osmLon = getOsmLon(rangePos.longitude)
                Position(osmLat, osmLon)
            }
            _satFootprint.postValue(satFootprint)
        }
    }

    private suspend fun setSelectedSatData(sat: Satellite, gsp: StationPosition, date: Date) {
        withContext(defaultDispatcher) {
            val satPos = sat.getPredictor(gsp).getSatPos(date)
            val osmLat = getOsmLat(Math.toDegrees(satPos.latitude))
            val osmLon = getOsmLon(Math.toDegrees(satPos.longitude))
            val osmPos = Position(osmLat, osmLon)
            val qthLoc =
                preferenceSource.positionToQTH(osmPos.latitude, osmPos.longitude) ?: "-- --"
            val velocity = getOrbitalVelocity(satPos.altitude)
            val satData = SatData(
                sat, sat.tle.catnum, sat.tle.name, satPos.range,
                satPos.altitude, velocity, qthLoc, osmPos
            )
            this@MapViewModel._satData.postValue(satData)
        }
    }

    private fun getOsmLat(latitude: Double): Double {
        return min(max(latitude, -85.0), 85.0)
    }

    private fun getOsmLon(longitude: Double): Double {
        val newLongitude = when {
            longitude < -180.0 -> longitude + 360.0
            longitude > 180.0 -> longitude - 360.0
            else -> longitude
        }
        return min(max(newLongitude, -180.0), 180.0)
    }

    private fun getOrbitalVelocity(altitude: Double): Double {
        val earthG = 6.674 * 10.0.pow(-11)
        val earthM = 5.98 * 10.0.pow(24)
        val radius = 6.37 * 10.0.pow(6) + altitude * 10.0.pow(3)
        return sqrt(earthG * earthM / radius) / 1000
    }
}
