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
package com.rtbishop.look4sat.core.domain.predict

import com.rtbishop.look4sat.core.domain.utility.toDegrees
import com.rtbishop.look4sat.core.domain.utility.toRadians
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Standalone celestial computations extracted from PREDICT v2.2.5.
 * Provides Sun position, Moon position, satellite visibility classification,
 * orbital metadata, RA/Dec conversion, and rise/set finding for Sun and Moon.
 *
 * All angles are in degrees unless noted. Time is Unix epoch milliseconds.
 *
 * Shared math utilities (thetaGJD, modulus, mod2PI, deltaET, millisToDaynum,
 * solarPositionECI, eciToGeodetic) live in OrbitalMath.kt in the same package.
 */
object CelestialComputer {

    // ── Result types ──

    /** Sun position as seen from a ground observer. */
    data class SunPosition(
        val azimuth: Double,         // degrees, 0=N, 90=E
        val elevation: Double,       // degrees, >0 above horizon
        val distance: Double,        // normalized: 1.0 + ((range - AU) / AU)
        val rangeRate: Double,       // km/s
        val latitude: Double,        // sub-solar point latitude, degrees
        val longitude: Double,       // sub-solar point longitude, degrees
        val rightAscension: Double,  // degrees
        val declination: Double      // degrees
    )

    /** Moon position as seen from a ground observer. */
    data class MoonPosition(
        val azimuth: Double,         // degrees, 0=N, 90=E
        val elevation: Double,       // degrees
        val rightAscension: Double,  // degrees
        val declination: Double,     // degrees
        val gha: Double,             // Greenwich Hour Angle, degrees
        val angularDiameter: Double, // apparent diameter relative to Earth's diameter
        val radialVelocity: Double   // m/s, Doppler radial velocity for EME
    )

    /**
     * 3-state satellite visibility classification.
     * - [VISIBLE]: satellite is sunlit, observer is in darkness (sun below -12°) — optically visible
     * - [DAYLIGHT]: satellite is sunlit, observer is in daylight
     * - [ECLIPSED]: satellite is in Earth's shadow
     */
    enum class SatVisibility { VISIBLE, DAYLIGHT, ECLIPSED }

    /** Orbital metadata not typically included in pass data. */
    data class OrbitalMetadata(
        val footprintDiameter: Double, // km, ground coverage circle diameter
        val orbitNumber: Long,         // current orbit/revolution number
        val betaAngle: Double,         // degrees, angle between orbital plane and Sun
        val orbitalPhase: Double       // 0-256 phase within current orbit
    )

    // ── Sun position ──

    /**
     * Compute the Sun's full position as seen from [observer] at [timeMillis].
     * Includes az/el, RA/Dec, sub-solar lat/lon, range, and range rate.
     * Based on FindSun() from PREDICT v2.2.5.
     */
    fun getSunPosition(observer: GeoPos, timeMillis: Long): SunPosition {
        val daynum = millisToDaynum(timeMillis)
        val julUtc = daynum + 2444238.5
        val sunVec = solarPositionECI(julUtc)
        val zeroVel = doubleArrayOf(0.0, 0.0, 0.0)
        val obsGeo = observerGeodetic(observer)

        // Az, El, Range, RangeRate
        val obsSet = computeObsAngles(julUtc, sunVec, zeroVel, obsGeo)

        // Lat/Lon of sub-solar point
        val latLon = eciToGeodetic(julUtc, sunVec)

        // RA/Dec
        val raDec = calculateRADec(julUtc, sunVec, zeroVel, obsGeo)

        return SunPosition(
            azimuth = obsSet[0].toDegrees(),
            elevation = obsSet[1].toDegrees(),
            distance = 1.0 + ((obsSet[2] - ASTRONOMICAL_UNIT) / ASTRONOMICAL_UNIT),
            rangeRate = 1000.0 * obsSet[3],
            latitude = latLon[0].toDegrees(),
            longitude = latLon[1].toDegrees().let { if (it > 180.0) it - 360.0 else it },
            rightAscension = raDec[0].toDegrees(),
            declination = raDec[1].toDegrees()
        )
    }

    // ── Moon position ──

