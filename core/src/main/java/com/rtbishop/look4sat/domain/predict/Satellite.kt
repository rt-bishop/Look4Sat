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

import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.*

abstract class Satellite(val params: TLE) {

    private val flatFactor = 3.35281066474748E-3
    private val deg2Rad = 1.745329251994330E-2
    private val secPerDay = 8.6400E4
    private val minPerDay = 1.44E3
    private val epsilon = 1.0E-12
    private val position = Vector4()
    private val velocity = Vector4()
    private var perigee = 0.0
    val orbitalPeriod = 24 * 60 / params.meanmo
    val earthRadius = 6378.137
    val j3Harmonic = -2.53881E-6
    val twoPi = Math.PI * 2.0
    val twoThirds = 2.0 / 3.0
    val xke = 7.43669161E-2
    val ck2 = 5.413079E-4
    val ck4 = 6.209887E-7
    var qoms24 = 0.0
    var s4 = 0.0

    internal fun willBeSeen(pos: GeoPos): Boolean {
        return if (params.meanmo < 1e-8) false
        else {
            val sma = 331.25 * exp(ln(1440.0 / params.meanmo) * (2.0 / 3.0))
            val apogee = sma * (1.0 + params.eccn) - earthRadius
            var lin = params.incl
            if (lin >= 90.0) lin = 180.0 - lin
            acos(earthRadius / (apogee + earthRadius)) + lin * deg2Rad > abs(pos.latitude * deg2Rad)
        }
    }

    internal fun getPosition(pos: GeoPos, time: Long): SatPos {
        val satPos = SatPos()
        // Date/time at which the position and velocity were calculated
        val julUTC = calcCurrentDaynum(time) + 2444238.5
        // Convert satellite's epoch time to Julian and calculate time since epoch in minutes
        val julEpoch = juliandDateOfEpoch(params.epoch)
        val tsince = (julUTC - julEpoch) * minPerDay
        calculateSDP4orSGP4(tsince)
        // Scale position and velocity vectors to km and km/sec
        convertSatState(position, velocity)
        // Calculate velocity of satellite
        magnitude(velocity)
        val squintVector = Vector4()
        // Angles in rads, dist in km, vel in km/S. Calculate sat Az, El, Range and Range-rate.
        calculateObs(julUTC, position, velocity, pos, squintVector, satPos)
        calculateLatLonAlt(julUTC, satPos, position)
        return satPos.apply { this.time = time }
    }

    // Read the system clock and return the number of days since 31Dec79 00:00:00 UTC (daynum 0)
    private fun calcCurrentDaynum(now: Long): Double {
        val sgp4Epoch = Calendar.getInstance(TimeZone.getTimeZone("UTC:UTC"))
        sgp4Epoch.clear()
        sgp4Epoch[1979, 11, 31, 0, 0] = 0
        val then = sgp4Epoch.timeInMillis
        val millis = now - then
        return millis / 1000.0 / 60.0 / 60.0 / 24.0
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
        if (params.isDeepspace) (this as DeepSpaceSat).calculateSDP4(tsince)
        else (this as NearEarthSat).calculateSGP4(tsince)
    }

    // Converts the sat position and velocity vectors to km and km/sec
    private fun convertSatState(pos: Vector4, vel: Vector4) {
        scaleVector(earthRadius, pos)
        scaleVector(earthRadius * minPerDay / secPerDay, vel)
    }

