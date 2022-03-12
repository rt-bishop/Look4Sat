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
package com.rtbishop.look4sat.presentation.mapScreen

import androidx.lifecycle.*
import com.rtbishop.look4sat.domain.IDataRepository
import com.rtbishop.look4sat.domain.ISatelliteManager
import com.rtbishop.look4sat.domain.ISettingsManager
import com.rtbishop.look4sat.domain.predict.GeoPos
import com.rtbishop.look4sat.domain.predict.SatPos
import com.rtbishop.look4sat.domain.predict.Satellite
import com.rtbishop.look4sat.utility.QthConverter
import com.rtbishop.look4sat.utility.toDegrees
import com.rtbishop.look4sat.utility.toTimerString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

@HiltViewModel
class MapViewModel @Inject constructor(
    private val satelliteManager: ISatelliteManager,
    private val repository: IDataRepository,
    private val settings: ISettingsManager,
) : ViewModel() {

    private val stationPosition = settings.loadStationPosition()
    private var allPasses = satelliteManager.getPasses()
    private var allSatellites = listOf<Satellite>()
    private var dataUpdateJob: Job? = null
    private var dataUpdateRate = 1000L
    private var selectedSatellite: Satellite? = null

    val stationPos = liveData {
        val osmLat = clipLat(stationPosition.lat)
        val osmLon = clipLon(stationPosition.lon)
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
                        allPasses.find { pass -> pass.progress < 100 && !pass.isDeepSpace }
                            ?.let { pass -> selectSatellite(pass.satellite) }
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
                getSatTrack(satellite, stationPosition, dateNow)
                while (isActive) {
                    dateNow.time = System.currentTimeMillis()
                    getPositions(allSatellites, stationPosition, dateNow)
                    getSatFootprint(satellite, stationPosition, dateNow)
                    getSatData(satellite, stationPosition, dateNow)
                    delay(updateFreq)
                }
            }
        }
    }

    private suspend fun getSatTrack(satellite: Satellite, pos: GeoPos, date: Date) {
        val satTracks = mutableListOf<List<GeoPos>>()
        val currentTrack = mutableListOf<GeoPos>()
        val endDate = Date(date.time + (satellite.data.orbitalPeriod * 2.4 * 60000L).toLong())
        var oldLongitude = 0.0
        satelliteManager.getTrack(satellite, pos, date.time, endDate.time).forEach { satPos ->
            val osmLat = clipLat(satPos.latitude.toDegrees())
            val osmLon = clipLon(satPos.longitude.toDegrees())
            val currentPosition = GeoPos(osmLat, osmLon)
            if (oldLongitude < -170.0 && currentPosition.lon > 170.0) {
                // adding left terminal position
                currentTrack.add(GeoPos(osmLat, -180.0))
                val finishedTrack = mutableListOf<GeoPos>().apply { addAll(currentTrack) }
                satTracks.add(finishedTrack)
                currentTrack.clear()
            } else if (oldLongitude > 170.0 && currentPosition.lon < -170.0) {
                // adding right terminal position
                currentTrack.add(GeoPos(osmLat, 180.0))
                val finishedTrack = mutableListOf<GeoPos>().apply { addAll(currentTrack) }
                satTracks.add(finishedTrack)
                currentTrack.clear()
            }
            oldLongitude = currentPosition.lon
            currentTrack.add(currentPosition)
        }
        satTracks.add(currentTrack)
        _track.postValue(satTracks)
    }

    private suspend fun getPositions(satellites: List<Satellite>, pos: GeoPos, date: Date) {
        val positions = mutableMapOf<Satellite, GeoPos>()
        satellites.forEach { satellite ->
            val satPos = satelliteManager.getPosition(satellite, pos, date.time)
            val osmLat = clipLat(satPos.latitude.toDegrees())
            val osmLon = clipLon(satPos.longitude.toDegrees())
            positions[satellite] = GeoPos(osmLat, osmLon)
        }
        _positions.postValue(positions)
    }

    private suspend fun getSatFootprint(satellite: Satellite, pos: GeoPos, date: Date) {
        val satPos = satelliteManager.getPosition(satellite, pos, date.time)
        _footprint.postValue(satPos)
    }

    private suspend fun getSatData(sat: Satellite, pos: GeoPos, date: Date) {
        var aosTime = 0L.toTimerString()
        allPasses.find { pass -> pass.catNum == sat.data.catnum && pass.progress < 100 }
            ?.let { satPass ->
                if (!satPass.isDeepSpace) {
                    aosTime = if (date.time < satPass.aosTime) {
                        val millisBeforeStart = satPass.aosTime.minus(date.time)
                        millisBeforeStart.toTimerString()
                    } else {
                        val millisBeforeEnd = satPass.losTime.minus(date.time)
                        millisBeforeEnd.toTimerString()
                    }
                }
            }
        val satPos = satelliteManager.getPosition(sat, pos, date.time)
        val azimuth = satPos.azimuth.toDegrees()
        val elevation = satPos.elevation.toDegrees()
        val osmLat = clipLat(satPos.latitude.toDegrees())
        val osmLon = clipLon(satPos.longitude.toDegrees())
        val osmPos = GeoPos(osmLat, osmLon)
        val qthLoc = QthConverter.positionToQth(osmPos.lat, osmPos.lon) ?: "-- --"
        val velocity = satPos.getOrbitalVelocity()
        val phase = satPos.phase.toDegrees()
        val visibility = satPos.eclipsed
        val satData = MapData(
            sat.data.catnum, sat.data.name, aosTime, azimuth, elevation, satPos.distance,
            satPos.altitude, velocity, qthLoc, osmPos, sat.data.orbitalPeriod, phase, visibility
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