    /**
     * Compute the Moon's position as seen from [observer] at [timeMillis].
     * Full Meeus lunar ephemeris from PREDICT v2.2.5 with expanded terms
     * and radial velocity approximation for EME Doppler.
     */
    fun getMoonPosition(observer: GeoPos, timeMillis: Long): MoonPosition {
        val daynum = millisToDaynum(timeMillis)
        val jd = daynum + 2444238.5
        var t = (jd - 2415020.0) / 36525.0
        val t2 = t * t
        val t3 = t2 * t

        var l1 = 270.434164 + 481267.8831 * t - 0.001133 * t2 + 0.0000019 * t3
        var mSun = 358.475833 + 35999.0498 * t - 0.00015 * t2 - 0.0000033 * t3
        var m1 = 296.104608 + 477198.8491 * t + 0.009192 * t2 + 0.0000144 * t3
        var d = 350.737486 + 445267.1142 * t - 0.001436 * t2 + 0.0000019 * t3
        var ff = 11.250889 + 483202.0251 * t - 0.003211 * t2 - 0.0000003 * t3
        val om = (259.183275 - 1934.142 * t + 0.002078 * t2 + 0.0000022 * t3) * DEG2RAD

        val correction512 = sin((51.2 + 20.2 * t) * DEG2RAD)
        val ss = 0.003964 * sin((346.56 + 132.87 * t - 0.0091731 * t2) * DEG2RAD)
        l1 += 0.000233 * correction512 + ss + 0.001964 * sin(om)
        mSun -= 0.001778 * correction512
        m1 += 0.000817 * correction512 + ss + 0.002541 * sin(om)
        d += 0.002011 * correction512 + ss + 0.001964 * sin(om)
        ff += ss - 0.024691 * sin(om) - 0.004328 * sin(om + (275.05 - 2.3 * t) * DEG2RAD)

        val ex = 1.0 - 0.002495 * t - 0.00000752 * t2
        l1 = primeAngle(l1); mSun = primeAngle(mSun); m1 = primeAngle(m1)
        d = primeAngle(d); ff = primeAngle(ff)

        val mR = mSun * DEG2RAD
        val m1R = m1 * DEG2RAD
        val dR = d * DEG2RAD
        val ffR = ff * DEG2RAD

        // Ecliptic longitude — expanded v225 terms
        var l = l1 + 6.28875 * sin(m1R) + 1.274018 * sin(2 * dR - m1R) + 0.658309 * sin(2 * dR)
        l += 0.213616 * sin(2 * m1R) - ex * 0.185596 * sin(mR) - 0.114336 * sin(2 * ffR)
        l += 0.058793 * sin(2 * dR - 2 * m1R) + ex * 0.057212 * sin(2 * dR - mR - m1R) + 0.05332 * sin(2 * dR + m1R)
        l += ex * 0.045874 * sin(2 * dR - mR) + ex * 0.041024 * sin(m1R - mR) - 0.034718 * sin(dR)
        l -= ex * 0.030465 * sin(mR + m1R) + 0.015326 * sin(2 * dR - 2 * ffR) - 0.012528 * sin(2 * ffR + m1R)
        l -= 0.01098 * sin(2 * ffR - m1R) + 0.010674 * sin(4 * dR - m1R) + 0.010034 * sin(3 * m1R)
        l += 0.008548 * sin(4 * dR - 2 * m1R) - ex * 0.00791 * sin(mR - m1R + 2 * dR)
        l -= ex * 0.006783 * sin(2 * dR + mR)
        l += 0.005162 * sin(m1R - dR) + ex * 0.005 * sin(mR + dR) + ex * 0.004049 * sin(m1R - mR + 2 * dR)
        l += 0.003996 * sin(2 * m1R + 2 * dR) + 0.003862 * sin(4 * dR) + 0.003665 * sin(2 * dR - 3 * m1R)
        l += ex * 0.002695 * sin(2 * m1R - mR) + 0.002602 * sin(m1R - 2 * ffR - 2 * dR)
        l += ex * 0.002396 * sin(2 * dR - mR - 2 * m1R)
        l -= 0.002349 * sin(m1R + dR) + ex * ex * 0.002249 * sin(2 * dR - 2 * mR)
        l -= ex * 0.002125 * sin(2 * m1R + mR)
        l -= ex * ex * 0.002079 * sin(2 * mR) + ex * ex * 0.002059 * sin(2 * dR - m1R - 2 * mR)
        l -= 0.001773 * sin(m1R + 2 * dR - 2 * ffR)
        l += ex * 0.00122 * sin(4 * dR - mR - m1R) - 0.00111 * sin(2 * m1R + 2 * ffR) + 0.000892 * sin(m1R - 3 * dR)
        l -= ex * 0.000811 * sin(mR + m1R + 2 * dR) + ex * 0.000761 * sin(4 * dR - mR - 2 * m1R)
        l += ex * ex * 0.000717 * sin(m1R - 2 * mR)
        l += ex * ex * 0.000704 * sin(m1R - 2 * mR - 2 * dR) + ex * 0.000693 * sin(mR - 2 * m1R + 2 * dR)
        l += ex * 0.000598 * sin(2 * dR - mR - 2 * ffR) + 0.00055 * sin(m1R + 4 * dR)
        l += 0.000538 * sin(4 * m1R) + ex * 0.000521 * sin(4 * dR - mR) + 0.000486 * sin(2 * m1R - dR)
        l -= 0.001595 * sin(2 * ffR + 2 * dR)

        // Ecliptic latitude — expanded v225 terms
        var b =
            5.128189 * sin(ffR) + 0.280606 * sin(m1R + ffR) + 0.277693 * sin(m1R - ffR) + 0.173238 * sin(2 * dR - ffR)
        b += 0.055413 * sin(2 * dR + ffR - m1R) + 0.046272 * sin(2 * dR - ffR - m1R) + 0.032573 * sin(2 * dR + ffR)
        b += 0.017198 * sin(2 * m1R + ffR) + 9.266999e-03 * sin(2 * dR + m1R - ffR) + 0.008823 * sin(2 * m1R - ffR)
        b += ex * 0.008247 * sin(2 * dR - mR - ffR) + 0.004323 * sin(2 * dR - ffR - 2 * m1R)
        b += 0.0042 * sin(2 * dR + ffR + m1R)
        b += ex * 0.003372 * sin(ffR - mR - 2 * dR) + ex * 0.002472 * sin(2 * dR + ffR - mR - m1R)
        b += ex * 0.002222 * sin(2 * dR + ffR - mR)
        b += 0.002072 * sin(2 * dR - ffR - mR - m1R) + ex * 0.001877 * sin(ffR - mR + m1R)
        b += 0.001828 * sin(4 * dR - ffR - m1R)
        b -= ex * 0.001803 * sin(ffR + mR) - 0.00175 * sin(3 * ffR)
        b += ex * 0.00157 * sin(m1R - mR - ffR) - 0.001487 * sin(ffR + dR)
        b -= ex * 0.001481 * sin(ffR + mR + m1R) + ex * 0.001417 * sin(ffR - mR - m1R)
        b += ex * 0.00135 * sin(ffR - mR) + 0.00133 * sin(ffR - dR)
        b += 0.001106 * sin(ffR + 3 * m1R) + 0.00102 * sin(4 * dR - ffR) + 0.000833 * sin(ffR + 4 * dR - m1R)
        b += 0.000781 * sin(m1R - 3 * ffR) + 0.00067 * sin(ffR + 4 * dR - 2 * m1R)
        b += 0.000606 * sin(2 * dR - 3 * ffR)
        b += 0.000597 * sin(2 * dR + 2 * m1R - ffR) + ex * 0.000492 * sin(2 * dR + m1R - mR - ffR)
        b += 0.00045 * sin(2 * m1R - ffR - 2 * dR)
        b += 0.000439 * sin(3 * m1R - ffR) + 0.000423 * sin(ffR + 2 * dR + 2 * m1R)
        b += 0.000422 * sin(2 * dR - ffR - 3 * m1R)
        b -= ex * 0.000367 * sin(mR + ffR + 2 * dR - m1R) - ex * 0.000353 * sin(mR + ffR + 2 * dR)
        b += 0.000331 * sin(ffR + 4 * dR)
        b += ex * 0.000317 * sin(2 * dR + ffR - mR + m1R) + ex * ex * 0.000306 * sin(2 * dR - 2 * mR - ffR)
        b -= 0.000283 * sin(m1R + 3 * ffR)

        val w1 = 0.0004664 * cos(om)
        val w2 = 0.0000754 * cos(om + (275.05 - 2.3 * t) * DEG2RAD)
        val bt = b * (1.0 - w1 - w2)

        // Parallax — expanded v225 terms
        var p =
            0.950724 + 0.051818 * cos(m1R) + 0.009531 * cos(2 * dR - m1R) + 0.007843 * cos(2 * dR) + 0.002824 * cos(2 * m1R)
        p += 0.000857 * cos(2 * dR + m1R) + ex * 0.000533 * cos(2 * dR - mR) + ex * 0.000401 * cos(2 * dR - mR - m1R)
        p += 0.000173 * cos(3 * m1R) + 0.000167 * cos(4 * dR - m1R) - ex * 0.000111 * cos(mR)
        p += 0.000103 * cos(4 * dR - 2 * m1R) - 0.000084 * cos(2 * m1R - 2 * dR) - ex * 0.000083 * cos(2 * dR + mR)
        p += 0.000079 * cos(2 * dR + 2 * m1R)
        p += 0.000072 * cos(4 * dR) + ex * 0.000064 * cos(2 * dR - mR + m1R) - ex * 0.000063 * cos(2 * dR + mR - m1R)
        p += ex * 0.000041 * cos(mR + dR) + ex * 0.000035 * cos(2 * m1R - mR) - 0.000033 * cos(3 * m1R - 2 * dR)
        p -= 0.00003 * cos(m1R + dR) - 0.000029 * cos(2 * ffR - 2 * dR) - ex * 0.000029 * cos(2 * m1R + mR)
        p += ex * ex * 0.000026 * cos(2 * dR - 2 * mR) - 0.000023 * cos(2 * ffR - 2 * dR + m1R)
        p += ex * 0.000019 * cos(4 * dR - mR - m1R)

        val bRad = bt * DEG2RAD
        val lm = l * DEG2RAD
        val moonDx = 3.0 / (PI * p)

        // Ecliptic → equatorial
        val z = (jd - 2415020.5) / 365.2422
        val ob = (23.452294 - (0.46845 * z + 5.9e-07 * z * z) / 3600.0).toRadians()
        val dec = asin(sin(bRad) * cos(ob) + cos(bRad) * sin(ob) * sin(lm))
        var ra = acos(cos(bRad) * cos(lm) / cos(dec)); if (lm > PI) ra = TWO_PI - ra

        val n = observer.latitude * DEG2RAD
        t = (jd - 2451545.0) / 36525.0
        var teg = 280.46061837 + 360.98564736629 * (jd - 2451545.0) + (0.000387933 * t - t * t / 38710000.0) * t
        while (teg > 360.0) teg -= 360.0
        val th = fixAngle((teg - observer.longitude) * DEG2RAD)
        val h = th - ra
        val azVal = atan2(sin(h), cos(h) * sin(n) - tan(dec) * cos(n)) + PI
        val el = asin(sin(n) * sin(dec) + cos(n) * cos(dec) * cos(h))

        // Moon radial velocity approximation (from "Amateur Radio Software", GM4ANB, RSGB 1985)
        val mm = fixAngle(1.319238 + daynum * 0.228027135)
        val radT2 = 0.10976
        val radT1 = mm + radT2 * sin(mm)
        var dv = 0.01255 * moonDx * moonDx * sin(radT1) * (1.0 + radT2 * cos(mm))
        dv *= 4449.0
        val earthR = 6378.0
        val moonDist = 384401.0
        val radT3 = earthR * moonDist * (cos(dec) * cos(n) * sin(h)) /
            sqrt(moonDist * moonDist - moonDist * earthR * sin(el))
        val moonDv = dv + radT3 * 0.0753125

        val moonRa = ra / DEG2RAD
        var moonGha = teg - moonRa
        if (moonGha < 0.0) moonGha += 360.0

        return MoonPosition(
            azimuth = azVal / DEG2RAD,
            elevation = el / DEG2RAD,
            rightAscension = moonRa,
            declination = dec / DEG2RAD,
            gha = moonGha,
            angularDiameter = moonDx,
            radialVelocity = moonDv
        )
    }

