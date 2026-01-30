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
package com.rtbishop.look4sat.data.repository

import com.rtbishop.look4sat.domain.model.SatRadio
import com.rtbishop.look4sat.domain.predict.GeoPos
import com.rtbishop.look4sat.domain.predict.OrbitalObject
import com.rtbishop.look4sat.domain.predict.OrbitalPass
import com.rtbishop.look4sat.domain.predict.OrbitalPos
import com.rtbishop.look4sat.domain.repository.ISatelliteRepo
import com.rtbishop.look4sat.domain.repository.ISettingsRepo
import com.rtbishop.look4sat.domain.source.ILocalSource
import com.rtbishop.look4sat.domain.utility.round
import com.rtbishop.look4sat.domain.utility.toDegrees
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

class SatelliteRepo(
    private val dispatcher: CoroutineDispatcher,
    private val localStorage: ILocalSource,
    private val settingsRepo: ISettingsRepo
) : ISatelliteRepo {

    private val _passes = MutableStateFlow<List<OrbitalPass>>(emptyList())
    override val passes: StateFlow<List<OrbitalPass>> = _passes

    private val _satellites = MutableStateFlow<List<OrbitalObject>>(emptyList())
    override val satellites: StateFlow<List<OrbitalObject>> = _satellites

    override suspend fun getRadiosWithId(id: Int) = localStorage.getRadiosWithId(id)

    override suspend fun initRepository() = withContext(dispatcher) {
        settingsRepo.selectedIds.collect { selectedIds ->
            _satellites.update { localStorage.getEntriesWithIds(selectedIds) }
            val (hoursAhead, minElevation, modes) = settingsRepo.passesSettings.value
            val timeNow = 1000 * ((System.currentTimeMillis() + 500) / 1000)
            calculatePasses(timeNow, hoursAhead, minElevation, modes)
        }
    }

    override suspend fun getPosition(sat: OrbitalObject, pos: GeoPos, time: Long): OrbitalPos {
        return withContext(dispatcher) { sat.getPosition(pos, time) }
    }

    override suspend fun getTrack(sat: OrbitalObject, pos: GeoPos, start: Long, end: Long): List<OrbitalPos> {
        return withContext(dispatcher) {
            val positions = mutableListOf<OrbitalPos>()
            var currentTime = start
            while (currentTime < end) {
                positions.add(sat.getPosition(pos, currentTime))
                currentTime += 15000
            }
            positions
        }
    }

    override suspend fun getRadios(
        sat: OrbitalObject,
        pos: GeoPos,
        radios: List<SatRadio>,
        time: Long
    ): List<SatRadio> {
        return withContext(dispatcher) {
            val satPos = sat.getPosition(pos, time)
            val copiedList = radios.map { it.copy() }
            copiedList.forEach { transmitter ->
                transmitter.downlinkLow?.let { transmitter.downlinkLow = satPos.getDownlinkFreq(it) }
                transmitter.downlinkHigh?.let { transmitter.downlinkHigh = satPos.getDownlinkFreq(it) }
                transmitter.uplinkLow?.let { transmitter.uplinkLow = satPos.getUplinkFreq(it) }
                transmitter.uplinkHigh?.let { transmitter.uplinkHigh = satPos.getUplinkFreq(it) }
            }
            copiedList.map { it.copy() }
        }
    }

    override suspend fun processPasses(passList: List<OrbitalPass>, time: Long): List<OrbitalPass> {
        return withContext(dispatcher) {
            passList.forEach { pass ->
                if (!pass.isDeepSpace) {
                    val timeStart = pass.aosTime
                    if (time > timeStart) {
                        val deltaNow = time.minus(timeStart).toFloat()
                        val deltaTotal = pass.losTime.minus(timeStart).toFloat()
                        pass.progress = (deltaNow / deltaTotal).round(2)
                    }
                }
            }
            passList.filter { pass -> pass.progress < 1.0 }.map { it.copy() }
        }
    }

    override suspend fun calculatePasses(time: Long, hoursAhead: Int, minElevation: Double, modes: List<String>) {
        if (_satellites.value.isNotEmpty()) {
            withContext(dispatcher) {
                val newPasses = mutableListOf<OrbitalPass>()
                val idsWithModes = localStorage.getIdsWithModes(modes)
                _satellites.value.forEach { satellite ->
                    if (idsWithModes.isEmpty() || satellite.data.catnum in idsWithModes) {
                        newPasses.addAll(satellite.getPasses(settingsRepo.stationPosition.value, time, hoursAhead))
                    }
                }
                _passes.update { newPasses.filter(time, hoursAhead, minElevation) }
            }
        } else {
            _passes.update { emptyList() }
        }
    }

    private fun OrbitalObject.getPasses(pos: GeoPos, time: Long, hours: Int): List<OrbitalPass> {
        val passes = mutableListOf<OrbitalPass>()
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

    private fun List<OrbitalPass>.filter(time: Long, hoursAhead: Int, minElev: Double): List<OrbitalPass> {
        val timeFuture = time + (hoursAhead * 60L * 60L * 1000L)
        return this.filter { it.losTime > time }.filter { it.aosTime < timeFuture }
            .filter { it.maxElevation > minElev }.sortedBy { it.aosTime }
    }

    private fun getGeoPass(sat: OrbitalObject, pos: GeoPos, time: Long): OrbitalPass {
        val satPos = sat.getPosition(pos, time)
        val aos = time - 24 * 60L * 60L * 1000L
        val los = time + 24 * 60L * 60L * 1000L // val tca = (aos + los) / 2
        val az = satPos.azimuth.toDegrees().round(1)
        val elev = satPos.elevation.toDegrees().round(1)
        val alt = satPos.altitude
        return OrbitalPass(aos, az, los, az, alt.toInt(), elev, sat)
    }

    private fun getLeoPass(sat: OrbitalObject, pos: GeoPos, time: Long, rewind: Boolean): OrbitalPass {
        val quarterOrbitMin = (sat.data.orbitalPeriod / 4.0).toInt()
        var calendarTimeMillis = time
        var elevation: Double
        var maxElevation = 0.0
        var alt = 0.0 // var tcaAz = 0.0
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
                alt = satPos.altitude // tcaAz = satPos.azimuth.toDegrees()
            }
        } while (satPos.elevation < 0.0)

        // refine to 1 second
        calendarTimeMillis += -60L * 1000L
        do {
            calendarTimeMillis += 1L * 500L
            satPos = sat.getPosition(pos, calendarTimeMillis)
            elevation = satPos.elevation
            if (elevation > maxElevation) {
                maxElevation = elevation
                alt = satPos.altitude // tcaAz = satPos.azimuth.toDegrees()
            }
        } while (satPos.elevation < 0.0)

        val aos = 1000 * ((satPos.time + 500) / 1000)
        val aosAz = satPos.azimuth.toDegrees().round(1)

        // find when sat goes below
        do {
            calendarTimeMillis += 30L * 1000L
            satPos = sat.getPosition(pos, calendarTimeMillis)
            elevation = satPos.elevation
            if (elevation > maxElevation) {
                maxElevation = elevation
                alt = satPos.altitude // tcaAz = satPos.azimuth.toDegrees()
            }
        } while (satPos.elevation > 0.0)

        // refine to 1 second
        calendarTimeMillis += -30L * 1000L
        do {
            calendarTimeMillis += 1L * 500L
            satPos = sat.getPosition(pos, calendarTimeMillis)
            elevation = satPos.elevation
            if (elevation > maxElevation) {
                maxElevation = elevation
                alt = satPos.altitude // tcaAz = satPos.azimuth.toDegrees()
            }
        } while (satPos.elevation > 0.0)

        val los = 1000 * ((satPos.time + 500) / 1000) // val tca = (aos + los) / 2
        val losAz = satPos.azimuth.toDegrees().round(1)
        val elev = maxElevation.toDegrees().round(1)
        return OrbitalPass(aos, aosAz, los, losAz, alt.toInt(), elev, sat)
    }
}
