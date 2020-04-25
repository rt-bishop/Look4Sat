/**
 * predict4java: An SDP4 / SGP4 library for satellite orbit predictions
 *
 * Copyright (C)  2004-2010  David A. B. Johnson, G4DPZ.
 *
 * This class is a Java port of one of the core elements of
 * the Predict program, Copyright John A. Magliacane,
 * KD2BD 1991-2003: http://www.qsl.net/kd2bd/predict.html
 *
 * Dr. T.S. Kelso is the author of the SGP4/SDP4 orbital models,
 * originally written in Fortran and Pascal, and released into the
 * public domain through his website (http://www.celestrak.com/).
 * Neoklis Kyriazis, 5B4AZ, later re-wrote Dr. Kelso's code in C,
 * and released it under the GNU GPL in 2002.
 * PREDICT's core is based on 5B4AZ's code translation efforts.
 *
 * Author: David A. B. Johnson, G4DPZ <dave></dave>@g4dpz.me.uk>
 *
 * Comments, questions and bugreports should be submitted via
 * http://sourceforge.net/projects/websat/
 * More details can be found at the project home page:
 *
 * http://websat.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, visit http://www.fsf.org/
 */
package com.rtbishop.look4sat.utility

import com.github.amsacode.predict4java.*
import java.util.*

class PassPredictor(private val tle: TLE, private val qth: GroundStationPosition) {
    private val sat: Satellite = SatelliteFactory.createSatellite(tle)
    private val south = "south"
    private val north = "north"
    private val deadSpotNone = "none"
    private val speedOfLight = 2.99792458E8
    private val twoPi = Math.PI * 2.0
    private val timeZone: TimeZone = TimeZone.getTimeZone("UTC")
    private var iterationCount = 0

    fun getDownlinkFreq(freq: Long, date: Date): Long {
        val cal = Calendar.getInstance(timeZone).apply {
            clear()
            timeInMillis = date.time
        }
        val satPos = getSatPos(cal.time)
        val rangeRate = satPos.rangeRate
        return (freq.toDouble() * (speedOfLight - rangeRate * 1000.0) / speedOfLight).toLong()
    }

    fun getUplinkFreq(freq: Long, date: Date): Long {
        val cal = Calendar.getInstance(timeZone).apply {
            clear()
            timeInMillis = date.time
        }
        val satPos = getSatPos(cal.time)
        val rangeRate = satPos.rangeRate
        return (freq.toDouble() * (speedOfLight + rangeRate * 1000.0) / speedOfLight).toLong()
    }

    fun getSatPos(time: Date): SatPos {
        iterationCount++
        return sat.getPosition(qth, time)
    }

    fun getPositions(
        referenceDate: Date,
        incrementSeconds: Int,
        minutesBefore: Int,
        minutesAfter: Int
    ): List<SatPos> {
        val positions: MutableList<SatPos> = ArrayList()
        val endDateDate = Date(referenceDate.time + minutesAfter * 60L * 1000L)
        var trackDate = Date(referenceDate.time - minutesBefore * 60L * 1000L)

        while (trackDate.before(endDateDate)) {
            positions.add(getSatPos(trackDate))
            trackDate = Date(trackDate.time + incrementSeconds * 1000)
        }

        return positions
    }

    fun getPasses(start: Date, hoursAhead: Int, windBack: Boolean): List<SatPassTime> {
        iterationCount = 0

        val satellite = SatelliteFactory.createSatellite(tle)
        val passes: MutableList<SatPassTime> = ArrayList()
        val trackEndDate = Date(start.time + hoursAhead * 60L * 60L * 1000L)
        var trackStartDate = start
        var windBackTime = windBack
        var lastAOS: Date
        var count = 0

        if (satellite.willBeSeen(qth)) {
            if (tle.isDeepspace) passes.add(nextGeoSatPass(start))
            else {
                do {
                    if (count > 0) windBackTime = false
                    val pass = nextSatPass(trackStartDate, windBackTime)
                    lastAOS = pass.startTime
                    passes.add(pass)
                    trackStartDate =
                        Date(pass.endTime.time + threeQuarterOrbitMinutes() * 60L * 1000L)
                    count++
                } while (lastAOS < trackEndDate)
            }
        }

        return passes
    }