    // ── Satellite visibility ──

    /**
     * Classify satellite visibility given its eclipse state and the Sun's elevation
     * at the observer's location.
     *
     * @param isEclipsed whether the satellite is in Earth's shadow
     * @param sunElevationDeg Sun elevation at observer in degrees
     * @param satElevationDeg satellite elevation at observer in degrees (must be >= 0)
     */
    fun classifyVisibility(
        isEclipsed: Boolean,
        sunElevationDeg: Double,
        satElevationDeg: Double
    ): SatVisibility {
        if (isEclipsed) return SatVisibility.ECLIPSED
        return if (sunElevationDeg <= -12.0 && satElevationDeg >= 0.0) SatVisibility.VISIBLE
        else SatVisibility.DAYLIGHT
    }

    // ── Orbital metadata ──

    /**
     * Compute orbital metadata for a satellite at its current position.
     *
     * @param altitudeKm satellite altitude in km
     * @param meanMotion revolutions per day from TLE
     * @param bstar drag term from TLE
     * @param meanAnomaly mean anomaly at epoch (radians)
     * @param revNumAtEpoch revolution number at TLE epoch
     * @param ageDays days since TLE epoch (julUTC - julEpoch)
     * @param phase orbital phase in radians (from SGP4/SDP4 output)
     * @param satPosECI satellite ECI position [x, y, z]
     * @param satVelECI satellite ECI velocity [vx, vy, vz]
     * @param sunPosECI sun ECI position [x, y, z]
     */
    fun computeOrbitalMetadata(
        altitudeKm: Double,
        meanMotion: Double,
        bstar: Double,
        meanAnomaly: Double,
        revNumAtEpoch: Int,
        ageDays: Double,
        phase: Double,
        satPosECI: DoubleArray,
        satVelECI: DoubleArray,
        sunPosECI: DoubleArray
    ): OrbitalMetadata {
        // Footprint diameter (km)
        val footprint = 12756.33 * acos(EARTH_RADIUS / (EARTH_RADIUS + altitudeKm))

        // Orbit number
        val xmnpda = 1.44E3
        val orbitNum = floor(
            (meanMotion * xmnpda / TWO_PI + ageDays * bstar) * ageDays + meanAnomaly / TWO_PI
        ).toLong() + revNumAtEpoch

        // Beta angle: angle between orbital plane and Sun direction
        // Orbital plane normal = cross(pos, vel)
        val nx = satPosECI[1] * satVelECI[2] - satPosECI[2] * satVelECI[1]
        val ny = satPosECI[2] * satVelECI[0] - satPosECI[0] * satVelECI[2]
        val nz = satPosECI[0] * satVelECI[1] - satPosECI[1] * satVelECI[0]
        val nMag = sqrt(nx * nx + ny * ny + nz * nz)
        val sMag = sqrt(sunPosECI[0] * sunPosECI[0] + sunPosECI[1] * sunPosECI[1] + sunPosECI[2] * sunPosECI[2])
        val dotNS = nx * sunPosECI[0] + ny * sunPosECI[1] + nz * sunPosECI[2]
        val betaAngle = if (nMag > 0 && sMag > 0) {
            (PI / 2.0 - acos(dotNS / (nMag * sMag))).toDegrees()
        } else 0.0

        // Phase (0-256 scale, matching PREDICT convention)
        val orbitalPhase = 256.0 * (phase / TWO_PI)

        return OrbitalMetadata(footprint, orbitNum, betaAngle, orbitalPhase)
    }

