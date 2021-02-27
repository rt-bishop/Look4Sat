package com.rtbishop.look4sat.utility

import com.github.amsacode.predict4java.GroundStationPosition
import com.github.amsacode.predict4java.SatPassTime
import com.github.amsacode.predict4java.SatPos
import com.github.amsacode.predict4java.Satellite
import java.util.*

class PassPredictor(private val satellite: Satellite, private val qth: GroundStationPosition) {
    private val oneQuarterOrbitMin = (24.0 * 60.0 / satellite.tle.meanmo / 4.0).toInt()
    private val speedOfLight = 2.99792458E8
    private val polePassed = "none"
    
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
                    lastAosDate = pass.startTime
                    passes.add(pass)
                    startDate = Date(pass.endTime.time + (oneQuarterOrbitMin * 3) * 60L * 1000L)
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
        val aosAzimuth = (satPos.azimuth / (2.0 * Math.PI) * 360.0).toInt()
        val losAzimuth = (satPos.azimuth / (2.0 * Math.PI) * 360.0).toInt()
        val maxEl = satPos.elevation / (2.0 * Math.PI) * 360.0
        return SatPassTime(startDate, endDate, polePassed, aosAzimuth, losAzimuth, maxEl)
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
        
        // test for the elevation being above the horizon
        if (satPos.elevation > 0.0) {
            // move time forward in 30 second intervals until the sat goes below the horizon
            do {
                calendar.add(Calendar.SECOND, 30)
                satPos = getSatPos(calendar.time)
            } while (satPos.elevation > 0.0)
            // move time forward 3/4 orbit
            calendar.add(Calendar.MINUTE, oneQuarterOrbitMin * 3)
        }
        
        // find the next time it comes above the horizon
        do {
            calendar.add(Calendar.SECOND, 60)
            satPos = getSatPos(calendar.time)
            elevation = satPos.elevation
            if (elevation > maxElevation) maxElevation = elevation
        } while (satPos.elevation < 0.0)
        
        // refine it to 3 seconds
        calendar.add(Calendar.SECOND, -60)
        do {
            calendar.add(Calendar.SECOND, 3)
            satPos = getSatPos(calendar.time)
            elevation = satPos.elevation
            if (elevation > maxElevation) maxElevation = elevation
        } while (satPos.elevation < 0.0)
        
        val startDate = satPos.time
        val aosAzimuth = (satPos.azimuth / (2.0 * Math.PI) * 360.0).toInt()
        
        // now find when it goes below
        do {
            calendar.add(Calendar.SECOND, 30)
            satPos = getSatPos(calendar.time)
            elevation = satPos.elevation
            if (elevation > maxElevation) maxElevation = elevation
        } while (satPos.elevation > 0.0)
        
        // refine it to 3 seconds
        calendar.add(Calendar.SECOND, -30)
        do {
            calendar.add(Calendar.SECOND, 3)
            satPos = getSatPos(calendar.time)
            elevation = satPos.elevation
            if (elevation > maxElevation) maxElevation = elevation
        } while (satPos.elevation > 0.0)
        
        val endDate = satPos.time
        val losAzimuth = (satPos.azimuth / (2.0 * Math.PI) * 360.0).toInt()
        val maxEl = maxElevation / (2.0 * Math.PI) * 360.0
        return SatPassTime(startDate, endDate, polePassed, aosAzimuth, losAzimuth, maxEl)
    }
}
