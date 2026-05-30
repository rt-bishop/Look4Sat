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
package com.rtbishop.look4sat.core.domain

import com.rtbishop.look4sat.core.domain.utility.DataParser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

@ExperimentalCoroutinesApi
class DataParserTest {

    private val testDispatcher = StandardTestDispatcher()
    private val dataParser = DataParser(testDispatcher)
    private val validCSVStream = """
        OBJECT_NAME,OBJECT_ID,EPOCH,MEAN_MOTION,ECCENTRICITY,INCLINATION,RA_OF_ASC_NODE,ARG_OF_PERICENTER,MEAN_ANOMALY,EPHEMERIS_TYPE,CLASSIFICATION_TYPE,NORAD_CAT_ID,ELEMENT_SET_NO,REV_AT_EPOCH,BSTAR,MEAN_MOTION_DOT,MEAN_MOTION_DDOT
        ISS (ZARYA),1998-067A,2021-11-16T12:28:09.322176,15.48582035,.0004694,51.6447,309.4881,203.6966,299.8876,0,U,25544,999,31220,.31985E-4,.1288E-4,0
        ISS (ZARYA),1998-067A,2024-03-09T05:45:04.737024,15.49756209,.0005741,51.6418,90.7424,343.9724,92.8274,0,U,25544,999,44305,.25016E-3,.1373E-3,0
    """.trimIndent().byteInputStream()
    private val invalidCSVStream = """
        ISS (ZARYA),1998-067A,2021-11-16T12:28:09.322176,15.48582035,.0004694,51.6447,309.4881,203.6966,299.8876,0,U,25544,999,31220,.31985E-4,.1288E-4,0
        OBJECT_NAME,OBJECT_ID,EPOCH,MEAN_MOTION,ECCENTRICITY,INCLINATION,RA_OF_ASC_NODE,ARG_OF_PERICENTER,MEAN_ANOMALY,EPHEMERIS_TYPE,CLASSIFICATION_TYPE,NORAD_CAT_ID,ELEMENT_SET_NO,REV_AT_EPOCH,BSTAR,MEAN_MOTION_DOT,MEAN_MOTION_DDOT
    """.trimIndent().byteInputStream()
    private val validTLEStream = """
        ISS (ZARYA)
        1 25544U 98067A   21320.51955234  .00001288  00000+0  31985-4 0  9990
        2 25544  51.6447 309.4881 0004694 203.6966 299.8876 15.48582035312205
        ISS (ZARYA)
        1 25544U 98067A   24069.23963816  .00013730  00000+0  25016-3 0  9999
        2 25544  51.6418  90.7424 0005741 343.9724  92.8274 15.49756209443058
    """.trimIndent().byteInputStream()
    private val invalidTLEStream = """
        1 25544U 98067A   21320.51955234  .00001288  00000+0  31985-4 0  9990
        2 25544  51.6447 309.4881 0004694 203.6966 299.8876 15.48582035312205
    """.trimIndent().byteInputStream()
    private val validJSONStream = """
        [{"uuid":"UzPz4gcsNBPKPKAFPmer7g","description":"Upper side band (drifting)","alive":true,"type":"Transmitter","uplink_low":null,"uplink_high":null,"uplink_drift":null,"downlink_low":136658500,"downlink_high":null,"downlink_drift":null,"mode":"USB","mode_id":9,"uplink_mode":null,"invert":false,"baud":null,"sat_id":"SCHX-0895-2361-9925-0309","norad_cat_id":965,"status":"active","updated":"2019-04-18T05:39:53.343316Z","citation":"CITATION NEEDED - https://xkcd.com/285/","service":"Unknown","coordination":"","coordination_url":""}]
    """.trimIndent().byteInputStream()
    private val invalidJSONStream = """
        [{"description":"Upper side band (drifting)","alive":true,"type":"Transmitter","uplink_low":null,"uplink_high":null,"uplink_drift":null,"downlink_low":136658500,"downlink_high":null,"downlink_drift":null,"mode":"USB","mode_id":9,"uplink_mode":null,"invert":false,"baud":null,"sat_id":"SCHX-0895-2361-9925-0309","norad_cat_id":965,"status":"active","updated":"2019-04-18T05:39:53.343316Z","citation":"CITATION NEEDED - https://xkcd.com/285/","service":"Unknown","coordination":"","coordination_url":""}]
    """.trimIndent().byteInputStream()

    @Test
    fun `Given valid CSV stream returns valid data`() = runTest(testDispatcher) {
        val parsedList = dataParser.parseCSVStream(validCSVStream)
        assert(parsedList.size == 2)
        assert(parsedList[0].epoch == 21320.51955234)
        assert(parsedList[1].epoch == 24069.23963816)
    }

