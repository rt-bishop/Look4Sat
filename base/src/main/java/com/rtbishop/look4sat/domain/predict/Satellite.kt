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

import kotlin.math.*

const val ASTRONOMICAL_UNIT = 1.49597870691E8
const val DEG2RAD = 0.017453292519943295
const val RAD2DEG = 57.29577951308232
const val EARTH_RADIUS = 6378.137
const val EPSILON = 1.0E-12
const val FLAT_FACT = 3.35281066474748E-3
const val J3_HARMONIC = -2.53881E-6
const val MIN_PER_DAY = 1.44E3
const val SEC_PER_DAY = 8.6400E4
const val SOLAR_RADIUS = 6.96000E5
const val SPEED_OF_LIGHT = 2.99792458E8
const val PI = 3.141592653589793
const val PI_2 = PI / 2.0
const val TWO_PI = PI * 2.0
const val TWO_THIRDS = 2.0 / 3.0
const val CK2 = 5.413079E-4
const val CK4 = 6.209887E-7
const val XKE = 7.43669161E-2

abstract class Satellite(val data: OrbitalData) {

    private val position = Vector4()
    private val velocity = Vector4()
    private var satPos = SatPos()
    private var eclipseDepth = 0.0
    private var gsPosTheta = 0.0
    private var julUTC = 0.0
    private var perigee = 0.0
    var qoms24 = 0.0
    var s4 = 0.0

    internal fun willBeSeen(pos: GeoPos): Boolean {
        return if (data.meanmo < 1e-8) false
        else {
            val sma = 331.25 * exp(ln(MIN_PER_DAY / data.meanmo) * (2.0 / 3.0))
            val apogee = sma * (1.0 + data.eccn) - EARTH_RADIUS
            var lin = data.incl
            if (lin >= 90.0) lin = 180.0 - lin
            acos(EARTH_RADIUS / (apogee + EARTH_RADIUS)) + lin * DEG2RAD > abs(pos.lat * DEG2RAD)
        }
    }

    internal fun getPosition(pos: GeoPos, time: Long): SatPos {
        satPos = SatPos()
        // Date/time at which the position and velocity were calculated
        julUTC = calcCurrentDaynum(time) + 2444238.5
        // Convert satellite's epoch time to Julian and calculate time since epoch in minutes
        val julEpoch = juliandDateOfEpoch(data.epoch)
        val tsince = (julUTC - julEpoch) * MIN_PER_DAY
        calculateSDP4orSGP4(tsince)
        // Scale position and velocity vectors to km and km/sec
        convertSatState(position, velocity)
        // Calculate velocity of satellite
        magnitude(velocity)
        val squintVector = Vector4()
        // Angles in rads, dist in km, vel in km/S. Calculate sat Az, El, Range and Range-rate.
        calculateObs(julUTC, position, velocity, pos, squintVector)
        calculateLatLonAlt(julUTC)
        satPos.time = time
        satPos.eclipsed = isEclipsed()
        satPos.eclipseDepth = eclipseDepth
        return satPos
    }

    private fun calcCurrentDaynum(now: Long): Double {
        val then = 315446400000 // time in millis on 31Dec79 00:00:00 UTC (daynum 0)
        return (now - then) / 1000.0 / 60.0 / 60.0 / 24.0
    }

    private fun juliandDateOfEpoch(epoch: Double): Double {
        var year = floor(epoch * 1E-3)
        val day = (epoch * 1E-3 - year) * 1000.0
        year = if (year < 57) year + 2000 else year + 1900
        return julianDateOfYear(year) + day
    }

    internal fun julianDateOfYear(theYear: Double): Double {
        val aYear = theYear - 1
        var i = floor(aYear / 100).toLong()
        val a = i
        i = a / 4
        val b = 2 - a + i
        i = floor(365.25 * aYear).toLong()
        i += (30.6001 * 14).toLong()
        return i + 1720994.5 + b
    }

    private fun calculateSDP4orSGP4(tsince: Double) {
        if (data.isDeepSpace) (this as DeepSpaceSat).calculateSDP4(tsince)
        else (this as NearEarthSat).calculateSGP4(tsince)
    }

