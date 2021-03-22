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
package com.rtbishop.look4sat.utility

import com.github.amsacode.predict4java.GroundStationPosition
import com.github.amsacode.predict4java.SatPos
import com.github.amsacode.predict4java.Satellite
import com.rtbishop.look4sat.data.model.SatPassTime
import java.util.*

class PassPredictor(private val satellite: Satellite, private val qth: GroundStationPosition) {

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
        return satellite.getPosition(qth, date)
    }

    fun getPositions(refDate: Date, stepSeconds: Int, minBefore: Int, minAfter: Int): List<SatPos> {
        val positions = mutableListOf<SatPos>()
        val endDate = Date(refDate.time + minAfter * 60L * 1000L)
        val startDate = Date(refDate.time - minBefore * 60L * 1000L)
        var currentDate = startDate
        while (currentDate.before(endDate)) {
            positions.add(getSatPos(currentDate))
            currentDate = Date(currentDate.time + stepSeconds * 1000)
        }
        return positions
    }
    
    fun getPasses(refDate: Date, hoursAhead: Int, windBack: Boolean): List<SatPassTime> {
        val passes = mutableListOf<SatPassTime>()
        val endDate = Date(refDate.time + hoursAhead * 60L * 60L * 1000L)
        var startDate = refDate
        var shouldWindBack = windBack
        var lastAosDate: Date
        var count = 0
        if (satellite.willBeSeen(qth)) {
            if (satellite.tle.isDeepspace) {
                passes.add(nextDeepSpacePass(refDate))
            } else {
                do {
                    if (count > 0) shouldWindBack = false
                    val pass = nextNearEarthPass(startDate, shouldWindBack)
                    lastAosDate = pass.getStartTime()
                    passes.add(pass)
                    startDate =
                        Date(pass.getEndTime().time + (oneQuarterOrbitMin * 3) * 60L * 1000L)
                    count++
                } while (lastAosDate < endDate)
            }
        }
        return passes
    }
    
    private fun nextDeepSpacePass(refDate: Date): SatPassTime {
        val satPos = getSatPos(refDate)
        val startDate = Date(refDate.time - 24 * 60L * 60L * 1000L)
        val endDate = Date(refDate.time + 24 * 60L * 60L * 1000L)
        val azimuth = Math.toDegrees(satPos.azimuth).toInt()
        val maxEl = Math.toDegrees(satPos.elevation)
        return SatPassTime(startDate, endDate, azimuth, azimuth, maxEl)
    }
    
    private fun nextNearEarthPass(refDate: Date, windBack: Boolean = false): SatPassTime {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            timeInMillis = refDate.time
        }
        var maxElevation = 0.0
        var elevation: Double
        
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
            if (elevation > maxElevation) maxElevation = elevation
        } while (satPos.elevation < 0.0)

        // refine to 3 seconds
        calendar.add(Calendar.SECOND, -60)
        do {
            calendar.add(Calendar.SECOND, 3)
            satPos = getSatPos(calendar.time)
            elevation = satPos.elevation
            if (elevation > maxElevation) maxElevation = elevation
        } while (satPos.elevation < 0.0)

        val startDate = satPos.time
        val aosAzimuth = Math.toDegrees(satPos.azimuth).toInt()

        // find when sat goes below
        do {
            calendar.add(Calendar.SECOND, 30)
            satPos = getSatPos(calendar.time)
            elevation = satPos.elevation
            if (elevation > maxElevation) maxElevation = elevation
        } while (satPos.elevation > 0.0)

        // refine to 3 seconds
        calendar.add(Calendar.SECOND, -30)
        do {
            calendar.add(Calendar.SECOND, 3)
            satPos = getSatPos(calendar.time)
            elevation = satPos.elevation
            if (elevation > maxElevation) maxElevation = elevation
        } while (satPos.elevation > 0.0)

        val endDate = satPos.time
        val losAzimuth = Math.toDegrees(satPos.azimuth).toInt()
        val maxEl = Math.toDegrees(maxElevation)
        return SatPassTime(startDate, endDate, aosAzimuth, losAzimuth, maxEl)
    }
}