    @Test
    fun `Given valid CSV stream all orbital fields are parsed correctly`() = runTest(testDispatcher) {
        val csvStream = """
            OBJECT_NAME,OBJECT_ID,EPOCH,MEAN_MOTION,ECCENTRICITY,INCLINATION,RA_OF_ASC_NODE,ARG_OF_PERICENTER,MEAN_ANOMALY,EPHEMERIS_TYPE,CLASSIFICATION_TYPE,NORAD_CAT_ID,ELEMENT_SET_NO,REV_AT_EPOCH,BSTAR,MEAN_MOTION_DOT,MEAN_MOTION_DDOT
            ISS (ZARYA),1998-067A,2021-11-16T12:28:09.322176,15.48582035,.0004694,51.6447,309.4881,203.6966,299.8876,0,U,25544,999,31220,.31985E-4,.1288E-4,0
        """.trimIndent().byteInputStream()
        val sat = dataParser.parseCSVStream(csvStream)[0]
        assert(sat.name == "ISS (ZARYA)")
        assert(sat.catnum == 25544)
        assert(sat.meanmo == 15.48582035)
        assert(sat.eccn == 0.0004694)
        assert(sat.incl == 51.6447)
        assert(sat.raan == 309.4881)
        assert(sat.argper == 203.6966)
        assert(sat.meanan == 299.8876)
        assert(sat.bstar == 0.31985E-4)
        assert(sat.ndot == 0.1288E-4)
    }

    @Test
    fun `Given valid CSV stream ndot is parsed for decay detection`() = runTest(testDispatcher) {
        val csvStream = """
            OBJECT_NAME,OBJECT_ID,EPOCH,MEAN_MOTION,ECCENTRICITY,INCLINATION,RA_OF_ASC_NODE,ARG_OF_PERICENTER,MEAN_ANOMALY,EPHEMERIS_TYPE,CLASSIFICATION_TYPE,NORAD_CAT_ID,ELEMENT_SET_NO,REV_AT_EPOCH,BSTAR,MEAN_MOTION_DOT,MEAN_MOTION_DDOT
            ISS (ZARYA),1998-067A,2021-11-16T12:28:09.322176,15.48582035,.0004694,51.6447,309.4881,203.6966,299.8876,0,U,25544,999,31220,.31985E-4,.1288E-4,0
        """.trimIndent().byteInputStream()
        val sat = dataParser.parseCSVStream(csvStream)[0]
        // ISS is healthy, should not be decayed even years later
        assert(!sat.hasDecayed(System.currentTimeMillis()))
    }

    @Test
    fun `Given CSV with high drag satellite detects decay`() = runTest(testDispatcher) {
        // Simulate a satellite with high drag and old epoch that should have decayed
        val csvStream = """
            OBJECT_NAME,OBJECT_ID,EPOCH,MEAN_MOTION,ECCENTRICITY,INCLINATION,RA_OF_ASC_NODE,ARG_OF_PERICENTER,MEAN_ANOMALY,EPHEMERIS_TYPE,CLASSIFICATION_TYPE,NORAD_CAT_ID,ELEMENT_SET_NO,REV_AT_EPOCH,BSTAR,MEAN_MOTION_DOT,MEAN_MOTION_DDOT
            DEBRIS,2020-001A,2020-01-15T00:00:00.000000,15.9,.001,51.0,100.0,200.0,300.0,0,U,99999,1,100,.5E-3,.05,0
        """.trimIndent().byteInputStream()
        val sat = dataParser.parseCSVStream(csvStream)[0]
        // High mean motion (15.9) + high drag (.05) + old epoch → should be decayed by now
        assert(sat.hasDecayed(System.currentTimeMillis()))
    }

    @Test
    fun `Given invalid CSV stream returns empty list`() = runTest(testDispatcher) {
        assert(dataParser.parseCSVStream(invalidCSVStream).isEmpty())
    }

    @Test
    fun `Given valid TLE stream returns valid data`() = runTest(testDispatcher) {
        val parsedList = dataParser.parseTLEStream(validTLEStream)
        assert(parsedList.size == 2)
        assert(parsedList[0].epoch == 21320.51955234)
        assert(parsedList[1].epoch == 24069.23963816)
    }

    @Test
    fun `Given valid TLE stream all orbital fields are parsed correctly`() = runTest(testDispatcher) {
        val tleStream = """
            ISS (ZARYA)
            1 25544U 98067A   21320.51955234  .00001288  00000+0  31985-4 0  9990
            2 25544  51.6447 309.4881 0004694 203.6966 299.8876 15.48582035312205
        """.trimIndent().byteInputStream()
        val sat = dataParser.parseTLEStream(tleStream)[0]
        assert(sat.name == "ISS (ZARYA)")
        assert(sat.catnum == 25544)
        assert(sat.meanmo == 15.48582035)
        assert(sat.eccn == 0.0004694)
        assert(sat.incl == 51.6447)
        assert(sat.raan == 309.4881)
        assert(sat.argper == 203.6966)
        assert(sat.meanan == 299.8876)
        assert(sat.ndot == 0.00001288)
    }

    @Test
    fun `Given valid TLE stream ndot is parsed for decay detection`() = runTest(testDispatcher) {
        val tleStream = """
            ISS (ZARYA)
            1 25544U 98067A   21320.51955234  .00001288  00000+0  31985-4 0  9990
            2 25544  51.6447 309.4881 0004694 203.6966 299.8876 15.48582035312205
        """.trimIndent().byteInputStream()
        val sat = dataParser.parseTLEStream(tleStream)[0]
        assert(!sat.hasDecayed(System.currentTimeMillis()))
    }