    // Converts the sat position and velocity vectors to km and km/sec
    private fun convertSatState(pos: Vector4, vel: Vector4) {
        scaleVector(EARTH_RADIUS, pos)
        scaleVector(EARTH_RADIUS * MIN_PER_DAY / SEC_PER_DAY, vel)
    }

    // Calculates the topocentric coordinates of the object with ECI pos and vel at time
    private fun calculateObs(
        julianUTC: Double,
        positionVector: Vector4,
        velocityVector: Vector4,
        gsPos: GeoPos,
        squintVector: Vector4
    ) {
        val obsPos = Vector4()
        val obsVel = Vector4()
        val range = Vector4()
        val rgvel = Vector4()
        calculateUserPosVel(julianUTC, gsPos, obsPos, obsVel)
        range.setXYZ(
            positionVector.x - obsPos.x,
            positionVector.y - obsPos.y,
            positionVector.z - obsPos.z
        )
        // Save these values globally for calculating squint angles later
        squintVector.setXYZ(range.x, range.y, range.z)
        rgvel.setXYZ(
            velocityVector.x - obsVel.x,
            velocityVector.y - obsVel.y,
            velocityVector.z - obsVel.z
        )
        magnitude(range)
        val sinLat = sin(DEG2RAD * gsPos.lat)
        val cosLat = cos(DEG2RAD * gsPos.lat)
        val sinTheta = sin(gsPosTheta)
        val cosTheta = cos(gsPosTheta)
        val topS = sinLat * cosTheta * range.x + sinLat * sinTheta * range.y - cosLat * range.z
        val topE = -sinTheta * range.x + cosTheta * range.y
        val topZ = cosLat * cosTheta * range.x + cosLat * sinTheta * range.y + sinLat * range.z
        var azim = atan(-topE / topS)
        if (topS > 0.0) azim += PI
        if (azim < 0.0) azim += TWO_PI
        satPos.azimuth = azim
        satPos.elevation = asin(topZ / range.w)
        satPos.distance = range.w
        satPos.distanceRate = dot(range, rgvel) / range.w
        var elevation = satPos.elevation / TWO_PI * 360.0
        if (elevation > 90) elevation = 180 - elevation
        satPos.aboveHorizon = elevation - 0 > EPSILON
    }

    // Returns the ECI position and velocity of the observer
    private fun calculateUserPosVel(
        time: Double,
        gsPos: GeoPos,
        obsPos: Vector4,
        obsVel: Vector4
    ) {
        val mFactor = 7.292115E-5
        gsPosTheta = mod2PI(thetaGJD(time) + DEG2RAD * gsPos.lon)
        val c = invert(sqrt(1.0 + FLAT_FACT * (FLAT_FACT - 2) * sqr(sin(DEG2RAD * gsPos.lat))))
        val sq = sqr(1.0 - FLAT_FACT) * c
        val achcp = (EARTH_RADIUS * c + gsPos.alt / 1000.0) * cos(DEG2RAD * gsPos.lat)
        obsPos.setXYZ(
            achcp * cos(gsPosTheta), achcp * sin(gsPosTheta),
            (EARTH_RADIUS * sq + gsPos.alt / 1000.0) * sin(DEG2RAD * gsPos.lat)
        )
        obsVel.setXYZ(-mFactor * obsPos.y, mFactor * obsPos.x, 0.0)
        magnitude(obsPos)
        magnitude(obsVel)
    }

    // Calculate the geodetic position of an object given its ECI pos and time
    private fun calculateLatLonAlt(time: Double) {
        satPos.theta = atan2(position.y, position.x)
        satPos.longitude = mod2PI(satPos.theta - thetaGJD(time))
        val r = sqrt(sqr(position.x) + sqr(position.y))
        val e2 = FLAT_FACT * (2.0 - FLAT_FACT)
        satPos.latitude = atan2(position.z, r)
        var phi: Double
        var c: Double
        var i = 0
        var converged: Boolean
        do {
            phi = satPos.latitude
            c = invert(sqrt(1.0 - e2 * sqr(sin(phi))))
            satPos.latitude = atan2(position.z + EARTH_RADIUS * c * e2 * sin(phi), r)
            converged = abs(satPos.latitude - phi) < EPSILON
        } while (i++ < 10 && !converged)
        satPos.altitude = r / cos(satPos.latitude) - EARTH_RADIUS * c
        var temp = satPos.latitude
        if (temp > PI_2) {
            temp -= TWO_PI
            satPos.latitude = temp
        }
    }

