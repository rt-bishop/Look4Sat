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
package com.rtbishop.look4sat.domain.predict

import com.rtbishop.look4sat.domain.ISatelliteManager
import com.rtbishop.look4sat.domain.model.SatRadio
import com.rtbishop.look4sat.utility.toDegrees
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext

class SatelliteManager(private val defaultDispatcher: CoroutineDispatcher) : ISatelliteManager {

    private var passes = listOf<SatPass>()
    private val _calculatedPasses = MutableSharedFlow<List<SatPass>>(replay = 1)
    override val calculatedPasses: SharedFlow<List<SatPass>> = _calculatedPasses

    override fun getPasses(): List<SatPass> = passes

    override suspend fun getPosition(sat: Satellite, pos: GeoPos, time: Long): SatPos {
        return withContext(defaultDispatcher) { sat.getPosition(pos, time) }
    }

    override suspend fun getTrack(
        sat: Satellite,
        pos: GeoPos,
        start: Long,
        end: Long
    ): List<SatPos> {
        return withContext(defaultDispatcher) {
            val positions = mutableListOf<SatPos>()
            var currentTime = start
            while (currentTime < end) {
                positions.add(sat.getPosition(pos, currentTime))
                currentTime += 15000
            }
            positions
        }
    }

    override suspend fun processRadios(
        sat: Satellite,
        pos: GeoPos,
        radios: List<SatRadio>,
        time: Long
    ): List<SatRadio> {
        return withContext(defaultDispatcher) {
            val satPos = sat.getPosition(pos, time)
            val copiedList = radios.map { it.copy() }
            copiedList.forEach { transmitter ->
                transmitter.downlink?.let {
                    transmitter.downlink = satPos.getDownlinkFreq(it)
                }
                transmitter.uplink?.let {
                    transmitter.uplink = satPos.getUplinkFreq(it)
                }
            }
            copiedList.map { it.copy() }
        }
    }

    override suspend fun processPasses(passList: List<SatPass>, time: Long): List<SatPass> {
        return withContext(defaultDispatcher) {
            passList.forEach { pass ->
                if (!pass.isDeepSpace) {
                    val timeStart = pass.aosTime
                    if (time > timeStart) {
                        val deltaNow = time.minus(timeStart).toFloat()
                        val deltaTotal = pass.losTime.minus(timeStart).toFloat()
                        pass.progress = ((deltaNow / deltaTotal) * 100).toInt()
                    }
                }
            }
            passList.filter { pass -> pass.progress < 100 }.map { it.copy() }
        }
    }

    override suspend fun calculatePasses(
        satList: List<Satellite>,
        pos: GeoPos,
        time: Long,
        hoursAhead: Int,
        minElevation: Double
    ) {
        if (satList.isEmpty()) {
            val newPasses = emptyList<SatPass>()
            passes = newPasses
            _calculatedPasses.emit(newPasses)
        } else {
            withContext(defaultDispatcher) {
                val allPasses = mutableListOf<SatPass>()
                satList.forEach { satellite ->
                    allPasses.addAll(satellite.getPasses(pos, time, hoursAhead))
                }
                val newPasses = allPasses.filter(time, hoursAhead, minElevation)
                passes = newPasses
                _calculatedPasses.emit(newPasses)
            }
        }
    }

    private fun Satellite.getPasses(pos: GeoPos, time: Long, hours: Int): List<SatPass> {
        val passes = mutableListOf<SatPass>()
        val endDate = time + hours * 60L * 60L * 1000L
        val quarterOrbitMin = (this.data.orbitalPeriod / 4.0).toInt()
        var startDate = time
        var shouldRewind = true
        var lastAosDate: Long
        var count = 0
        if (this.willBeSeen(pos)) {
            if (this.data.isDeepSpace) {
                passes.add(getGeoPass(this, pos, time))
            } else {
                do {
                    if (count > 0) shouldRewind = false
                    val pass = getLeoPass(this, pos, startDate, shouldRewind)
                    lastAosDate = pass.aosTime
                    passes.add(pass)
                    startDate = pass.losTime + (quarterOrbitMin * 3) * 60L * 1000L
                    count++
                } while (lastAosDate < endDate)
            }
        }
        return passes
    }

