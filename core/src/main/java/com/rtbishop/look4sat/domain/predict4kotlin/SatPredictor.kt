package com.rtbishop.look4sat.domain.predict4kotlin

import java.util.*
import kotlin.math.*

const val speedOfLight = 2.99792458E8
const val earthRadiusKm = 6378.16

fun Satellite.getQuarterOrbitMin(): Int {
    return (24.0 * 60.0 / this.tle.meanmo / 4.0).toInt()
}

fun SatPos.getRangeCircle(): List<Position> {
    val positions = mutableListOf<Position>()
    val lat = this.latitude
    val lon = this.longitude
    // rangeCircleRadiusKm
    // earthRadiusKm * acos(earthRadiusKm / (earthRadiusKm + satPos.altitude))
    val beta = acos(earthRadiusKm / (earthRadiusKm + this.altitude))
    var tempAzimuth = 0
    while (tempAzimuth < 360) {
        val azimuth = tempAzimuth / 360.0 * 2.0 * Math.PI
        var rangelat = asin(sin(lat) * cos(beta) + cos(azimuth) * sin(beta) * cos(lat))
        val num = (cos(beta) - (sin(lat) * sin(rangelat)))
        val den = cos(lat) * cos(rangelat)
        var rangelon = if (tempAzimuth == 0 && (beta > ((Math.PI / 2.0) - lat))) {
            lon + Math.PI
        } else if (tempAzimuth == 180 && (beta > ((Math.PI / 2.0) - lat))) {
            lon + Math.PI
        } else if (abs(num / den) > 1.0) {
            lon
        } else {
            if ((180 - tempAzimuth) >= 0) {
                lon - acos(num / den)
            } else {
                lon + acos(num / den)
            }
        }
        while (rangelon < 0.0) rangelon += Math.PI * 2.0
        while (rangelon > Math.PI * 2.0) rangelon -= Math.PI * 2.0
        rangelat = Math.toDegrees(rangelat)
        rangelon = Math.toDegrees(rangelon)
        positions.add(Position(rangelat, rangelon))
        tempAzimuth += 1
    }
    return positions
}

fun SatPos.getDownlinkFreq(freq: Long): Long {
    return (freq.toDouble() * (speedOfLight - this.rangeRate * 1000.0) / speedOfLight).toLong()
}

fun SatPos.getUplinkFreq(freq: Long): Long {
    return (freq.toDouble() * (speedOfLight + this.rangeRate * 1000.0) / speedOfLight).toLong()
}

fun Satellite.getSatPos(date: Date): SatPos {
    return this.getPosition(stationPos, date)
}

fun Satellite.getPositions(date: Date, stepSec: Int, minBefore: Int, orbits: Double): List<SatPos> {
    val positions = mutableListOf<SatPos>()
    val orbitalPeriod = 24 * 60 / this.tle.meanmo
    val endDate = Date(date.time + (orbitalPeriod * orbits * 60L * 1000L).toLong())
    val startDate = Date(date.time - minBefore * 60L * 1000L)
    var currentDate = startDate
    while (currentDate.before(endDate)) {
        positions.add(getSatPos(currentDate))
        currentDate = Date(currentDate.time + stepSec * 1000)
    }
    return positions
}

fun Satellite.getPasses(refDate: Date, hoursAhead: Int, windBack: Boolean): List<SatPass> {
    val passes = mutableListOf<SatPass>()
    val oneQuarterOrbitMin = this.getQuarterOrbitMin()
    val endDate = Date(refDate.time + hoursAhead * 60L * 60L * 1000L)
    var startDate = refDate
    var shouldWindBack = windBack
    var lastAosDate: Date
    var count = 0
    if (this.willBeSeen(stationPos)) {
        if (this.tle.isDeepspace) {
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

private fun Satellite.nextDeepSpacePass(refDate: Date): SatPass {
    val satPos = getSatPos(refDate)
    val id = this.tle.catnum
    val name = this.tle.name
    val isDeep = this.tle.isDeepspace
    val aos = Date(refDate.time - 24 * 60L * 60L * 1000L).time
    val los = Date(refDate.time + 24 * 60L * 60L * 1000L).time
    val tca = Date((aos + los) / 2).time
    val az = Math.toDegrees(satPos.azimuth)
    val elev = Math.toDegrees(satPos.elevation)
    val alt = satPos.altitude
    return SatPass(id, name, isDeep, aos, az, los, az, tca, az, alt, elev, this)
}

private fun Satellite.nextNearEarthPass(refDate: Date, windBack: Boolean = false): SatPass {
    val oneQuarterOrbitMin = this.getQuarterOrbitMin()
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        timeInMillis = refDate.time
    }
    val id = this.tle.catnum
    val name = this.tle.name
    val isDeep = this.tle.isDeepspace

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
