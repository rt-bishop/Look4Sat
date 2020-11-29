/*******************************************************************************
 Look4Sat. Amateur radio satellite tracker and pass predictor.
 Copyright (C) 2019, 2020 Arty Bishop (bishop.arty@gmail.com)

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along
 with this program; if not, write to the Free Software Foundation, Inc.,
 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 ******************************************************************************/

package com.rtbishop.look4sat.ui.mainScreen

import android.graphics.Color
import android.graphics.Paint
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.amsacode.predict4java.Position
import com.github.amsacode.predict4java.SatPos
import com.rtbishop.look4sat.data.SatPass
import com.rtbishop.look4sat.data.SelectedSat
import com.rtbishop.look4sat.utility.PrefsManager
import com.rtbishop.look4sat.utility.Utilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt

class MapViewModel @ViewModelInject constructor(val prefsManager: PrefsManager) : ViewModel() {

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
    private var filteredPasses = listOf<SatPass>()
    private lateinit var selectedPass: SatPass

    private val _gsp = MutableLiveData(getStationPosition())
    fun getGSP(): LiveData<Position> = _gsp

    private val _selectedSat = MutableLiveData<SelectedSat>()
    fun getSelectedSat(): LiveData<SelectedSat> = _selectedSat

    private val _satMarkers = MutableLiveData<Map<SatPass, Position>>()
    fun getSatMarkers(): LiveData<Map<SatPass, Position>> = _satMarkers

    fun setPasses(passesList: List<SatPass>) {
        filteredPasses = passesList.distinctBy { it.tle }
        selectedPass = filteredPasses[0]
        viewModelScope.launch {
            while (true) {
                dateNow.time = System.currentTimeMillis()
                _selectedSat.value = getDataForSelSatellite(selectedPass)
                _satMarkers.value = getDataForAllSatellites(filteredPasses)
                delay(2000)
            }
        }
    }

    fun scrollSelection(decrement: Boolean) {
        val index = filteredPasses.indexOf(selectedPass)
        if (decrement) {
            if (index > 0) selectSatellite(filteredPasses[index - 1])
            else selectSatellite(filteredPasses[filteredPasses.size - 1])
        } else {
            if (index < filteredPasses.size - 1) selectSatellite(filteredPasses[index + 1])
            else selectSatellite(filteredPasses[0])
        }
    }

    fun selectSatellite(satPass: SatPass) {
        selectedPass = satPass
        viewModelScope.launch { _selectedSat.value = getDataForSelSatellite(selectedPass) }
    }

    private suspend fun getDataForSelSatellite(pass: SatPass): SelectedSat =
        withContext(Dispatchers.Default) {
            val satPos = pass.predictor.getSatPos(dateNow)
            val osmPos = getOsmPosition(satPos.latitude, satPos.longitude, true)
            val qthLoc = Utilities.locToQTH(osmPos.lat, osmPos.lon)
            val velocity = getSatVelocity(satPos.altitude)
            val coverage = satPos.rangeCircleRadiusKm * 2
            val footprint = getSatFootprint(satPos)
            val track = getSatTrack(pass)
            return@withContext SelectedSat(
                pass, pass.tle.catnum, pass.tle.name, satPos.range,
                satPos.altitude, velocity, qthLoc, osmPos, coverage, footprint, track
            )
        }

    private suspend fun getDataForAllSatellites(passes: List<SatPass>): Map<SatPass, Position> =
        withContext(Dispatchers.Default) {
            val passesMap = mutableMapOf<SatPass, Position>()
            passes.forEach {
                val satPos = it.predictor.getSatPos(dateNow)
                passesMap[it] = getOsmPosition(satPos.latitude, satPos.longitude, true)
            }
            return@withContext passesMap
        }

    private fun getSatTrack(pass: SatPass): Overlay {
        val period = (24 * 60 / pass.tle.meanmo).toInt()
        val positions = pass.predictor.getPositions(dateNow, 20, 0, period * 3)
        val trackOverlay = FolderOverlay()
        val trackPoints = mutableListOf<GeoPoint>()
        var oldLon = 0.0
        positions.forEach {
            val osmPos = getOsmPosition(it.latitude, it.longitude, true)
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
            val osmPos = getOsmPosition(it.lat, it.lon, false)
            rangePoints.add(GeoPoint(osmPos.lat, osmPos.lon))
        }
        return Polygon().apply {
            fillPaint.set(footprintPaint)
            outlinePaint.set(footprintPaint)
            points = rangePoints
        }
    }

    private fun getStationPosition(): Position {
        val stationPosition = prefsManager.getStationPosition()
        return getOsmPosition(stationPosition.latitude, stationPosition.longitude, false)
    }

    private fun getSatVelocity(altitude: Double): Double {
        val earthG = 6.674 * 10.0.pow(-11)
        val earthM = 5.98 * 10.0.pow(24)
        val radius = 6.37 * 10.0.pow(6) + altitude * 10.0.pow(3)
        return sqrt(earthG * earthM / radius) / 1000
    }

    private fun getOsmPosition(lat: Double, lon: Double, inRadians: Boolean): Position {
        return if (inRadians) {
            var osmLat = Math.toDegrees(lat)
            var osmLon = Math.toDegrees(lon)
            if (osmLat > 85.05) osmLat = 85.05 else if (osmLat < -85.05) osmLat = -85.05
            if (osmLon > 180f) osmLon -= 360f
            Position(osmLat, osmLon)
        } else {
            val osmLat = if (lat > 85.05) 85.05 else if (lat < -85.05) -85.05 else lat
            val osmLon = if (lon > 180.0) lon - 360.0 else lon
            Position(osmLat, osmLon)
        }
    }
}