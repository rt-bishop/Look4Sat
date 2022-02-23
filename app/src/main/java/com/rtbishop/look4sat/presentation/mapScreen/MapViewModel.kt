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
import com.rtbishop.look4sat.domain.IRepository
import com.rtbishop.look4sat.domain.ISettings
import com.rtbishop.look4sat.domain.QthConverter
import com.rtbishop.look4sat.domain.predict.GeoPos
import com.rtbishop.look4sat.domain.predict.Predictor
import com.rtbishop.look4sat.domain.predict.SatPos
import com.rtbishop.look4sat.domain.predict.Satellite
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

@HiltViewModel
class MapViewModel @Inject constructor(
    private val predictor: Predictor,
    private val repository: IRepository,
    private val settings: ISettings,
) : ViewModel() {

    private val stationPosition = settings.loadStationPosition()
    private var allSatellites = listOf<Satellite>()
    private var dataUpdateJob: Job? = null
    private var dataUpdateRate = 2000L
    private lateinit var selectedSatellite: Satellite

    val stationPos = liveData {
        val osmLat = clipLat(stationPosition.latitude)
        val osmLon = clipLon(stationPosition.longitude)
        emit(GeoPos(osmLat, osmLon))
    }

    private val _track = MutableLiveData<List<List<GeoPos>>>()
    val track: LiveData<List<List<GeoPos>>> = _track

    private val _footprint = MutableLiveData<SatPos>()
    val footprint: LiveData<SatPos> = _footprint

    private val _mapData = MutableLiveData<MapData>()
    val mapData: LiveData<MapData> = this._mapData

    private val _positions = MutableLiveData<Map<Satellite, GeoPos>>()
    val positions: LiveData<Map<Satellite, GeoPos>> = _positions

    fun scrollSelection(decrement: Boolean) {
        if (allSatellites.isNotEmpty()) {
            val index = allSatellites.indexOf(selectedSatellite)
            if (decrement) {
                if (index > 0) selectSatellite(allSatellites[index - 1])
                else selectSatellite(allSatellites[allSatellites.size - 1])
            } else {
                if (index < allSatellites.size - 1) selectSatellite(allSatellites[index + 1])
                else selectSatellite(allSatellites[0])
            }
        }
    }

    fun selectDefaultSatellite(catnum: Int) {
        viewModelScope.launch {
            val selectedIds = settings.loadEntriesSelection()
            repository.getEntriesWithIds(selectedIds).also { satellites ->
                if (satellites.isNotEmpty()) {
                    allSatellites = satellites
                    if (catnum == -1) {
                        selectSatellite(satellites.first())
                    } else {
                        satellites.find { it.data.catnum == catnum }?.let { selectSatellite(it) }
                    }
                }
            }
        }
    }

    fun selectSatellite(satellite: Satellite, updateFreq: Long = dataUpdateRate) {
        selectedSatellite = satellite
        viewModelScope.launch {
            dataUpdateJob?.cancelAndJoin()
            dataUpdateJob = launch {
                val dateNow = Date()
                getSatTrack(selectedSatellite, stationPosition, dateNow)
                while (isActive) {
                    dateNow.time = System.currentTimeMillis()
                    getPositions(allSatellites, stationPosition, dateNow)
                    getSatFootprint(selectedSatellite, stationPosition, dateNow)
                    getSatData(selectedSatellite, stationPosition, dateNow)
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
        _track.postValue(satTracks)
    }

    private suspend fun getPositions(satellites: List<Satellite>, pos: GeoPos, date: Date) {
        val positions = mutableMapOf<Satellite, GeoPos>()
        satellites.forEach { satellite ->
            val satPos = predictor.getSatPos(satellite, pos, date.time)
            val osmLat = clipLat(Math.toDegrees(satPos.latitude))
            val osmLon = clipLon(Math.toDegrees(satPos.longitude))
            positions[satellite] = GeoPos(osmLat, osmLon)
        }
        _positions.postValue(positions)
    }

    private suspend fun getSatFootprint(satellite: Satellite, pos: GeoPos, date: Date) {
        val satPos = predictor.getSatPos(satellite, pos, date.time)
        _footprint.postValue(satPos)
    }

    private suspend fun getSatData(satellite: Satellite, pos: GeoPos, date: Date) {
        val satPos = predictor.getSatPos(satellite, pos, date.time)
        val osmLat = clipLat(Math.toDegrees(satPos.latitude))
        val osmLon = clipLon(Math.toDegrees(satPos.longitude))
        val osmPos = GeoPos(osmLat, osmLon)
        val qthLoc = QthConverter.positionToQth(osmPos.latitude, osmPos.longitude) ?: "-- --"
        val satData = MapData(
            satellite, satellite.data.catnum, satellite.data.name, satPos.distance,
            satPos.altitude, satPos.getOrbitalVelocity(), qthLoc, osmPos
        )
        _mapData.postValue(satData)
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