    internal fun calculatePosAndVel(
        rk: Double, uk: Double, xnodek: Double,
        xinck: Double, rdotk: Double, rfdotk: Double
    ) {
        // Orientation vectors
        val sinuk = sin(uk)
        val cosuk = cos(uk)
        val sinik = sin(xinck)
        val cosik = cos(xinck)
        val sinnok = sin(xnodek)
        val cosnok = cos(xnodek)
        val xmx = -sinnok * cosik
        val xmy = cosnok * cosik
        val ux = xmx * sinuk + cosnok * cosuk
        val uy = xmy * sinuk + sinnok * cosuk
        val uz = sinik * sinuk
        val vx = xmx * cosuk - cosnok * sinuk
        val vy = xmy * cosuk - sinnok * sinuk
        val vz = sinik * cosuk
        // Position and velocity
        position.setXYZ(ux, uy, uz)
        position.multiply(rk)
        velocity.x = rdotk * ux + rfdotk * vx
        velocity.y = rdotk * uy + rfdotk * vy
        velocity.z = rdotk * uz + rfdotk * vz
    }

    internal class Vector4(
        var w: Double = 0.0,
        var x: Double = 0.0,
        var y: Double = 0.0,
        var z: Double = 0.0
    ) {

        fun multiply(multiplier: Double) {
            x *= multiplier
            y *= multiplier
            z *= multiplier
        }

        fun setXYZ(xValue: Double, yValue: Double, zValue: Double) {
            x = xValue
            y = yValue
            z = zValue
        }
    }

    internal fun sqr(arg: Double): Double {
        return arg * arg
    }

    internal fun invert(value: Double): Double {
        return 1.0 / value
    }

    // Calculates the modulus of 2 * PI
    internal fun mod2PI(value: Double): Double {
        var retVal = value
        val i = (retVal / TWO_PI).toInt()
        retVal -= i * TWO_PI
        if (retVal < 0.0) retVal += TWO_PI
        return retVal
    }

    // Solves Keplers' Equation
    internal fun converge(temp: DoubleArray, axn: Double, ayn: Double, capu: Double) {
        var converged = false
        var i = 0
        do {
            temp[7] = sin(temp[2])
            temp[8] = cos(temp[2])
            temp[3] = axn * temp[7]
            temp[4] = ayn * temp[8]
            temp[5] = axn * temp[8]
            temp[6] = ayn * temp[7]
            val epw = (capu - temp[4] + temp[3] - temp[2]) / (1.0 - temp[5] - temp[6]) + temp[2]
            if (abs(epw - temp[2]) <= EPSILON) converged = true else temp[2] = epw
        } while (i++ < 10 && !converged)
    }

    internal fun calculatePhase(xlt: Double, xnode: Double, omgadf: Double) {
        var phaseValue = xlt - xnode - omgadf + TWO_PI
        if (phaseValue < 0.0) phaseValue += TWO_PI
        satPos.phase = mod2PI(phaseValue)
    }

    // Sets perigee and checks and adjusts the calculation if the perigee is less tan 156KM
    internal fun setPerigee(perigee: Double) {
        this.perigee = perigee
        checkPerigee()
    }

    // Checks and adjusts the calculation if the perigee is less tan 156KM
    private fun checkPerigee() {
        s4 = 1.012229
        qoms24 = 1.880279E-09
        if (perigee < 156.0) {
            s4 = if (perigee <= 98.0) 20.0 else perigee - 78.0
            qoms24 = ((120 - s4) / EARTH_RADIUS).pow(4.0)
            s4 = s4 / EARTH_RADIUS + 1.0
        }
    }

    // Checks if the satellite is in sunlight
    private fun isEclipsed(): Boolean {
        val sunVector = calculateSunVector()
        val sdEarth = asin(EARTH_RADIUS / position.w)
        val rho = subtract(sunVector, position)
        val sdSun = asin(SOLAR_RADIUS / rho.w)
        val earth = scalarNegMultiply(position)
        val delta = angle(sunVector, earth)
        eclipseDepth = sdEarth - sdSun - delta
        return if (sdEarth < sdSun) false else eclipseDepth >= 0
    }

