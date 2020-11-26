package com.rtbishop.look4sat.ui.mainScreen

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.amsacode.predict4java.GroundStationPosition
import com.github.amsacode.predict4java.Position
import com.rtbishop.look4sat.data.SatPass
import com.rtbishop.look4sat.utility.PrefsManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt

class MapViewModel @ViewModelInject constructor(prefsManager: PrefsManager) : ViewModel() {

    private val dateNow = Date()
    private var filteredPasses = listOf<SatPass>()

    private val _gsp = MutableLiveData(prefsManager.getStationPosition())
    fun getGSP(): LiveData<GroundStationPosition> = _gsp

    private val _selectedPass = MutableLiveData<SatPass?>()
    fun getSelectedPass(): LiveData<SatPass?> = _selectedPass

    private val _satMarkers = MutableLiveData<Map<SatPass, Position>>()
    fun getSatMarkers(): LiveData<Map<SatPass, Position>> = _satMarkers

    fun setPasses(passesList: List<SatPass>) {
        filteredPasses = passesList.distinctBy { it.tle }
        _selectedPass.value = filteredPasses[0]
        calculateSatPos(filteredPasses)
    }

    private fun calculateSatPos(passes: List<SatPass>) {
        viewModelScope.launch {
            while (true) {
                val passesMap = mutableMapOf<SatPass, Position>()
                dateNow.time = System.currentTimeMillis()
                passes.forEach {
                    val satPos = it.predictor.getSatPos(dateNow)
                    val osmPos = getOsmPosition(satPos.latitude, satPos.longitude, true)
                    passesMap[it] = osmPos
                }
                _satMarkers.value = passesMap
                delay(2000)
            }
        }
    }

    fun setSelectedSat(satPass: SatPass): Boolean {
        _selectedPass.value = satPass
        return true
    }

    fun changeSelection(decrement: Boolean) {
        val index = filteredPasses.indexOf(_selectedPass.value)
        _selectedPass.value = if (decrement) {
            if (index > 0) filteredPasses[index - 1] else filteredPasses[filteredPasses.size - 1]
        } else {
            if (index < filteredPasses.size - 1) filteredPasses[index + 1] else filteredPasses[0]
        }
    }

    fun getSatVelocity(satAlt: Double): Double {
        val earthG = 6.674 * 10.0.pow(-11)
        val earthM = 5.98 * 10.0.pow(24)
        val radius = 6.37 * 10.0.pow(6) + satAlt * 10.0.pow(3)
        return sqrt(earthG * earthM / radius) / 1000
    }

    fun getOsmPosition(lat: Double, lon: Double, inRadians: Boolean): Position {
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