    private fun List<SatPass>.filter(time: Long, hoursAhead: Int, minElev: Double): List<SatPass> {
        val timeFuture = time + (hoursAhead * 60L * 60L * 1000L)
        return this.filter { it.losTime > time }
            .filter { it.aosTime < timeFuture }
            .filter { it.maxElevation > minElev }
            .sortedBy { it.aosTime }
    }

    private fun getGeoPass(sat: Satellite, pos: GeoPos, time: Long): SatPass {
        val satPos = sat.getPosition(pos, time)
        val aos = time - 24 * 60L * 60L * 1000L
        val los = time + 24 * 60L * 60L * 1000L
        val tca = (aos + los) / 2
        val az = satPos.azimuth.toDegrees()
        val elev = satPos.elevation.toDegrees()
        val alt = satPos.altitude
        return SatPass(aos, az, los, az, tca, az, alt, elev, sat)
    }

    private fun getLeoPass(sat: Satellite, pos: GeoPos, time: Long, rewind: Boolean): SatPass {
        val quarterOrbitMin = (sat.data.orbitalPeriod / 4.0).toInt()
        var calendarTimeMillis = time
        var elevation: Double
        var maxElevation = 0.0
        var alt = 0.0
        var tcaAz = 0.0
        // rewind 1/4 of an orbit
        if (rewind) calendarTimeMillis += -quarterOrbitMin * 60L * 1000L

        var satPos = sat.getPosition(pos, calendarTimeMillis)
        if (satPos.elevation > 0.0) {
            // move forward in 30 second intervals until the sat goes below the horizon
            do {
                calendarTimeMillis += 30 * 1000L
                satPos = sat.getPosition(pos, calendarTimeMillis)
            } while (satPos.elevation > 0.0)
            // move forward 3/4 of an orbit
            calendarTimeMillis += quarterOrbitMin * 3 * 60L * 1000L
        }

        // find the next time sat comes above the horizon
        do {
            calendarTimeMillis += 60L * 1000L
            satPos = sat.getPosition(pos, calendarTimeMillis)
            elevation = satPos.elevation
            if (elevation > maxElevation) {
                maxElevation = elevation
                alt = satPos.altitude
                tcaAz = satPos.azimuth.toDegrees()
            }
        } while (satPos.elevation < 0.0)

        // refine to 3 seconds
        calendarTimeMillis += -60L * 1000L
        do {
            calendarTimeMillis += 3L * 1000L
            satPos = sat.getPosition(pos, calendarTimeMillis)
            elevation = satPos.elevation
            if (elevation > maxElevation) {
                maxElevation = elevation
                alt = satPos.altitude
                tcaAz = satPos.azimuth.toDegrees()
            }
        } while (satPos.elevation < 0.0)

        val aos = satPos.time
        val aosAz = satPos.azimuth.toDegrees()

        // find when sat goes below
        do {
            calendarTimeMillis += 30L * 1000L
            satPos = sat.getPosition(pos, calendarTimeMillis)
            elevation = satPos.elevation
            if (elevation > maxElevation) {
                maxElevation = elevation
                alt = satPos.altitude
                tcaAz = satPos.azimuth.toDegrees()
            }
        } while (satPos.elevation > 0.0)

        // refine to 3 seconds
        calendarTimeMillis += -30L * 1000L
        do {
            calendarTimeMillis += 3L * 1000L
            satPos = sat.getPosition(pos, calendarTimeMillis)
            elevation = satPos.elevation
            if (elevation > maxElevation) {
                maxElevation = elevation
                alt = satPos.altitude
                tcaAz = satPos.azimuth.toDegrees()
            }
        } while (satPos.elevation > 0.0)

        val los = satPos.time
        val losAz = satPos.azimuth.toDegrees()
        val tca = (aos + los) / 2
        val elev = maxElevation.toDegrees()
        return SatPass(aos, aosAz, los, losAz, tca, tcaAz, alt, elev, sat)
    }
}