    // ── Satellite status checks ──

    /** Check if a satellite is geostationary (mean motion ≈ 1.0027 rev/day). */
    fun isGeostationary(meanMotion: Double): Boolean = abs(meanMotion - 1.0027) < 0.0002

    /**
     * Check if a satellite has likely decayed based on drag and time since epoch.
     *
     * @param meanMotion revolutions per day
     * @param drag first derivative of mean motion / 2 (from TLE line 1)
     * @param epochDaynum TLE epoch as daynum (days since 31Dec79)
     * @param currentDaynum current time as daynum
     */
    fun hasDecayed(meanMotion: Double, drag: Double, epochDaynum: Double, currentDaynum: Double): Boolean {
        return epochDaynum + ((16.666666 - meanMotion) / (10.0 * abs(drag))) < currentDaynum
    }

    // ── Rise/Set finding ──

    /** Rise and set times for a celestial body. */
    data class RiseSetTimes(
        val riseTimeMillis: Long,  // 0 if not found
        val setTimeMillis: Long    // 0 if not found
    )

    /**
     * Find the next sunrise and sunset times from [startMillis] for [observer].
     * Uses the adaptive iteration from PREDICT v2.2.5's PredictSun().
     */
    fun findSunRiseSet(observer: GeoPos, startMillis: Long): RiseSetTimes {
        var daynum = millisToDaynum(startMillis)
        var sunPos = getSunPosition(observer, daynumToMillis(daynum))

        // Find sunrise: iterate until sun elevation crosses zero
        var sunrise = 0.0
        // If sun is already up, move forward until it sets first
        if (sunPos.elevation > 0) {
            while (sunPos.elevation > 0) {
                daynum += 0.004 * (sin(DEG2RAD * (sunPos.elevation + 0.5)))
                sunPos = getSunPosition(observer, daynumToMillis(daynum))
            }
            daynum += 0.4 // advance past night
        }
        // Now find next sunrise
        while (sunrise == 0.0) {
            if (abs(sunPos.elevation) < 0.03) {
                sunrise = daynum
            } else {
                daynum -= (0.004 * sunPos.elevation)
                sunPos = getSunPosition(observer, daynumToMillis(daynum))
            }
        }

        // Find sunset from sunrise
        daynum = sunrise
        sunPos = getSunPosition(observer, daynumToMillis(daynum))
        // Move forward through the day
        while (sunPos.elevation > -3) {
            daynum += 0.04 * (cos(DEG2RAD * (sunPos.elevation + 0.5)))
            sunPos = getSunPosition(observer, daynumToMillis(daynum))
        }
        // Refine sunset
        var sunset = 0.0
        while (sunset == 0.0) {
            daynum += 0.004 * (sin(DEG2RAD * (sunPos.elevation + 0.5)))
            sunPos = getSunPosition(observer, daynumToMillis(daynum))
            if (sunPos.elevation <= 0) sunset = daynum
        }

        return RiseSetTimes(daynumToMillis(sunrise), daynumToMillis(sunset))
    }

