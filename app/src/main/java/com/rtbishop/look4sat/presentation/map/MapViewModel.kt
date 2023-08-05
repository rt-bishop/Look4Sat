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
package com.rtbishop.look4sat.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rtbishop.look4sat.MainApplication
import com.rtbishop.look4sat.domain.predict.GeoPos
import com.rtbishop.look4sat.domain.predict.SatPos
import com.rtbishop.look4sat.domain.predict.Satellite
import com.rtbishop.look4sat.domain.repository.ISatelliteRepo
import com.rtbishop.look4sat.domain.repository.ISettingsRepo
import com.rtbishop.look4sat.domain.utility.QthConverter
import com.rtbishop.look4sat.domain.utility.toDegrees
import com.rtbishop.look4sat.domain.utility.toTimerString
import java.util.Date
import kotlin.collections.set
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MapViewModel(private val satelliteRepo: ISatelliteRepo, settingsRepo: ISettingsRepo) : ViewModel() {

    private val stationPos = settingsRepo.stationPosition.value
    private var allPasses = satelliteRepo.passes.value
    private var allSatellites = satelliteRepo.satellites.value
    private var dataUpdateJob: Job? = null
    private var dataUpdateRate = 1000L
    private var selectedSatellite: Satellite? = null

    val stationPosition = flow {
        val osmLat = clipLat(stationPos.latitude)
        val osmLon = clipLon(stationPos.longitude)
        emit(GeoPos(osmLat, osmLon))
    }

    private val _track = MutableSharedFlow<List<List<GeoPos>>>()
    val track: SharedFlow<List<List<GeoPos>>> = _track

    private val _footprint = MutableSharedFlow<SatPos>()
    val footprint: SharedFlow<SatPos> = _footprint

    private val _mapData = MutableSharedFlow<MapData>()
    val mapData: SharedFlow<MapData> = _mapData

    private val _positions = MutableSharedFlow<Map<Satellite, GeoPos>>()
    val positions: SharedFlow<Map<Satellite, GeoPos>> = _positions

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
        if (allSatellites.isNotEmpty()) {
            if (catnum == -1) {
                allPasses.find { pass -> pass.progress < 100 && !pass.isDeepSpace }
                    ?.let { pass -> selectSatellite(pass.satellite) }
            } else {
                allSatellites.find { it.data.catnum == catnum }?.let { selectSatellite(it) }
            }
        }
    }

    fun selectSatellite(satellite: Satellite, updateFreq: Long = dataUpdateRate) {
        selectedSatellite = satellite
        viewModelScope.launch {
            dataUpdateJob?.cancelAndJoin()
            dataUpdateJob = launch {
                val dateNow = Date()
                getSatTrack(satellite, stationPos, dateNow)
                while (isActive) {
                    dateNow.time = System.currentTimeMillis()
                    getPositions(allSatellites, stationPos, dateNow)
                    getSatFootprint(satellite, stationPos, dateNow)
                    getSatData(satellite, stationPos, dateNow)
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
        satelliteRepo.getTrack(satellite, pos, date.time, endDate.time).forEach { satPos ->
            val osmLat = clipLat(satPos.latitude.toDegrees())
            val osmLon = clipLon(satPos.longitude.toDegrees())
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
        _track.emit(satTracks)
    }

    private suspend fun getPositions(satellites: List<Satellite>, pos: GeoPos, date: Date) {
        val positions = mutableMapOf<Satellite, GeoPos>()
        satellites.forEach { satellite ->
            val satPos = satelliteRepo.getPosition(satellite, pos, date.time)
            val osmLat = clipLat(satPos.latitude.toDegrees())
            val osmLon = clipLon(satPos.longitude.toDegrees())
            positions[satellite] = GeoPos(osmLat, osmLon)
        }
        _positions.emit(positions)
    }

    private suspend fun getSatFootprint(satellite: Satellite, pos: GeoPos, date: Date) {
        val satPos = satelliteRepo.getPosition(satellite, pos, date.time)
        _footprint.emit(satPos)
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
        val satPos = satelliteRepo.getPosition(sat, pos, date.time)
        val azimuth = satPos.azimuth.toDegrees()
        val elevation = satPos.elevation.toDegrees()
        val osmLat = clipLat(satPos.latitude.toDegrees())
        val osmLon = clipLon(satPos.longitude.toDegrees())
        val osmPos = GeoPos(osmLat, osmLon)
        val qthLoc = QthConverter.positionToQth(osmPos.latitude, osmPos.longitude) ?: "-- --"
        val velocity = satPos.getOrbitalVelocity()
        val phase = satPos.phase.toDegrees()
        val visibility = satPos.eclipsed
        val satData = MapData(
            sat.data.catnum,
            sat.data.name,
            aosTime,
            azimuth,
            elevation,
            satPos.distance,
            satPos.altitude,
            velocity,
            qthLoc,
            osmPos,
            sat.data.orbitalPeriod,
            phase,
            visibility
        )
        _mapData.emit(satData)
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

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            val applicationKey = ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY
            initializer {
                val container = (this[applicationKey] as MainApplication).container
                MapViewModel(container.satelliteRepo, container.settingsRepo)
            }
        }
    }
}
