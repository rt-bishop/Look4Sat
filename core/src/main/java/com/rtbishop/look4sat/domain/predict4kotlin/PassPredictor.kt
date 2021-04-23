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
package com.rtbishop.look4sat.domain.predict4kotlin

import java.util.*

class PassPredictor(private val satellite: Satellite, private val gsp: StationPosition) {

    private val oneQuarterOrbitMin = (24.0 * 60.0 / satellite.tle.meanmo / 4.0).toInt()
    private val speedOfLight = 2.99792458E8

    fun getDownlinkFreq(freq: Long, date: Date): Long {
        val rangeRate = getSatPos(date).rangeRate
        return (freq.toDouble() * (speedOfLight - rangeRate * 1000.0) / speedOfLight).toLong()
    }

    fun getUplinkFreq(freq: Long, date: Date): Long {
        val rangeRate = getSatPos(date).rangeRate
        return (freq.toDouble() * (speedOfLight + rangeRate * 1000.0) / speedOfLight).toLong()
    }

    fun getSatPos(date: Date): SatPos {
        return satellite.getPosition(gsp, date)
    }

    fun getPositions(refDate: Date, stepSec: Int, minBefore: Int, orbits: Double): List<SatPos> {
        val positions = mutableListOf<SatPos>()
        val orbitalPeriod = 24 * 60 / satellite.tle.meanmo
        val endDate = Date(refDate.time + (orbitalPeriod * orbits * 60L * 1000L).toLong())
        val startDate = Date(refDate.time - minBefore * 60L * 1000L)
        var currentDate = startDate
        while (currentDate.before(endDate)) {
            positions.add(getSatPos(currentDate))
            currentDate = Date(currentDate.time + stepSec * 1000)
        }
        return positions
    }

    fun getPasses(refDate: Date, hoursAhead: Int, windBack: Boolean): List<SatPass> {
        val passes = mutableListOf<SatPass>()
        val endDate = Date(refDate.time + hoursAhead * 60L * 60L * 1000L)
        var startDate = refDate
        var shouldWindBack = windBack
        var lastAosDate: Date
        var count = 0
        if (satellite.willBeSeen(gsp)) {
            if (satellite.tle.isDeepspace) {
                passes.add(nextDeepSpacePass(refDate))
            } else {
                do {
                    if (count > 0) shouldWindBack = false
                    val pass = nextNearEarthPass(startDate, shouldWindBack)
                    lastAosDate = pass.aosDate
                    passes.add(pass)
                    startDate =
                        Date(pass.losDate.time + (oneQuarterOrbitMin * 3) * 60L * 1000L)
                    count++
                } while (lastAosDate < endDate)
            }
        }
        return passes
    }

    private fun nextDeepSpacePass(refDate: Date): SatPass {
        val satPos = getSatPos(refDate)
        val id = satellite.tle.catnum
        val name = satellite.tle.name
        val isDeep = satellite.tle.isDeepspace
        val aos = Date(refDate.time - 24 * 60L * 60L * 1000L).time
        val los = Date(refDate.time + 24 * 60L * 60L * 1000L).time
        val tca = Date((aos + los) / 2).time
        val az = Math.toDegrees(satPos.azimuth)
        val elev = Math.toDegrees(satPos.elevation)
        val alt = satPos.altitude
        return SatPass(id, name, isDeep, aos, az, los, az, tca, az, alt, elev, this)
    }

    private fun nextNearEarthPass(refDate: Date, windBack: Boolean = false): SatPass {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            timeInMillis = refDate.time
        }
        val id = satellite.tle.catnum
        val name = satellite.tle.name
        val isDeep = satellite.tle.isDeepspace

        var elevation: Double
        var maxElevation = 0.0
        var alt = 0.0
        var tcaAz = 0.0

        // wind back time 1/4 of an orbit
        if (windBack) calendar.add(Calendar.MINUTE, -oneQuarterOrbitMin)
        var satPos = getSatPos(calendar.time)

        if (satPos.elevation > 0.0) {
            // move forward in 30 second intervals until the sat goes below the horizon
            do {
                calendar.add(Calendar.SECOND, 30)
                satPos = getSatPos(calendar.time)
            } while (satPos.elevation > 0.0)
            // move forward 3/4 of an orbit
            calendar.add(Calendar.MINUTE, oneQuarterOrbitMin * 3)
        }

        // find the next time sat comes above the horizon
        do {
            calendar.add(Calendar.SECOND, 60)
            satPos = getSatPos(calendar.time)
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
            satPos = getSatPos(calendar.time)
            elevation = satPos.elevation
            if (elevation > maxElevation) {
                maxElevation = elevation
                alt = satPos.altitude
                tcaAz = Math.toDegrees(satPos.azimuth)
            }
        } while (satPos.elevation < 0.0)

        val aos = satPos.time.time
        val aosAz = Math.toDegrees(satPos.azimuth)

        // find when sat goes below
        do {
            calendar.add(Calendar.SECOND, 30)
            satPos = getSatPos(calendar.time)
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
            satPos = getSatPos(calendar.time)
            elevation = satPos.elevation
            if (elevation > maxElevation) {
                maxElevation = elevation
                alt = satPos.altitude
                tcaAz = Math.toDegrees(satPos.azimuth)
            }
        } while (satPos.elevation > 0.0)

        val los = satPos.time.time
        val losAz = Math.toDegrees(satPos.azimuth)
        val tca = Date((aos + los) / 2).time
        val elev = Math.toDegrees(maxElevation)
        return SatPass(id, name, isDeep, aos, aosAz, los, losAz, tca, tcaAz, alt, elev, this)
    }
}