    /**
     * Find the next moonrise and moonset times from [startMillis] for [observer].
     * Uses the adaptive iteration from PREDICT v2.2.5's PredictMoon().
     */
    fun findMoonRiseSet(observer: GeoPos, startMillis: Long): RiseSetTimes {
        var daynum = millisToDaynum(startMillis)
        var moonPos = getMoonPosition(observer, daynumToMillis(daynum))

        // If moon is already up, move forward until it sets
        if (moonPos.elevation > 0) {
            while (moonPos.elevation > 0) {
                daynum += 0.004 * (sin(DEG2RAD * (moonPos.elevation + 0.5)))
                moonPos = getMoonPosition(observer, daynumToMillis(daynum))
            }
            daynum += 0.4
        }
        // Find moonrise
        var moonrise = 0.0
        while (moonrise == 0.0) {
            if (abs(moonPos.elevation) < 0.03) {
                moonrise = daynum
            } else {
                daynum -= (0.004 * moonPos.elevation)
                moonPos = getMoonPosition(observer, daynumToMillis(daynum))
            }
        }

        // Find moonset from moonrise
        daynum = moonrise
        moonPos = getMoonPosition(observer, daynumToMillis(daynum))
        while (moonPos.elevation > -3) {
            daynum += 0.04 * (cos(DEG2RAD * (moonPos.elevation + 0.5)))
            moonPos = getMoonPosition(observer, daynumToMillis(daynum))
        }
        var moonset = 0.0
        while (moonset == 0.0) {
            daynum += 0.004 * (sin(DEG2RAD * (moonPos.elevation + 0.5)))
            moonPos = getMoonPosition(observer, daynumToMillis(daynum))
            if (moonPos.elevation <= 0) moonset = daynum
        }

        return RiseSetTimes(daynumToMillis(moonrise), daynumToMillis(moonset))
    }

