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
package com.rtbishop.look4sat.data

import com.rtbishop.look4sat.domain.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class PassPredictor(private val defaultDispatcher: CoroutineDispatcher) {

    // Radar view - beacon
    private val _skyPosition = MutableSharedFlow<SatPos>(replay = 1)
    val skyPosition: SharedFlow<SatPos> = _skyPosition

    suspend fun getSkyPosition(pass: SatPass, stationPos: StationPos, date: Date) {
        withContext(defaultDispatcher) {
            _skyPosition.emit(pass.satellite.getPosition(stationPos, date.time))
        }
    }

    // Radar view - track
    private val _skyTrack = MutableSharedFlow<List<SatPos>>(replay = 1)
    val skyTrack: SharedFlow<List<SatPos>> = _skyTrack

    suspend fun getSkyTrack(pass: SatPass, stationPos: StationPos) {
        withContext(defaultDispatcher) {
            val positions = mutableListOf<SatPos>()
            var currentTime = pass.aosTime
            while (currentTime < pass.losTime) {
                positions.add(pass.satellite.getPosition(stationPos, currentTime))
                currentTime += 15000
            }
            _skyTrack.emit(positions)
        }
    }

    // Map view - beacons
    private val _mapPositions = MutableSharedFlow<Map<Satellite, GeoPos>>(replay = 1)
    val mapPositions: SharedFlow<Map<Satellite, GeoPos>> = _mapPositions

    suspend fun getMapPositions(satellites: List<Satellite>, stationPos: StationPos, date: Date) {
        withContext(defaultDispatcher) {
            val positions = mutableMapOf<Satellite, GeoPos>()
            satellites.forEach { satellite ->
                val satPos = satellite.getPosition(stationPos, date.time)
                val osmLat = clipLat(Math.toDegrees(satPos.latitude))
                val osmLon = clipLon(Math.toDegrees(satPos.longitude))
                positions[satellite] = GeoPos(osmLat, osmLon)
            }
            _mapPositions.emit(positions)
        }
    }

    // Map view - track
    private val _mapTrack = MutableSharedFlow<List<List<GeoPos>>>(replay = 1)
    val mapTrack: SharedFlow<List<List<GeoPos>>> = _mapTrack

    suspend fun getMapTrack(satellite: Satellite, stationPos: StationPos, date: Date) {
        withContext(defaultDispatcher) {
            val positions = mutableListOf<SatPos>()
            val orbitalPeriod = satellite.getQuarterOrbitMin() * 4
            val endTime = date.time + (orbitalPeriod * 2.4 * 60L * 1000L).toLong()
            var currentTime = date.time
            while (currentTime < endTime) {
                positions.add(satellite.getPosition(stationPos, currentTime))
                currentTime += 15000
            }

            val satTracks = mutableListOf<List<GeoPos>>()
            val currentTrack = mutableListOf<GeoPos>()
            var oldLongitude = 0.0
            positions.forEach { satPos ->
                val osmLat = clipLat(Math.toDegrees(satPos.latitude))
                val osmLon = clipLon(Math.toDegrees(satPos.longitude))
                val currentPos = GeoPos(osmLat, osmLon)
                if (oldLongitude < -170.0 && currentPos.longitude > 170.0) {
                    // adding left terminal position
                    currentTrack.add(GeoPos(osmLat, -180.0))
                    satTracks.add(mutableListOf<GeoPos>().apply { addAll(currentTrack) })
                    currentTrack.clear()
                } else if (oldLongitude > 170.0 && currentPos.longitude < -170.0) {
                    // adding right terminal position
                    currentTrack.add(GeoPos(osmLat, 180.0))
                    satTracks.add(mutableListOf<GeoPos>().apply { addAll(currentTrack) })
                    currentTrack.clear()
                }
                oldLongitude = currentPos.longitude
                currentTrack.add(currentPos)
            }
            satTracks.add(currentTrack)
            _mapTrack.emit(satTracks)
        }
    }

    private val _mapFootprint = MutableSharedFlow<List<GeoPos>>(replay = 1)
    val mapFootprint: SharedFlow<List<GeoPos>> = _mapFootprint

    suspend fun setSelectedSatFootprint(satellite: Satellite, stationPos: StationPos, date: Date) {
        withContext(defaultDispatcher) {
            val satPos = satellite.getPosition(stationPos, date.time)
            val satFootprint = satPos.getRangeCircle().map { groundPos ->
                val osmLat = clipLat(groundPos.latitude)
                val osmLon = clipLon(groundPos.longitude)
                GeoPos(osmLat, osmLon)
            }
            _mapFootprint.emit(satFootprint)
        }
    }

    private val _satData = MutableSharedFlow<SatData>(replay = 1)
    val satData: SharedFlow<SatData> = _satData

    suspend fun setSelectedSatData(satellite: Satellite, stationPos: StationPos, date: Date) {
        withContext(defaultDispatcher) {
            val satPos = satellite.getPosition(stationPos, date.time)
            val osmLat = clipLat(Math.toDegrees(satPos.latitude))
            val osmLon = clipLon(Math.toDegrees(satPos.longitude))
            val osmPos = GeoPos(osmLat, osmLon)
            val qthLoc = QthConverter.positionToQTH(osmPos.latitude, osmPos.longitude) ?: "-- --"
            val velocity = getOrbitalVelocity(satPos.altitude)
            val satData = SatData(
                satellite, satellite.params.catnum, satellite.params.name, satPos.range,
                satPos.altitude, velocity, qthLoc, osmPos
            )
            _satData.emit(satData)
        }
    }

    private fun getOrbitalVelocity(altitude: Double): Double {
        val earthG = 6.674 * 10.0.pow(-11)
        val earthM = 5.98 * 10.0.pow(24)
        val radius = 6.37 * 10.0.pow(6) + altitude * 10.0.pow(3)
        return sqrt(earthG * earthM / radius) / 1000
    }

    private fun clipLat(pLatitude: Double): Double {
        return clip(pLatitude, -85.05, 85.05)
    }

    private fun clipLon(pLongitude: Double): Double {
        var result = pLongitude
        while (result < -180.0) result += 360.0
        while (result > 180.0) result -= 360.0
        return clip(result, -180.0, 180.0)
    }

    private fun clip(currentValue: Double, minValue: Double, maxValue: Double): Double {
        return min(max(currentValue, minValue), maxValue)
    }

    fun getPositions(
        satellite: Satellite,
        stationPos: StationPos,
        refDate: Date,
        stepSec: Int,
        minBefore: Int,
        orbits: Double
    ): List<SatPos> {
        val positions = mutableListOf<SatPos>()
        val orbitalPeriod = 24 * 60 / satellite.params.meanmo
        val endDate = Date(refDate.time + (orbitalPeriod * orbits * 60L * 1000L).toLong())
        val startDate = Date(refDate.time - minBefore * 60L * 1000L)
        var currentDate = startDate
        while (currentDate.before(endDate)) {
            val satPos = satellite.getPosition(stationPos, currentDate.time)
            positions.add(satPos)
            currentDate = Date(currentDate.time + stepSec * 1000)
        }
        return positions
    }

    // List view - passes
    private val _passes = MutableSharedFlow<List<SatPass>>(replay = 1)
    private var selectedSatellites = emptyList<Satellite>()
    val passes: SharedFlow<List<SatPass>> = _passes

    suspend fun triggerCalculation(
        satellites: List<Satellite>,
        stationPos: StationPos,
        refDate: Date = Date(),
        hoursAhead: Int = 8
    ) {
        if (satellites.isEmpty()) {
            _passes.emit(emptyList())
        } else {
            val oldCatNums = selectedSatellites.map { it.params.catnum }
            val newCatNums = satellites.map { it.params.catnum }
            if (oldCatNums != newCatNums) forceCalculation(
                satellites,
                stationPos,
                refDate,
                hoursAhead
            )
        }
    }

    suspend fun forceCalculation(
        satellites: List<Satellite>,
        stationPos: StationPos,
        refDate: Date = Date(),
        hoursAhead: Int = 8
    ) {
        if (satellites.isEmpty()) {
            _passes.emit(emptyList())
        } else {
            withContext(defaultDispatcher) {
                val allPasses = mutableListOf<SatPass>()
                selectedSatellites = satellites
                satellites.forEach { satellite ->
                    val passes = getPasses(satellite, stationPos, refDate, hoursAhead)
                    allPasses.addAll(passes)
                }
                _passes.emit(filterPasses(allPasses, refDate))
            }
        }
    }

    private fun getPasses(
        satellite: Satellite,
        stationPos: StationPos,
        date: Date,
        hoursAhead: Int
    ): List<SatPass> {
        val passes = mutableListOf<SatPass>()
        val endDate = Date(date.time + hoursAhead * 60L * 60L * 1000L)
        val quarterOrbitMin = satellite.getQuarterOrbitMin()
        var startDate = date
        var shouldWindBack = true
        var lastAosDate: Date
        var count = 0
        if (satellite.willBeSeen(stationPos)) {
            if (satellite.params.isDeepspace) {
                passes.add(nextDeepSpacePass(satellite, stationPos, date))
            } else {
                do {
                    if (count > 0) shouldWindBack = false
                    val pass = nextNearEarthPass(satellite, stationPos, startDate, shouldWindBack)
                    lastAosDate = Date(pass.aosTime)
                    passes.add(pass)
                    startDate = Date(pass.losTime + (quarterOrbitMin * 3) * 60L * 1000L)
                    count++
                } while (lastAosDate < endDate)
            }
        }
        return passes
    }

    private fun filterPasses(
        passes: List<SatPass>,
        refDate: Date,
        hoursAhead: Int = 8,
        minElevation: Double = 16.0
    ): List<SatPass> {
        val timeFuture = refDate.time + (hoursAhead * 3600 * 1000)
        return passes.filter { it.losTime > refDate.time }
            .filter { it.aosTime < timeFuture }
            .filter { it.maxElevation > minElevation }
            .sortedBy { it.aosTime }
    }

    private fun nextDeepSpacePass(
        satellite: Satellite,
        stationPos: StationPos,
        refDate: Date
    ): SatPass {
        val satPos = satellite.getPosition(stationPos, refDate.time)
        val aos = Date(refDate.time - 24 * 60L * 60L * 1000L).time
        val los = Date(refDate.time + 24 * 60L * 60L * 1000L).time
        val tca = Date((aos + los) / 2).time
        val az = Math.toDegrees(satPos.azimuth)
        val elev = Math.toDegrees(satPos.elevation)
        val alt = satPos.altitude
        return SatPass(aos, az, los, az, tca, az, alt, elev, satellite)
    }

    private fun nextNearEarthPass(
        satellite: Satellite,
        stationPos: StationPos,
        refDate: Date,
        windBack: Boolean = false
    ): SatPass {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            timeInMillis = refDate.time
        }
        val quarterOrbitMin = satellite.getQuarterOrbitMin()

        var elevation: Double
        var maxElevation = 0.0
        var alt = 0.0
        var tcaAz = 0.0

        // wind back time 1/4 of an orbit
        if (windBack) calendar.add(Calendar.MINUTE, -quarterOrbitMin)
        var satPos = satellite.getPosition(stationPos, calendar.time.time)

        if (satPos.elevation > 0.0) {
            // move forward in 30 second intervals until the sat goes below the horizon
            do {
                calendar.add(Calendar.SECOND, 30)
                satPos = satellite.getPosition(stationPos, calendar.time.time)
            } while (satPos.elevation > 0.0)
            // move forward 3/4 of an orbit
            calendar.add(Calendar.MINUTE, quarterOrbitMin * 3)
        }

        // find the next time sat comes above the horizon
        do {
            calendar.add(Calendar.SECOND, 60)
            satPos = satellite.getPosition(stationPos, calendar.time.time)
            elevation = satPos.elevation
            if (elevation > maxElevation) {
                maxElevation = elevation
                alt = satPos.altitude
                tcaAz = Math.toDegrees(satPos.azimuth)
            }
        } while (satPos.elevation < 0.0)

        // refine to 3 seconds
        calendar.add(Calendar.SECOND, -60)
        do {
            calendar.add(Calendar.SECOND, 3)
            satPos = satellite.getPosition(stationPos, calendar.time.time)
            elevation = satPos.elevation
            if (elevation > maxElevation) {
                maxElevation = elevation
                alt = satPos.altitude
                tcaAz = Math.toDegrees(satPos.azimuth)
            }
        } while (satPos.elevation < 0.0)

        val aos = satPos.time
        val aosAz = Math.toDegrees(satPos.azimuth)

        // find when sat goes below
        do {
            calendar.add(Calendar.SECOND, 30)
            satPos = satellite.getPosition(stationPos, calendar.time.time)
            elevation = satPos.elevation
            if (elevation > maxElevation) {
                maxElevation = elevation
                alt = satPos.altitude
                tcaAz = Math.toDegrees(satPos.azimuth)
            }
        } while (satPos.elevation > 0.0)

        // refine to 3 seconds
        calendar.add(Calendar.SECOND, -30)
        do {
            calendar.add(Calendar.SECOND, 3)
            satPos = satellite.getPosition(stationPos, calendar.time.time)
            elevation = satPos.elevation
            if (elevation > maxElevation) {
                maxElevation = elevation
                alt = satPos.altitude
                tcaAz = Math.toDegrees(satPos.azimuth)
            }
        } while (satPos.elevation > 0.0)

        val los = satPos.time
        val losAz = Math.toDegrees(satPos.azimuth)
        val tca = Date((aos + los) / 2).time
        val elev = Math.toDegrees(maxElevation)
        return SatPass(aos, aosAz, los, losAz, tca, tcaAz, alt, elev, satellite)
    }
}
