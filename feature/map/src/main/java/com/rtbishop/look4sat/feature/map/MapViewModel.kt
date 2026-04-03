/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2026 Arty Bishop and contributors.
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
package com.rtbishop.look4sat.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rtbishop.look4sat.core.domain.predict.GeoPos
import com.rtbishop.look4sat.core.domain.predict.OrbitalObject
import com.rtbishop.look4sat.core.domain.predict.OrbitalPass
import com.rtbishop.look4sat.core.domain.predict.OrbitalPos
import com.rtbishop.look4sat.core.domain.repository.IContainerProvider
import com.rtbishop.look4sat.core.domain.repository.ISatelliteRepo
import com.rtbishop.look4sat.core.domain.repository.ISettingsRepo
import com.rtbishop.look4sat.core.domain.utility.clipLat
import com.rtbishop.look4sat.core.domain.utility.clipLon
import com.rtbishop.look4sat.core.domain.utility.positionToQth
import com.rtbishop.look4sat.core.domain.utility.toDegrees
import com.rtbishop.look4sat.core.domain.utility.toTimerString
import com.rtbishop.look4sat.core.presentation.getDefaultPass
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Date

class MapViewModel(private val satelliteRepo: ISatelliteRepo, settingsRepo: ISettingsRepo) :
    ViewModel() {

    private val stationPos = settingsRepo.stationPosition.value
    private val defaultPass = getDefaultPass()
    private val _uiState = MutableStateFlow(
        MapState(
            mapData = null,
            isLightUi = settingsRepo.otherSettings.value.stateOfLightTheme,
            stationPosition = null,
            orbitalPass = defaultPass,
            track = null,
            footprint = null,
            positions = null,
            sendAction = ::handleAction
        )
    )
    private var allPasses = satelliteRepo.passes.value
    private var allSatellites = satelliteRepo.satellites.value
    private var dataUpdateJob: Job? = null
    private var dataUpdateRate = 1000L
    private var selectedOrbitalObject: OrbitalObject? = null
    val uiState: StateFlow<MapState> = _uiState

    init {
        selectDefaultSatellite(-1)
    }

    private fun handleAction(action: MapAction) {
        when (action) {
            MapAction.SelectPrev -> scrollSelection(true)
            MapAction.SelectNext -> scrollSelection(false)
            is MapAction.SelectItem -> selectSatellite(action.item)
            is MapAction.SelectDefaultItem -> selectDefaultSatellite(action.catnum)
        }
    }

    private fun getStationPosition() {
        val osmLat = clipLat(stationPos.latitude)
        val osmLon = clipLon(stationPos.longitude)
        _uiState.update { it.copy(stationPosition = GeoPos(osmLat, osmLon)) }
    }

    private fun selectDefaultSatellite(catnum: Int) {
        if (allSatellites.isNotEmpty()) {
            if (catnum == -1) {
                allPasses.find { pass -> pass.progress < 100 && !pass.isDeepSpace }
                    ?.let { pass -> selectSatellite(pass.orbitalObject) }
            } else {
                allSatellites.find { it.data.catnum == catnum }?.let { selectSatellite(it) }
            }
        }
    }

    private fun scrollSelection(decrement: Boolean) {
        if (allSatellites.isNotEmpty()) {
            val index = allSatellites.indexOf(selectedOrbitalObject)
            if (decrement) {
                if (index > 0) selectSatellite(allSatellites[index - 1])
                else selectSatellite(allSatellites[allSatellites.size - 1])
            } else {
                if (index < allSatellites.size - 1) selectSatellite(allSatellites[index + 1])
                else selectSatellite(allSatellites[0])
            }
        }
    }

    private fun selectSatellite(orbitalObject: OrbitalObject, updateFreq: Long = dataUpdateRate) {
        selectedOrbitalObject = orbitalObject
        viewModelScope.launch {
            dataUpdateJob?.cancelAndJoin()
            dataUpdateJob = launch {
                val dateNow = Date()
                getStationPosition()
                getSatTrack(orbitalObject, stationPos, dateNow)
                while (isActive) {
                    dateNow.time = System.currentTimeMillis()
                    updateMapState(orbitalObject, allSatellites, stationPos, dateNow)
                    delay(updateFreq)
                }
            }
        }
    }

    /**
     * Combined update: computes all satellite positions in parallel, then derives
     * the selected satellite's footprint and info panel data from the same batch,
     * and emits a single state update to avoid redundant recompositions.
     */
    private suspend fun updateMapState(
        selected: OrbitalObject,
        orbitalObjects: List<OrbitalObject>,
        pos: GeoPos,
        date: Date
    ) {
        // 1. Compute all positions in parallel, collecting full OrbitalPos for the selected sat
        val positionsMap = HashMap<OrbitalObject, GeoPos>(orbitalObjects.size)
        var selectedSatPos: OrbitalPos? = null

        coroutineScope {
            // Chunk satellites to balance parallelism vs coroutine overhead
            val chunkSize = maxOf(1, orbitalObjects.size / PARALLEL_CHUNKS)
            val deferreds = orbitalObjects.chunked(chunkSize).map { chunk ->
                async {
                    val localPositions = ArrayList<Pair<OrbitalObject, GeoPos>>(chunk.size)
                    var localSelectedPos: OrbitalPos? = null
                    for (satellite in chunk) {
                        val satPos = satelliteRepo.getPosition(satellite, pos, date.time)
                        val osmLat = clipLat(satPos.latitude.toDegrees())
                        val osmLon = clipLon(satPos.longitude.toDegrees())
                        localPositions.add(satellite to GeoPos(osmLat, osmLon))
                        if (satellite === selected) {
                            localSelectedPos = satPos
                        }
                    }
                    Pair(localPositions, localSelectedPos)
                }
            }
            for ((localPositions, localSelectedPos) in deferreds.awaitAll()) {
                for (pair in localPositions) {
                    positionsMap[pair.first] = pair.second
                }
                if (localSelectedPos != null) {
                    selectedSatPos = localSelectedPos
                }
            }
        }

        // 2. Derive footprint and info data from the already-computed selected position
        val satPos = selectedSatPos ?: satelliteRepo.getPosition(selected, pos, date.time)
        val footprint = satPos
        val mapData = buildMapData(selected, satPos, date)

        // 3. Single atomic state update — one recomposition per cycle
        _uiState.update {
            it.copy(
                positions = positionsMap,
                footprint = footprint,
                mapData = mapData.first,
                orbitalPass = mapData.second
            )
        }
    }

    /**
     * Builds the map info panel data from an already-computed OrbitalPos.
     * No additional getPosition() call needed.
     */
    private fun buildMapData(
        sat: OrbitalObject,
        satPos: OrbitalPos,
        date: Date
    ): Pair<MapData, OrbitalPass> {
        var orbitalPass = defaultPass
        var aosTime = 0L.toTimerString()
        var isTimeAos = true
        allPasses.find { pass -> pass.catNum == sat.data.catnum && pass.progress < 1 }
            ?.let { satPass ->
                orbitalPass = satPass
                if (!satPass.isDeepSpace) {
                    aosTime = if (date.time < satPass.aosTime) {
                        val millisBeforeStart = satPass.aosTime.minus(date.time)
                        isTimeAos = true
                        millisBeforeStart.toTimerString()
                    } else {
                        val millisBeforeEnd = satPass.losTime.minus(date.time)
                        isTimeAos = false
                        millisBeforeEnd.toTimerString()
                    }
                }
            }
        val azimuth = satPos.azimuth.toDegrees()
        val elevation = satPos.elevation.toDegrees()
        val osmLat = clipLat(satPos.latitude.toDegrees())
        val osmLon = clipLon(satPos.longitude.toDegrees())
        val osmPos = GeoPos(osmLat, osmLon)
        val qthLoc = positionToQth(osmPos.latitude, osmPos.longitude) ?: "-- --"
        val velocity = satPos.getOrbitalVelocity()
        val phase = satPos.phase.toDegrees()
        val visibility = satPos.eclipsed
        val satData = MapData(
            sat.data.catnum,
            sat.data.name,
            aosTime,
            isTimeAos,
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
        return satData to orbitalPass
    }

    private suspend fun getSatTrack(orbitalObject: OrbitalObject, pos: GeoPos, date: Date) {
        val satTracks = mutableListOf<List<GeoPos>>()
        val currentTrack = mutableListOf<GeoPos>()
        val endDate = Date(date.time + (orbitalObject.data.orbitalPeriod * 2.4 * 60000L).toLong())
        var oldLongitude = 0.0
        satelliteRepo.getTrack(orbitalObject, pos, date.time, endDate.time).forEach { satPos ->
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
        _uiState.update { it.copy(track = satTracks) }
    }

    companion object {
        /** Number of parallel chunks for satellite position computation */
        private const val PARALLEL_CHUNKS = 4

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            val applicationKey = ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY
            initializer {
                val container = (this[applicationKey] as IContainerProvider).getMainContainer()
                MapViewModel(container.satelliteRepo, container.settingsRepo)
            }
        }
    }
}
