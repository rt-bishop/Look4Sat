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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.math.abs

class CelestialComputerTest {

    private data class RiseSetCase(
        val name: String,
        val observer: GeoPos,
        val startIso: String
    )

    @Test
    fun `findSunRiseSet returns distinct sunrise and sunset for representative locations`() {
        val cases = listOf(
            RiseSetCase("Equator at March equinox", GeoPos(0.0, 0.0), "2026-03-20T00:00:00Z"),
            RiseSetCase("Equator at September equinox", GeoPos(0.0, 0.0), "2026-09-23T00:00:00Z"),
            RiseSetCase("Sydney winter", GeoPos(-33.8688, 151.2093), "2026-06-21T00:00:00Z"),
            RiseSetCase("Buenos Aires winter", GeoPos(-34.6037, -58.3816), "2026-06-21T00:00:00Z"),
            RiseSetCase("Cape Town winter", GeoPos(-33.9249, 18.4241), "2026-06-21T00:00:00Z"),
            RiseSetCase("London summer", GeoPos(51.5074, -0.1278), "2026-06-21T00:00:00Z")
        )

        cases.forEach { testCase ->
            val result = CelestialComputer.findSunRiseSet(testCase.observer, testCase.startIso.toMillis())
            val daylightDuration = result.setTimeMillis - result.riseTimeMillis

            assertTrue("${testCase.name}: sunrise should be non-zero", result.riseTimeMillis > 0L)
            assertTrue("${testCase.name}: sunset should be non-zero", result.setTimeMillis > 0L)
            assertTrue("${testCase.name}: sunset should be after sunrise", result.setTimeMillis > result.riseTimeMillis)
            assertTrue("${testCase.name}: daylight duration should be longer than 1 hour", daylightDuration > HOUR_MILLIS)
            assertTrue("${testCase.name}: daylight duration should be shorter than 24 hours", daylightDuration < DAY_MILLIS)

            val riseElevation = CelestialComputer.getSunPosition(testCase.observer, result.riseTimeMillis).elevation
            val setElevation = CelestialComputer.getSunPosition(testCase.observer, result.setTimeMillis).elevation
            assertEquals("${testCase.name}: sunrise should converge near the standard threshold", SUNRISE_SET_THRESHOLD, riseElevation, 0.02)
            assertEquals("${testCase.name}: sunset should converge near the standard threshold", SUNRISE_SET_THRESHOLD, setElevation, 0.02)
        }
    }

    @Test
    fun `findSunRiseSet does not return the same instant for equinox regression cases`() {
        listOf("2026-03-20T00:00:00Z", "2026-09-23T00:00:00Z").forEach { startIso ->
            val result = CelestialComputer.findSunRiseSet(GeoPos(0.0, 0.0), startIso.toMillis())
            val separationMillis = abs(result.setTimeMillis - result.riseTimeMillis)

            assertTrue("$startIso: sunrise and sunset should be separated", separationMillis > HOUR_MILLIS)
        }
    }

    private fun String.toMillis(): Long = Instant.parse(this).toEpochMilli()

    private data class MoonCase(
        val latitude: Double,
        val longitude: Double,
        val iso: String,
        val azimuth: Double,
        val elevation: Double
    )

    @Test
    fun `getMoonPosition matches reference topocentric az-el across the ecliptic longitude wrap`() {
        // Reference values are topocentric, refraction excluded. The first three cases sit on or
        // near the ecliptic longitude 0h crossing, where an acos based RA conversion mirrors the
        // Moon around 0h and skews azimuth by up to 16 degrees.
        val cases = listOf(
            MoonCase(52.52, 13.405, "2026-01-23T12:12:00Z", 133.317, 28.418),
            MoonCase(52.52, 13.405, "2026-09-26T02:00:00Z", 239.701, 20.508),
            MoonCase(52.52, 13.405, "2026-09-26T05:00:00Z", 276.290, -4.496),
            MoonCase(52.52, 13.405, "2026-10-05T19:00:00Z", 358.283, -19.612),
            MoonCase(-33.8688, 151.2093, "2026-03-15T11:00:00Z", 184.964, -36.659),
            MoonCase(40.7128, -74.0060, "2026-07-04T02:00:00Z", 98.385, -10.437)
        )

        cases.forEach { testCase ->
            val observer = GeoPos(testCase.latitude, testCase.longitude)
            val moon = CelestialComputer.getMoonPosition(observer, testCase.iso.toMillis())
            val azimuthDelta = abs((moon.azimuth - testCase.azimuth + 540.0) % 360.0 - 180.0)

            assertTrue(
                "${testCase.iso}: azimuth ${moon.azimuth} should be within 0.5 of ${testCase.azimuth}",
                azimuthDelta < 0.5
            )
            assertEquals("${testCase.iso}: elevation", testCase.elevation, moon.elevation, 0.5)
        }
    }

    private companion object {
        private const val SUNRISE_SET_THRESHOLD = -0.8333
        private const val HOUR_MILLIS = 60L * 60L * 1000L
        private const val DAY_MILLIS = 24L * HOUR_MILLIS
    }
}