    // ── Visual magnitude estimation ──

    /**
     * Estimate the apparent visual magnitude of a satellite.
     *
     * Uses the standard formula from McCants/Heavens-Above:
     *   apparentMag = stdMag + 5 * log10(range / 1000) - 15 * log10(cos(phaseAngle / 2))
     *
     * @param rangeKm slant range from observer to satellite in km
     * @param phaseAngleDeg Sun-satellite-observer angle in degrees
     * @param stdMag intrinsic/standard magnitude (default 4.0)
     * @return estimated apparent visual magnitude
     */
    fun estimateVisualMagnitude(rangeKm: Double, phaseAngleDeg: Double, stdMag: Double = 4.0): Double {
        if (rangeKm <= 0) return stdMag
        val halfPhaseRad = phaseAngleDeg.toRadians() / 2.0
        val cosHalfPhase = cos(halfPhaseRad)
        val phaseTerm = if (cosHalfPhase > 1e-6) -15.0 * log10(cosHalfPhase) else 99.0
        return stdMag + 5.0 * log10(rangeKm / 1000.0) + phaseTerm
    }

    /**
     * Compute the phase angle (Sun-satellite-observer) in degrees.
     *
     * @param satPosECI satellite ECI position [x, y, z] in km
     * @param sunPosECI sun ECI position [x, y, z] in km
     * @param obsPosECI observer ECI position [x, y, z] in km
     * @return phase angle in degrees (0 = fully illuminated face toward observer)
     */
    fun computePhaseAngle(satPosECI: DoubleArray, sunPosECI: DoubleArray, obsPosECI: DoubleArray): Double {
        val toSunX = sunPosECI[0] - satPosECI[0]
        val toSunY = sunPosECI[1] - satPosECI[1]
        val toSunZ = sunPosECI[2] - satPosECI[2]
        val toObsX = obsPosECI[0] - satPosECI[0]
        val toObsY = obsPosECI[1] - satPosECI[1]
        val toObsZ = obsPosECI[2] - satPosECI[2]
        val dot = toSunX * toObsX + toSunY * toObsY + toSunZ * toObsZ
        val magSun = sqrt(toSunX * toSunX + toSunY * toSunY + toSunZ * toSunZ)
        val magObs = sqrt(toObsX * toObsX + toObsY * toObsY + toObsZ * toObsZ)
        if (magSun == 0.0 || magObs == 0.0) return 90.0
        val cosAngle = (dot / (magSun * magObs)).coerceIn(-1.0, 1.0)
        return acos(cosAngle).toDegrees()
    }