    private fun nextGeoSatPass(date: Date): SatPassTime {
        val aosAzimuth: Int
        val losAzimuth: Int
        val tca: Date
        val polePassed: String
        val cal = Calendar.getInstance(timeZone).apply {
            clear()
            timeInMillis = date.time
        }
        val satPos = getSatPos(cal.time)

        aosAzimuth = (satPos.azimuth / (2.0 * Math.PI) * 360.0).toInt()
        losAzimuth = (satPos.azimuth / (2.0 * Math.PI) * 360.0).toInt()
        tca = satPos.time
        polePassed = getPolePassed(satPos, satPos)

        cal.add(Calendar.HOUR, -12)
        val startDate = cal.time
        cal.add(Calendar.HOUR, 24)
        val endDate = cal.time

        return SatPassTime(
            startDate, endDate, tca, polePassed, aosAzimuth,
            losAzimuth, satPos.elevation / (2.0 * Math.PI) * 360.0
        )
    }

    private fun nextSatPass(date: Date, windBack: Boolean = false): SatPassTime {
        val aosAzimuth: Int
        val losAzimuth: Int
        var maxElevation = 0.0
        var elevation: Double
        var prevPos: SatPos
        var polePassed = deadSpotNone

        // get the current position
        val cal = Calendar.getInstance(timeZone).apply {
            clear()
            timeInMillis = date.time
        }

        // wind back time 1/4 of an orbit
        if (windBack) {
            val meanMotion = tle.meanmo
            cal.add(Calendar.MINUTE, (-24.0 * 60.0 / meanMotion / 4.0).toInt())
        }
        var satPos = getSatPos(cal.time)

        // test for the elevation being above the horizon
        if (satPos.elevation > 0.0) {
            // move time forward in 30 second intervals until the sat goes below the horizon
            do {
                satPos = getPosition(cal, 60)
            } while (satPos.elevation > 0.0)
            // move time forward 3/4 orbit
            cal.add(Calendar.MINUTE, threeQuarterOrbitMinutes())
        }

        // now find the next time it comes above the horizon
        do {
            satPos = getPosition(cal, 60)
            elevation = satPos.elevation
            if (elevation > maxElevation) {
                maxElevation = elevation
            }
        } while (satPos.elevation < 0.0)

        // refine it to 5 seconds
        cal.add(Calendar.SECOND, -60)
        do {
            satPos = getPosition(cal, 5)
            elevation = satPos.elevation
            if (elevation > maxElevation) {
                maxElevation = elevation
            }
            prevPos = satPos
        } while (satPos.elevation < 0.0)
        val startDate = satPos.time
        aosAzimuth = (satPos.azimuth / (2.0 * Math.PI) * 360.0).toInt()

        // now find when it goes below
        do {
            satPos = getPosition(cal, 30)
            val currPolePassed = getPolePassed(prevPos, satPos)
            if (currPolePassed != deadSpotNone) {
                polePassed = currPolePassed
            }
            elevation = satPos.elevation
            if (elevation > maxElevation) {
                maxElevation = elevation
            }
            prevPos = satPos
        } while (satPos.elevation > 0.0)

        // refine it to 5 seconds
        cal.add(Calendar.SECOND, -30)
        do {
            satPos = getPosition(cal, 5)
            elevation = satPos.elevation
            if (elevation > maxElevation) {
                maxElevation = elevation
            }
        } while (satPos.elevation > 0.0)

        val endDate = satPos.time
        losAzimuth = (satPos.azimuth / (2.0 * Math.PI) * 360.0).toInt()
        val tca = Date(startDate.time + (endDate.time - startDate.time) / 2)

        return SatPassTime(
            startDate, endDate, tca, polePassed, aosAzimuth,
            losAzimuth, maxElevation / (2.0 * Math.PI) * 360.0
        )
    }

    private fun getPosition(cal: Calendar, offSet: Int): SatPos {
        cal.add(Calendar.SECOND, offSet)
        return getSatPos(cal.time)
    }

    private fun getPolePassed(prevPos: SatPos, satPos: SatPos): String {
        var polePassed = deadSpotNone
        val az1 = prevPos.azimuth / twoPi * 360.0
        val az2 = satPos.azimuth / twoPi * 360.0
        if (az1 > az2) {
            // we may be moving from 350 or greater through north
            if (az1 > 350 && az2 < 10) polePassed = north
            else {
                // we may be moving from 190 or greater through south
                if (az1 > 180 && az2 < 180) polePassed = south
            }
        } else {
            // we may be moving from 10 or less through north
            if (az1 < 10 && az2 > 350) polePassed = north
            else {
                // we may be moving from 170 or more through south
                if (az1 < 180 && az2 > 180) polePassed = south
            }
        }
        return polePassed
    }

    private fun threeQuarterOrbitMinutes(): Int {
        return (24.0 * 60.0 / tle.meanmo * 0.75).toInt()
    }
}
