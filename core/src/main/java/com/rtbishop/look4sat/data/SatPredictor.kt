package com.rtbishop.look4sat.data

import com.rtbishop.look4sat.domain.predict4kotlin.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext
import java.util.*

class SatPredictor(
    private val preferencesSource: PreferencesSource,
    private val defaultDispatcher: CoroutineDispatcher
) {
    // Multi Sat passes
    private val _passes = MutableSharedFlow<List<SatPass>>(replay = 1)
    private var selectedSatellites = emptyList<Satellite>()
    val passes: SharedFlow<List<SatPass>> = _passes

    suspend fun triggerCalculation(satellites: List<Satellite>, refDate: Date = Date()) {
        if (satellites.isEmpty()) {
            _passes.emit(emptyList())
        } else {
            val oldCatNums = selectedSatellites.map { it.params.catnum }
            val newCatNums = satellites.map { it.params.catnum }
            if (oldCatNums != newCatNums) forceCalculation(satellites, refDate)
        }
    }

    suspend fun forceCalculation(satellites: List<Satellite>, refDate: Date = Date()) {
        if (satellites.isEmpty()) {
            _passes.emit(emptyList())
        } else {
            withContext(defaultDispatcher) {
                val allPasses = mutableListOf<SatPass>()
                selectedSatellites = satellites
                satellites.forEach { satellite -> allPasses.addAll(getPasses(satellite, refDate)) }
                _passes.emit(filterPasses(allPasses, refDate))
            }
        }
    }

    private fun getPasses(satellite: Satellite, refDate: Date): List<SatPass> {
        val predictor = satellite.getPredictor(preferencesSource.loadStationPosition())
        return predictor.getPasses(refDate, preferencesSource.getHoursAhead(), true)
    }

    private fun filterPasses(passes: List<SatPass>, refDate: Date): List<SatPass> {
        val timeFuture = refDate.time + (preferencesSource.getHoursAhead() * 3600 * 1000)
        return passes.filter { it.losTime > refDate.time }
            .filter { it.aosTime < timeFuture }
            .filter { it.maxElevation > preferencesSource.getMinElevation() }
            .sortedBy { it.aosTime }
    }

    // Single Sat radar

    // Constant calculation of beacon Az/El point
    private val _beaconPoint = MutableSharedFlow<SkyPos>(replay = 1)
    val beaconPoint: SharedFlow<SkyPos> = _beaconPoint

    suspend fun calculateSatBeacon(satPass: SatPass, stationPos: StationPos, date: Date) {
        withContext(defaultDispatcher) {
            val satPos = satPass.satellite.getPosition(stationPos, date)
            _beaconPoint.emit(SkyPos(satPos.azimuth, satPos.elevation))
        }
    }

    // Single calculation of beacon Az/El points list
    private val _trajectoryPoints = MutableSharedFlow<List<SkyPos>>(replay = 1)
    val trajectoryPoints: SharedFlow<List<SkyPos>> = _trajectoryPoints

    suspend fun calculateSatTrajectory(satPass: SatPass, stationPos: StationPos) {
        withContext(defaultDispatcher) {
            val trajectoryPoints = mutableListOf<SkyPos>()
            var currentTime = satPass.aosTime
            while (currentTime < satPass.losTime) {
                val satPos = satPass.satellite.getPosition(stationPos, Date(currentTime))
                trajectoryPoints.add(SkyPos(satPos.azimuth, satPos.elevation))
                currentTime += 15000
            }
            _trajectoryPoints.emit(trajectoryPoints)
        }
    }

    // Multi Sat map
}