    // ── Doppler ──

    /**
     * Compute Doppler shift for a given base frequency and range rate.
     *
     * @param frequencyHz base frequency in Hz
     * @param rangeRateKmS range rate in km/s (negative = approaching)
     * @return shifted frequency in Hz
     */
    fun dopplerShift(frequencyHz: Double, rangeRateKmS: Double): Double {
        return frequencyHz * (299792.458 - rangeRateKmS) / 299792.458
    }

    // ── Internal helpers ──

    private fun observerGeodetic(pos: GeoPos): DoubleArray {
        // [lat_rad, lon_rad, alt_km]  — longitude negated so that
        // mod2PI(thetaGJD + obsGeo[1]) == mod2PI(thetaGJD + lon_rad)
        return doubleArrayOf(pos.latitude * DEG2RAD, -pos.longitude * DEG2RAD, pos.altitude / 1000.0)
    }

    /**
     * Convert az/el observation to Right Ascension / Declination.
     * Returns [ra_rad, dec_rad].
     * Based on Calculate_RADec() from PREDICT v2.2.5 (Escobal method).
     */
    private fun calculateRADec(
        julUtc: Double,
        targetPos: DoubleArray,
        targetVel: DoubleArray,
        obsGeo: DoubleArray
    ): DoubleArray {
        val obsSet = computeObsAngles(julUtc, targetPos, targetVel, obsGeo)
        val az = obsSet[0]
        val el = obsSet[1]
        val phi = obsGeo[0]
        val theta = mod2PI(thetaGJD(julUtc) + obsGeo[1])
        val sinTheta = sin(theta)
        val cosTheta = cos(theta)
        val sinPhi = sin(phi)
        val cosPhi = cos(phi)
        val lxh = -cos(az) * cos(el)
        val lyh = sin(az) * cos(el)
        val lzh = sin(el)
        val sx = sinPhi * cosTheta
        val ex2 = -sinTheta
        val zx = cosTheta * cosPhi
        val sy = sinPhi * sinTheta
        val zy = sinTheta * cosPhi
        val sz = -cosPhi
        val lx = sx * lxh + ex2 * lyh + zx * lzh
        val ly = sy * lxh + cosTheta * lyh + zy * lzh
        val lz = sz * lxh + 0.0 * lyh + sinPhi * lzh
        val dec = asin(lz)
        val cosDelta = sqrt(1.0 - lz * lz)
        val sinAlpha = ly / cosDelta
        val cosAlpha = lx / cosDelta
        val ra = mod2PI(atan2(sinAlpha, cosAlpha))
        return doubleArrayOf(ra, dec)
    }

