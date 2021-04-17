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
package com.rtbishop.look4sat.ui.mapScreen

import android.graphics.Color
import android.graphics.Paint
import androidx.lifecycle.*
import com.github.amsacode.predict4java.Position
import com.github.amsacode.predict4java.SatPos
import com.github.amsacode.predict4java.Satellite
import com.rtbishop.look4sat.data.model.SelectedSat
import com.rtbishop.look4sat.data.repository.SatelliteRepo
import com.rtbishop.look4sat.utility.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import timber.log.Timber
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    prefsManager: PrefsManager,
    private val satelliteRepo: SatelliteRepo,
    private val qthConverter: QthConverter
) : ViewModel() {

    private val dateNow = Date()
    private val trackPaint = Paint().apply {
        strokeWidth = 2f
        style = Paint.Style.STROKE
        color = Color.RED
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }
    private val footprintPaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = Color.parseColor("#26FFE082")
        isAntiAlias = true
    }

    private val gsp = prefsManager.getStationPosition()

    private var filteredSats = listOf<Satellite>()
    private lateinit var selectedSat: Satellite

    private val _selectedSat = MutableLiveData<SelectedSat>()
    fun getSelectedSat(): LiveData<SelectedSat> = _selectedSat

    private val _satMarkers = MutableLiveData<Map<Satellite, Position>>()
    fun getSatMarkers(): LiveData<Map<Satellite, Position>> = _satMarkers

    val stationPosition = liveData {
        emit(Position(gsp.latitude.toOsmLat(), gsp.longitude.toOsmLon()))
    }

    init {
        viewModelScope.launch {
            val satellites = satelliteRepo.getSelectedSatellites()
            if (satellites.isNotEmpty()) {
                filteredSats = satellites
                selectedSat = satellites.first()
                while (true) {
                    ensureActive()
                    dateNow.time = System.currentTimeMillis()
                    _selectedSat.value = getDataForSelSatellite(selectedSat)
                    _satMarkers.value = getDataForAllSatellites(filteredSats)
                    delay(2000)
                }
            }
        }
    }

    fun scrollSelection(decrement: Boolean) {
        if (filteredSats.isNotEmpty()) {
            val index = filteredSats.indexOf(selectedSat)
            if (decrement) {
                if (index > 0) selectSatellite(filteredSats[index - 1])
                else selectSatellite(filteredSats[filteredSats.size - 1])
            } else {
                if (index < filteredSats.size - 1) selectSatellite(filteredSats[index + 1])
                else selectSatellite(filteredSats[0])
            }
        }
    }

    fun selectSatellite(satellite: Satellite) {
        selectedSat = satellite
        viewModelScope.launch { _selectedSat.value = getDataForSelSatellite(selectedSat) }
    }

    private suspend fun getDataForSelSatellite(satellite: Satellite): SelectedSat =
        withContext(Dispatchers.Default) {
            val satPos = satellite.getPredictor(gsp).getSatPos(dateNow)
            val osmPos = Position(
                Math.toDegrees(satPos.latitude).toOsmLat(),
                Math.toDegrees(satPos.longitude).toOsmLon()
            )
            val qthLoc = qthConverter.locationToQTH(osmPos.lat, osmPos.lon) ?: "-- --"
            val velocity = satPos.altitude.getOrbitalVelocity()
            val footprint = getSatFootprint(satPos)
            val track = getSatTrack(satellite)
            return@withContext SelectedSat(
                satellite, satellite.tle.catnum, satellite.tle.name, satPos.range,
                satPos.altitude, velocity, qthLoc, osmPos, footprint, track
            )
        }

    private suspend fun getDataForAllSatellites(satellites: List<Satellite>): Map<Satellite, Position> =
        withContext(Dispatchers.Default) {
            val passesMap = mutableMapOf<Satellite, Position>()
            satellites.forEach { satellite ->
                val satPos = satellite.getPredictor(gsp).getSatPos(dateNow)
                passesMap[satellite] = Position(
                    Math.toDegrees(satPos.latitude).toOsmLat(),
                    Math.toDegrees(satPos.longitude).toOsmLon()
                )
            }
            return@withContext passesMap
        }

    private fun getSatTrack(satellite: Satellite): Overlay {
        val positions = satellite.getPredictor(gsp).getPositions(dateNow, 15, 0, 3.2)
        val trackOverlay = FolderOverlay()
        val trackPoints = mutableListOf<GeoPoint>()
        var oldLon = 0.0
        positions.forEach {
            val osmPos = Position(
                Math.toDegrees(it.latitude).toOsmLat(),
                Math.toDegrees(it.longitude).toOsmLon()
            )
            if (oldLon < -170.0 && osmPos.lon > 170.0 || oldLon > 170.0 && osmPos.lon < -170.0) {
                val currentPoints = mutableListOf<GeoPoint>()
                currentPoints.addAll(trackPoints)
                Polyline().apply {
                    outlinePaint.set(trackPaint)
                    setPoints(currentPoints)
                    trackOverlay.add(this)
                }
                trackPoints.clear()
            }
            oldLon = osmPos.lon
            trackPoints.add(GeoPoint(osmPos.lat, osmPos.lon))
        }
        Polyline().apply {
            outlinePaint.set(trackPaint)
            setPoints(trackPoints)
            trackOverlay.add(this)
        }
        return trackOverlay
    }

    private fun getSatFootprint(satPos: SatPos): Overlay {
        val rangePoints = mutableListOf<GeoPoint>()
        satPos.rangeCircle.forEach {
            val osmPos = Position(it.lat.toOsmLat(), it.lon.toOsmLon())
            rangePoints.add(GeoPoint(osmPos.lat, osmPos.lon))
        }
        return Polygon().apply {
            fillPaint.set(footprintPaint)
            outlinePaint.set(footprintPaint)
            try {
                points = rangePoints
            } catch (e: IllegalArgumentException) {
                Timber.d("RangeCircle: ${satPos.rangeCircle}, RangePoints: $rangePoints")
            }
        }
    }
}
