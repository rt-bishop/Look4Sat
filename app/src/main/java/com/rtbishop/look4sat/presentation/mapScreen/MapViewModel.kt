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
import com.rtbishop.look4sat.domain.DataRepository
import com.rtbishop.look4sat.domain.QthConverter
import com.rtbishop.look4sat.domain.predict.GeoPos
import com.rtbishop.look4sat.domain.predict.Predictor
import com.rtbishop.look4sat.domain.predict.Satellite
import com.rtbishop.look4sat.framework.PreferencesSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

@HiltViewModel
class MapViewModel @Inject constructor(
    private val dataRepository: DataRepository,
    private val predictor: Predictor,
    preferences: PreferencesSource,
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

    private val _satData = MutableLiveData<MapData>()
    val mapData: LiveData<MapData> = this._satData

    private val _satPositions = MutableLiveData<Map<Satellite, GeoPos>>()
    val satPositions: LiveData<Map<Satellite, GeoPos>> = _satPositions

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

    fun selectDefaultSatellite(catnum: Int?) {
        viewModelScope.launch {
            dataRepository.getSelectedSatellites().also { satellites ->
                if (satellites.isNotEmpty()) {
                    allSatList = satellites
                    if (catnum == null) {
                        selectSatellite(satellites.first())
                    } else {
                        satellites.find { it.params.catnum == catnum }?.let { selectSatellite(it) }
                    }
                }
            }
        }
    }

    fun selectSatellite(satellite: Satellite, updateFreq: Long = 2000) {
        selectedSat = satellite
        viewModelScope.launch {
            dataUpdateJob?.cancelAndJoin()
            dataUpdateJob = launch {
                val dateNow = Date()
                getSatTrack(selectedSat, stationPos, dateNow)
                while (isActive) {
                    dateNow.time = System.currentTimeMillis()
                    getPositions(allSatList, stationPos, dateNow)
                    getSatFootprint(selectedSat, stationPos, dateNow)
                    getSatData(selectedSat, stationPos, dateNow)
                    delay(updateFreq)
                }
            }
        }
    }

    private suspend fun getSatTrack(satellite: Satellite, pos: GeoPos, date: Date) {
        val satTracks = mutableListOf<List<GeoPos>>()
        val currentTrack = mutableListOf<GeoPos>()
        val endDate = Date(date.time + (satellite.orbitalPeriod * 2.4 * 60000L).toLong())
        var oldLongitude = 0.0
        predictor.getSatTrack(satellite, pos, date.time, endDate.time).forEach { satPos ->
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

    private suspend fun getPositions(satellites: List<Satellite>, pos: GeoPos, date: Date) {
        val positions = mutableMapOf<Satellite, GeoPos>()
        satellites.forEach { satellite ->
            val satPos = predictor.getSatPos(satellite, pos, date.time)
            val osmLat = clipLat(Math.toDegrees(satPos.latitude))
            val osmLon = clipLon(Math.toDegrees(satPos.longitude))
            positions[satellite] = GeoPos(osmLat, osmLon)
        }
        _satPositions.postValue(positions)
    }

    private suspend fun getSatFootprint(satellite: Satellite, pos: GeoPos, date: Date) {
        val satPos = predictor.getSatPos(satellite, pos, date.time)
        val satFootprint = satPos.getRangeCircle().map { rangePos ->
            val osmLat = clipLat(rangePos.latitude)
            val osmLon = clipLon(rangePos.longitude)
            GeoPos(osmLat, osmLon)
        }
        _satFootprint.postValue(satFootprint)
    }

    private suspend fun getSatData(satellite: Satellite, pos: GeoPos, date: Date) {
        val satPos = predictor.getSatPos(satellite, pos, date.time)
        val osmLat = clipLat(Math.toDegrees(satPos.latitude))
        val osmLon = clipLon(Math.toDegrees(satPos.longitude))
        val osmPos = GeoPos(osmLat, osmLon)
        val qthLoc = QthConverter.positionToQth(osmPos.latitude, osmPos.longitude) ?: "-- --"
        val satData = MapData(
            satellite, satellite.params.catnum, satellite.params.name, satPos.range,
            satPos.altitude, satPos.getOrbitalVelocity(), qthLoc, osmPos
        )
        _satData.postValue(satData)
    }

    private fun clipLat(latitude: Double): Double {
        return clip(latitude, -85.05, 85.05)
    }

    private fun clipLon(longitude: Double): Double {
        var result = longitude
        while (result < -180.0) result += 360.0
        while (result > 180.0) result -= 360.0
        return clip(result, -180.0, 180.0)
    }

    private fun clip(currentValue: Double, minValue: Double, maxValue: Double): Double {
        return min(max(currentValue, minValue), maxValue)
    }
}
