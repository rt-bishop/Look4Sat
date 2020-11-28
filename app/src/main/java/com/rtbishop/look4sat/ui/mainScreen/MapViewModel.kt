package com.rtbishop.look4sat.ui.mainScreen

import android.graphics.Color
import android.graphics.Paint
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.amsacode.predict4java.GroundStationPosition
import com.github.amsacode.predict4java.Position
import com.rtbishop.look4sat.data.SatPass
import com.rtbishop.look4sat.data.SelectedSat
import com.rtbishop.look4sat.utility.PrefsManager
import com.rtbishop.look4sat.utility.Utilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt

class MapViewModel @ViewModelInject constructor(prefsManager: PrefsManager) : ViewModel() {

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

    private val _gsp = MutableLiveData(prefsManager.getStationPosition())
    fun getGSP(): LiveData<GroundStationPosition> = _gsp

    private val _selectedSat = MutableLiveData<SelectedSat>()
    fun getSelectedSat(): LiveData<SelectedSat> = _selectedSat

    private val _satMarkers = MutableLiveData<Map<SatPass, Position>>()
    fun getSatMarkers(): LiveData<Map<SatPass, Position>> = _satMarkers

    fun setPasses(passesList: List<SatPass>) {
        filteredPasses = passesList.distinctBy { it.tle }
        setSelectedSat(filteredPasses[0])
        calculatePositions(filteredPasses)
    }

    fun changeSelection(decrement: Boolean) {
        val index = filteredPasses.indexOf(_selectedSat.value?.pass)
        if (decrement) {
            if (index > 0) setSelectedSat(filteredPasses[index - 1])
            else setSelectedSat(filteredPasses[filteredPasses.size - 1])
        } else {
            if (index < filteredPasses.size - 1) setSelectedSat(filteredPasses[index + 1])
            else setSelectedSat(filteredPasses[0])
        }
    }

    fun setSelectedSat(pass: SatPass?) {
        pass?.let {
            viewModelScope.launch(Dispatchers.Default) {
                val satPos = pass.predictor.getSatPos(dateNow)
                val osmPos = getOsmPosition(satPos.latitude, satPos.longitude, true)
                val qthLoc = Utilities.locToQTH(osmPos.lat, osmPos.lon)
                val velocity = getSatVelocity(satPos.altitude)
                val coverage = satPos.rangeCircleRadiusKm * 2
                val footprint = getSatFootprint(pass)
                val track = getSatTrack(pass)
                _selectedSat.postValue(
                    SelectedSat(
                        pass, pass.tle.catnum, pass.tle.name, satPos.range,
                        satPos.altitude, velocity, qthLoc, osmPos, coverage, footprint, track
                    )
                )
            }
        }
    }

    private fun calculatePositions(passes: List<SatPass>) {
        val passesMap = mutableMapOf<SatPass, Position>()
        viewModelScope.launch {
            while (true) {
                dateNow.time = System.currentTimeMillis()
                passes.forEach {
                    val satPos = it.predictor.getSatPos(dateNow)
                    passesMap[it] = getOsmPosition(satPos.latitude, satPos.longitude, true)
                }
                setSelectedSat(_selectedSat.value?.pass)
                _satMarkers.postValue(passesMap)
                delay(2000)
            }
        }
    }

    private fun getSatFootprint(pass: SatPass): Overlay {
        val rangePoints = mutableListOf<GeoPoint>()
        pass.predictor.getSatPos(dateNow).rangeCircle.withIndex().forEach {
            val osmPos = getOsmPosition(it.value.lat, it.value.lon, false)
            rangePoints.add(GeoPoint(osmPos.lat, osmPos.lon))
        }
        return Polygon().apply {
            fillPaint.set(footprintPaint)
            outlinePaint.set(footprintPaint)
            points = rangePoints
        }
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