    /**
     * Compute observer look-angles (az, el, range, rangeRate) to a target at ECI position.
     * Returns [azimuth_rad, elevation_rad, range_km, rangeRate_km/s].
     * Azimuth is north-referenced (0=N, π/2=E), matching OrbitalObject's convention.
     */
    private fun computeObsAngles(
        julUtc: Double,
        targetPos: DoubleArray,
        targetVel: DoubleArray,
        obsGeo: DoubleArray // [lat_rad, lon_rad, alt_km]
    ): DoubleArray {
        val theta = mod2PI(thetaGJD(julUtc) + obsGeo[1])
        val c = 1.0 / sqrt(1 + FLAT_FACT * (FLAT_FACT - 2) * sin(obsGeo[0]).pow(2))
        val sq = (1 - FLAT_FACT).pow(2) * c
        val achcp = (EARTH_RADIUS * c + obsGeo[2]) * cos(obsGeo[0])
        val ox = achcp * cos(theta)
        val oy = achcp * sin(theta)
        val oz = (EARTH_RADIUS * sq + obsGeo[2]) * sin(obsGeo[0])
        val ovx = -MFACTOR * oy
        val ovy = MFACTOR * ox

        val rx = targetPos[0] - ox
        val ry = targetPos[1] - oy
        val rz = targetPos[2] - oz
        val rMag = sqrt(rx * rx + ry * ry + rz * rz)
        val rvx = targetVel[0] - ovx
        val rvy = targetVel[1] - ovy
        val rvz = targetVel[2]

        val sinLat = sin(obsGeo[0])
        val cosLat = cos(obsGeo[0])
        val sinTheta = sin(theta)
        val cosTheta = cos(theta)
        val topS = sinLat * cosTheta * rx + sinLat * sinTheta * ry - cosLat * rz
        val topE = -sinTheta * rx + cosTheta * ry
        val topZ = cosLat * cosTheta * rx + cosLat * sinTheta * ry + sinLat * rz

        // Match north-based convention (0=N, 90=E) used by OrbitalObject.calculateObs
        var azim = atan2(-topE, topS)
        if (topS > 0.0) azim += PI
        if (azim < 0.0) azim += TWO_PI
        val el = asin(topZ / rMag)
        val rangeRate = (rx * rvx + ry * rvy + rz * rvz) / rMag

        return doubleArrayOf(azim, el, rMag, rangeRate)
    }

    private const val MFACTOR = 7.292115E-5

    private fun primeAngle(x: Double) = x - 360.0 * floor(x / 360.0)

    private fun fixAngle(x: Double): Double {
        var a = x; while (a > TWO_PI) a -= TWO_PI; return a
    }
}
