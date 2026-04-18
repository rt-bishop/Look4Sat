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
package com.rtbishop.look4sat.core.data.repository

import com.rtbishop.look4sat.core.domain.model.SatRadio
import com.rtbishop.look4sat.core.domain.predict.GeoPos
import com.rtbishop.look4sat.core.domain.predict.OrbitalObject
import com.rtbishop.look4sat.core.domain.predict.OrbitalPass
import com.rtbishop.look4sat.core.domain.predict.OrbitalPos
import com.rtbishop.look4sat.core.domain.repository.ISatelliteRepo
import com.rtbishop.look4sat.core.domain.repository.ISettingsRepo
import com.rtbishop.look4sat.core.domain.source.ILocalSource
import com.rtbishop.look4sat.core.domain.utility.round
import com.rtbishop.look4sat.core.domain.utility.toDegrees
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
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

    private val _isCalculating = MutableStateFlow(false)
    override val isCalculating: StateFlow<Boolean> = _isCalculating

    private val _satellites = MutableStateFlow<List<OrbitalObject>>(emptyList())
    override val satellites: StateFlow<List<OrbitalObject>> = _satellites

    override suspend fun getRadiosWithId(id: Int) = localStorage.getRadiosWithId(id)

    override suspend fun initRepository() = withContext(dispatcher) {
        settingsRepo.selectedIds.collect { selectedIds ->
            _satellites.update { localStorage.getEntriesWithIds(selectedIds) }
            val (_, hoursAhead, minElevation, modes) = settingsRepo.passesSettings.value
            calculatePasses(System.currentTimeMillis(), hoursAhead, minElevation, modes)
        }
    }

    override suspend fun getPosition(sat: OrbitalObject, pos: GeoPos, time: Long): OrbitalPos {
        return withContext(dispatcher) { sat.getPosition(pos, time) }
    }

    override suspend fun getTrack(sat: OrbitalObject, pos: GeoPos, start: Long, end: Long): List<OrbitalPos> {
        return withContext(dispatcher) {
            val estimatedSize = ((end - start) / 15000).toInt() + 1
            val positions = ArrayList<OrbitalPos>(estimatedSize)
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
            radios.map { transmitter ->
                transmitter.copy(
                    downlinkLow = transmitter.downlinkLow?.let { satPos.getDownlinkFreq(it) },
                    downlinkHigh = transmitter.downlinkHigh?.let { satPos.getDownlinkFreq(it) },
                    uplinkLow = transmitter.uplinkLow?.let { satPos.getUplinkFreq(it) },
                    uplinkHigh = transmitter.uplinkHigh?.let { satPos.getUplinkFreq(it) }
                )
            }
        }
    }

    override suspend fun calculatePasses(time: Long, hoursAhead: Int, minElevation: Double, modes: List<String>) {
        _isCalculating.value = true
        // Normalize to the start of the current minute so that coarse 60-second stepping
        // in getLeoPass always begins from the same phase, producing stable AOS/LOS times
        val normalizedTime = time / 60_000L * 60_000L
        val currentSatellites = _satellites.value
        withContext(dispatcher) {
            val idsWithModes = localStorage.getIdsWithModes(modes)
            val stationPos = settingsRepo.stationPosition.value
            val filteredSatellites = if (idsWithModes.isEmpty()) {
                currentSatellites
            } else {
                currentSatellites.filter { it.data.catnum in idsWithModes }
            }
            // Compute passes for each satellite in parallel
            val passLists = coroutineScope {
                filteredSatellites.map { satellite ->
                    async { satellite.getPasses(stationPos, normalizedTime, hoursAhead) }
                }.awaitAll()
            }
            // Flatten and filter in a single pass
            val timeFuture = normalizedTime + (hoursAhead * 60L * 60L * 1000L)
            val newPasses = ArrayList<OrbitalPass>()
            for (list in passLists) {
                for (pass in list) {
                    if (pass.losTime > time && pass.aosTime < timeFuture && pass.maxElevation > minElevation) {
                        newPasses.add(pass)
                    }
                }
            }
            newPasses.sortBy { it.aosTime }
            delay(1000) // Simulate loading time for better UX
            _passes.update { newPasses }
        }
        _isCalculating.value = false
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
        // rewind 1/4 of an orbit
        if (rewind) calendarTimeMillis -= quarterOrbitMin * 60L * 1000L

        // Use lightweight elevation check for coarse searching
        if (sat.getElevation(pos, calendarTimeMillis) > 0.0) {
            // move forward in 30 second intervals until the sat goes below the horizon
            do {
                calendarTimeMillis += 30 * 1000L
            } while (sat.getElevation(pos, calendarTimeMillis) > 0.0)
            // move forward 3/4 of an orbit
            calendarTimeMillis += quarterOrbitMin * 3 * 60L * 1000L
        }

        // find the next time sat comes above the horizon (coarse: 60s steps)
        do {
            calendarTimeMillis += 60L * 1000L
            elevation = sat.getElevation(pos, calendarTimeMillis)
            if (elevation > maxElevation) maxElevation = elevation
        } while (elevation < 0.0)

        // refine AOS to ~500ms precision
        calendarTimeMillis -= 60L * 1000L
        do {
            calendarTimeMillis += 500L
            elevation = sat.getElevation(pos, calendarTimeMillis)
            if (elevation > maxElevation) maxElevation = elevation
        } while (elevation < 0.0)

        // Get full position for AOS data (azimuth, altitude)
        val aosPos = sat.getFullPosition(pos, calendarTimeMillis)
        val aos = 1000 * ((aosPos.time + 500) / 1000)
        val aosAz = aosPos.azimuth.toDegrees().round(1)

        // find when sat goes below (coarse: 30s steps)
        do {
            calendarTimeMillis += 30L * 1000L
            elevation = sat.getElevation(pos, calendarTimeMillis)
            if (elevation > maxElevation) maxElevation = elevation
        } while (elevation > 0.0)

        // refine LOS to ~500ms precision
        calendarTimeMillis -= 30L * 1000L
        do {
            calendarTimeMillis += 500L
            elevation = sat.getElevation(pos, calendarTimeMillis)
            if (elevation > maxElevation) maxElevation = elevation
        } while (elevation > 0.0)

        // Get full position for LOS data (azimuth, altitude)
        val losPos = sat.getFullPosition(pos, calendarTimeMillis)
        val los = 1000 * ((losPos.time + 500) / 1000)
        val losAz = losPos.azimuth.toDegrees().round(1)

        // Get altitude at approximate TCA (max elevation)
        val tcaTime = (aos + los) / 2
        val tcaPos = sat.getFullPosition(pos, tcaTime)
        val alt = tcaPos.altitude

        val elev = maxElevation.toDegrees().round(1)
        return OrbitalPass(aos, aosAz, los, losAz, alt.toInt(), elev, sat)
    }
}