    private fun calculateSunVector(): Vector4 {
        val mjd = julUTC - 2415020.0
        val year = 1900 + mjd / 365.25
        val solTime = (mjd + deltaEt(year) / SEC_PER_DAY) / 36525.0
        val mTemp = modulus(35999.04975 * solTime, 360.0)
        val m = radians(
            modulus(358.47583 + mTemp - (0.000150 + 0.0000033 * solTime) * sqr(solTime), 360.0)
        )
        val lTemp = modulus(36000.76892 * solTime, 360.0)
        val l = radians(
            modulus(279.69668 + lTemp + 0.0003025 * sqr(solTime), 360.0)
        )
        val e = 0.01675104 - (0.0000418 + 0.000000126 * solTime) * solTime
        val c = radians(
            ((1.919460 - (0.004789 + 0.000014 * solTime) * solTime) * sin(m))
                    + ((0.020094 - 0.000100 * solTime) * sin(2 * m)) + 0.000293 * sin(3 * m)
        )
        val o = radians(modulus(259.18 - 1934.142 * solTime, 360.0))
        val lsa = modulus(l + c - radians(0.00569 - 0.00479 * sin(o)), TWO_PI)
        val nu = modulus(m + c, TWO_PI)
        var r = (1.0000002 * (1.0 - sqr(e)) / (1.0 + e * cos(nu)))
        val eps = radians(
            23.452294 - (0.0130125 + (0.00000164 - 0.000000503 * solTime) * solTime)
                    * solTime + 0.00256 * cos(o)
        )
        r *= ASTRONOMICAL_UNIT
        return Vector4(r, r * cos(lsa), r * sin(lsa) * cos(eps), r * sin(lsa) * sin(eps))
    }

    private fun subtract(v1: Vector4, v2: Vector4): Vector4 {
        val v3 = Vector4()
        v3.x = v1.x - v2.x
        v3.y = v1.y - v2.y
        v3.z = v1.z - v2.z
        magnitude(v3)
        return v3
    }

    private fun scalarNegMultiply(vector: Vector4): Vector4 {
        val neg = -1.0
        return Vector4(vector.w * abs(neg), vector.x * neg, vector.y * neg, vector.z * neg)
    }

    private fun angle(v1: Vector4, v2: Vector4): Double {
        magnitude(v1)
        magnitude(v2)
        return acos(dot(v1, v2) / (v1.w * v2.w))
    }

    private fun deltaEt(year: Double): Double {
        return 26.465 + 0.747622 * (year - 1950) + (1.886913 * sin(TWO_PI * (year - 1975) / 33))
    }

    private fun radians(degrees: Double): Double {
        return degrees * DEG2RAD
    }

    // Calculates the dot product of two vectors
    private fun dot(v1: Vector4, v2: Vector4): Double {
        return v1.x * v2.x + v1.y * v2.y + v1.z * v2.z
    }

    // Returns fractional part of double argument
    private fun fraction(arg: Double): Double {
        return arg - floor(arg)
    }

    // Calculates scalar magnitude of a vector4 argument
    private fun magnitude(v: Vector4) {
        v.w = sqrt(sqr(v.x) + sqr(v.y) + sqr(v.z))
    }

    private fun modulus(arg1: Double, arg2: Double = SEC_PER_DAY): Double {
        var returnValue = arg1
        val i = floor(returnValue / arg2).toInt()
        returnValue -= i * arg2
        if (returnValue < 0.0) returnValue += arg2
        return returnValue
    }

    // Multiplies the vector v1 by the scalar k
    private fun scaleVector(k: Double, v: Vector4) {
        v.multiply(k)
        magnitude(v)
    }

    private fun thetaGJD(theJD: Double): Double {
        val earthRotPerSidDay = 1.00273790934
        val ut = fraction(theJD + 0.5)
        val aJD = theJD - ut
        val tu = (aJD - 2451545.0) / 36525.0
        var gmst = 24110.54841 + tu * (8640184.812866 + tu * (0.093104 - tu * 6.2E-6))
        gmst = modulus(gmst + SEC_PER_DAY * earthRotPerSidDay * ut)
        return TWO_PI * gmst / SEC_PER_DAY
    }
}
