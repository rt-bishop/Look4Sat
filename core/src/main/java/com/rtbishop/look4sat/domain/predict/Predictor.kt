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
package com.rtbishop.look4sat.domain.predict

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext
import java.util.*

class Predictor(private val predictorDispatcher: CoroutineDispatcher) {

    private val _passes = MutableSharedFlow<List<SatPass>>(replay = 1)
    private var selectedSatIds = emptyList<Int>()
    val passes: SharedFlow<List<SatPass>> = _passes

    suspend fun getSatPos(sat: Satellite, pos: GeoPos, date: Date): SatPos =
        withContext(predictorDispatcher) {
            return@withContext sat.getPosition(pos, date.time)
        }

    suspend fun getSatTrack(sat: Satellite, pos: GeoPos, start: Date, end: Date): List<SatPos> =
        withContext(predictorDispatcher) {
            val positions = mutableListOf<SatPos>()
            var currentTime = start.time
            while (currentTime < end.time) {
                positions.add(sat.getPosition(pos, currentTime))
                currentTime += 15000
            }
            return@withContext positions
        }

    suspend fun triggerCalculation(
        satellites: List<Satellite>,
        pos: GeoPos,
        date: Date = Date(),
        hoursAhead: Int = 8,
        minElevation: Double = 16.0
    ) {
        if (satellites.isEmpty()) {
            _passes.emit(emptyList())
        } else {
            val newCatNums = satellites.map { it.params.catnum }
            if (selectedSatIds != newCatNums) {
                forceCalculation(satellites, pos, date, hoursAhead, minElevation)
            }
        }
    }

    suspend fun forceCalculation(
        satellites: List<Satellite>,
        pos: GeoPos,
        date: Date = Date(),
        hoursAhead: Int = 8,
        minElevation: Double = 16.0
    ) {
        if (satellites.isEmpty()) {
            _passes.emit(emptyList())
        } else {
            withContext(predictorDispatcher) {
                val allPasses = mutableListOf<SatPass>()
                selectedSatIds = satellites.map { it.params.catnum }
                satellites.forEach { satellite ->
                    allPasses.addAll(satellite.getPasses(pos, date, hoursAhead))
                }
                _passes.emit(allPasses.filter(date, hoursAhead, minElevation))
            }
        }
    }

    private fun Satellite.getPasses(pos: GeoPos, date: Date, hours: Int): List<SatPass> {
        val passes = mutableListOf<SatPass>()
        val endDate = Date(date.time + hours * 60L * 60L * 1000L)
        val quarterOrbitMin = (this.orbitalPeriod / 4.0).toInt()
        var startDate = date
        var shouldRewind = true
        var lastAosDate: Date
        var count = 0
        if (this.willBeSeen(pos)) {
            if (this.params.isDeepspace) {
                passes.add(getGeoPass(this, pos, date))
            } else {
                do {
                    if (count > 0) shouldRewind = false
                    val pass = getLeoPass(this, pos, startDate, shouldRewind)
                    lastAosDate = Date(pass.aosTime)
                    passes.add(pass)
                    startDate = Date(pass.losTime + (quarterOrbitMin * 3) * 60L * 1000L)
                    count++
                } while (lastAosDate < endDate)
            }
        }
        return passes
    }

    private fun List<SatPass>.filter(date: Date, hoursAhead: Int, minElev: Double): List<SatPass> {
        val timeFuture = date.time + (hoursAhead * 60L * 60L * 1000L)
        return this.filter { it.losTime > date.time }
            .filter { it.aosTime < timeFuture }
            .filter { it.maxElevation > minElev }
            .sortedBy { it.aosTime }
    }

    private fun getGeoPass(sat: Satellite, pos: GeoPos, date: Date): SatPass {
        val satPos = sat.getPosition(pos, date.time)
        val aos = Date(date.time - 24 * 60L * 60L * 1000L).time
        val los = Date(date.time + 24 * 60L * 60L * 1000L).time
        val tca = Date((aos + los) / 2).time
        val az = Math.toDegrees(satPos.azimuth)
        val elev = Math.toDegrees(satPos.elevation)
        val alt = satPos.altitude
        return SatPass(aos, az, los, az, tca, az, alt, elev, sat)
    }

    private fun getLeoPass(sat: Satellite, pos: GeoPos, date: Date, rewind: Boolean): SatPass {
        val quarterOrbitMin = (sat.orbitalPeriod / 4.0).toInt()
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            timeInMillis = date.time
        }
        var elevation: Double
        var maxElevation = 0.0
        var alt = 0.0
        var tcaAz = 0.0
        // rewind 1/4 of an orbit
        if (rewind) calendar.add(Calendar.MINUTE, -quarterOrbitMin)

        var satPos = sat.getPosition(pos, calendar.time.time)
        if (satPos.elevation > 0.0) {
            // move forward in 30 second intervals until the sat goes below the horizon
            do {
                calendar.add(Calendar.SECOND, 30)
                satPos = sat.getPosition(pos, calendar.time.time)
            } while (satPos.elevation > 0.0)
            // move forward 3/4 of an orbit
            calendar.add(Calendar.MINUTE, quarterOrbitMin * 3)
        }

        // find the next time sat comes above the horizon
        do {
            calendar.add(Calendar.SECOND, 60)
            satPos = sat.getPosition(pos, calendar.time.time)
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
            satPos = sat.getPosition(pos, calendar.time.time)
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
            satPos = sat.getPosition(pos, calendar.time.time)
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
            satPos = sat.getPosition(pos, calendar.time.time)
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
        return SatPass(aos, aosAz, los, losAz, tca, tcaAz, alt, elev, sat)
    }
}