    @Test
    fun `Given invalid TLE stream returns empty list`() = runTest(testDispatcher) {
        assert(dataParser.parseTLEStream(invalidTLEStream).isEmpty())
    }

    @Test
    fun `Given valid JSON stream returns valid data`() = runTest(testDispatcher) {
        assert(dataParser.parseJSONStream(validJSONStream)[0].downlinkLow == 136658500L)
    }

    @Test
    fun `Given valid JSON stream all radio fields are parsed correctly`() = runTest(testDispatcher) {
        val jsonStream = """
            [{"uuid":"UzPz4gcsNBPKPKAFPmer7g","description":"Upper side band (drifting)","alive":true,"type":"Transmitter","uplink_low":145900000,"uplink_high":146000000,"uplink_drift":null,"downlink_low":136658500,"downlink_high":136700000,"downlink_drift":null,"mode":"USB","mode_id":9,"uplink_mode":"FM","invert":true,"baud":null,"sat_id":"SCHX-0895-2361-9925-0309","norad_cat_id":965,"status":"active","updated":"2019-04-18T05:39:53.343316Z","citation":"CITATION NEEDED","service":"Unknown","coordination":"","coordination_url":""}]
        """.trimIndent().byteInputStream()
        val radio = dataParser.parseJSONStream(jsonStream)[0]
        assert(radio.uuid == "UzPz4gcsNBPKPKAFPmer7g")
        assert(radio.info == "Upper side band (drifting)")
        assert(radio.isAlive)
        assert(radio.downlinkLow == 136658500L)
        assert(radio.downlinkHigh == 136700000L)
        assert(radio.downlinkMode == "USB")
        assert(radio.uplinkLow == 145900000L)
        assert(radio.uplinkHigh == 146000000L)
        assert(radio.uplinkMode == "FM")
        assert(radio.isInverted)
        assert(radio.catnum == 965)
    }

    @Test
    fun `Given JSON with null optional fields parses without error`() = runTest(testDispatcher) {
        val jsonStream = """
            [{"uuid":"abc123","description":"Beacon","alive":false,"type":"Transmitter","uplink_low":null,"uplink_high":null,"uplink_drift":null,"downlink_low":145800000,"downlink_high":null,"downlink_drift":null,"mode":null,"mode_id":null,"uplink_mode":null,"invert":false,"baud":null,"sat_id":"TEST","norad_cat_id":12345,"status":"active","updated":"2024-01-01T00:00:00Z","citation":"","service":"Unknown","coordination":"","coordination_url":""}]
        """.trimIndent().byteInputStream()
        val radio = dataParser.parseJSONStream(jsonStream)[0]
        assert(radio.uuid == "abc123")
        assert(!radio.isAlive)
        assert(radio.downlinkLow == 145800000L)
        assert(radio.downlinkHigh == null)
        assert(radio.downlinkMode == null)
        assert(radio.uplinkLow == null)
        assert(radio.uplinkHigh == null)
        assert(radio.uplinkMode == null)
        assert(!radio.isInverted)
        assert(radio.catnum == 12345)
    }

    @Test
    fun `Given invalid JSON stream returns empty list`() = runTest(testDispatcher) {
        assert(dataParser.parseJSONStream(invalidJSONStream).isEmpty())
    }

    @Test
    fun `Given valid data streams parsed results match`() = runTest(testDispatcher) {
        assert(dataParser.parseCSVStream(validCSVStream) == dataParser.parseTLEStream(validTLEStream))
    }

    @Test
    fun `isLeapYear returns correct results`() {
        val years = listOf(1900, 1984, 1994, 2000, 2016, 2022, 2024, 2042, 2048, 2100)
        val expected = listOf(false, true, false, true, true, false, true, false, true, false)
        val results = years.map { dataParser.isLeapYear(it) }
        assert(results == expected)
    }

    @Test
    fun `getDayOfYear returns correct day for January 1st`() {
        assert(dataParser.getDayOfYear(2024, 1, 1) == 1)
        assert(dataParser.getDayOfYear(2023, 1, 1) == 1)
    }

    @Test
    fun `getDayOfYear returns correct day for March 1st in leap and non-leap years`() {
        // 2024 is leap: Jan(31) + Feb(29) + 1 = 61
        assert(dataParser.getDayOfYear(2024, 3, 1) == 61)
        // 2023 is not leap: Jan(31) + Feb(28) + 1 = 60
        assert(dataParser.getDayOfYear(2023, 3, 1) == 60)
    }

    @Test
    fun `getDayOfYear returns correct day for December 31st`() {
        assert(dataParser.getDayOfYear(2024, 12, 31) == 366) // leap year
        assert(dataParser.getDayOfYear(2023, 12, 31) == 365) // non-leap year
    }

    @Test
    fun `getDayOfYear returns correct day for November 16th`() {
        // Matches the CSV test data epoch: 2021-11-16 → day 320
        assert(dataParser.getDayOfYear(2021, 11, 16) == 320)
    }
}