    // Calculates the topocentric coordinates of the object with ECI pos and vel at time
    private fun calculateObs(
        julianUTC: Double,
        positionVector: Vector4,
        velocityVector: Vector4,
        gsPos: GeoPos,
        squintVector: Vector4,
        satPos: SatPos
    ) {
        val obsPos = Vector4()
        val obsVel = Vector4()
        val range = Vector4()
        val rgvel = Vector4()
        val gsPosTheta = AtomicReference<Double>()
        calculateUserPosVel(julianUTC, gsPos, gsPosTheta, obsPos, obsVel)
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
        val sinLat = sin(deg2Rad * gsPos.latitude)
        val cosLat = cos(deg2Rad * gsPos.latitude)
        val sinTheta = sin(gsPosTheta.get())
        val cosTheta = cos(gsPosTheta.get())
        val topS = sinLat * cosTheta * range.x + sinLat * sinTheta * range.y - cosLat * range.z
        val topE = -sinTheta * range.x + cosTheta * range.y
        val topZ = cosLat * cosTheta * range.x + cosLat * sinTheta * range.y + sinLat * range.z
        var azim = atan(-topE / topS)
        if (topS > 0.0) azim += Math.PI
        if (azim < 0.0) azim += twoPi
        satPos.azimuth = azim
        satPos.elevation = asin(topZ / range.w)
        satPos.range = range.w
        satPos.rangeRate = dot(range, rgvel) / range.w
    }

    // Returns the ECI position and velocity of the observer
    private fun calculateUserPosVel(
        time: Double,
        gsPos: GeoPos,
        gsPosTheta: AtomicReference<Double>,
        obsPos: Vector4,
        obsVel: Vector4
    ) {
        val mFactor = 7.292115E-5
        gsPosTheta.set(mod2PI(thetaGJD(time) + deg2Rad * gsPos.longitude))
        val c =
            invert(sqrt(1.0 + flatFactor * (flatFactor - 2) * sqr(sin(deg2Rad * gsPos.latitude))))
        val sq = sqr(1.0 - flatFactor) * c
        val achcp = (earthRadius * c) * cos(deg2Rad * gsPos.latitude)
        obsPos.setXYZ(
            achcp * cos(gsPosTheta.get()), achcp * sin(gsPosTheta.get()),
            (earthRadius * sq) * sin(deg2Rad * gsPos.latitude)
        )
        obsVel.setXYZ(-mFactor * obsPos.y, mFactor * obsPos.x, 0.0)
        magnitude(obsPos)
        magnitude(obsVel)
    }

    // Calculate the geodetic position of an object given its ECI pos and time
    private fun calculateLatLonAlt(
        time: Double,
        satPos: SatPos,
        position: Vector4 = this.position
    ) {
        satPos.theta = atan2(position.y, position.x)
        satPos.longitude = mod2PI(satPos.theta - thetaGJD(time))
        val r = sqrt(sqr(position.x) + sqr(position.y))
        val e2 = flatFactor * (2.0 - flatFactor)
        satPos.latitude = atan2(position.z, r)
        var phi: Double
        var c: Double
        var i = 0
        var converged: Boolean
        do {
            phi = satPos.latitude
            c = invert(sqrt(1.0 - e2 * sqr(sin(phi))))
            satPos.latitude = atan2(position.z + earthRadius * c * e2 * sin(phi), r)
            converged = abs(satPos.latitude - phi) < epsilon
        } while (i++ < 10 && !converged)
        satPos.altitude = r / cos(satPos.latitude) - earthRadius * c
        var temp = satPos.latitude
        if (temp > Math.PI / 2.0) {
            temp -= twoPi
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

    internal class Vector4 {
        var w = 0.0
        var x = 0.0
        var y = 0.0
        var z = 0.0

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
        val i = (retVal / twoPi).toInt()
        retVal -= i * twoPi
        if (retVal < 0.0) retVal += twoPi
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
            if (abs(epw - temp[2]) <= epsilon) converged = true else temp[2] = epw
        } while (i++ < 10 && !converged)
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
            qoms24 = ((120 - s4) / earthRadius).pow(4.0)
            s4 = s4 / earthRadius + 1.0
        }
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

    private fun modulus(arg1: Double): Double {
        var returnValue = arg1
        val i = floor(returnValue / secPerDay).toInt()
        returnValue -= i * secPerDay
        if (returnValue < 0.0) returnValue += secPerDay
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
        gmst = modulus(gmst + secPerDay * earthRotPerSidDay * ut)
        return twoPi * gmst / secPerDay
    }
}
