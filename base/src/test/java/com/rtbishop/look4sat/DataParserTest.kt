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
package com.rtbishop.look4sat

import com.rtbishop.look4sat.utility.DataParser
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
        FLTSATCOM 8 (USA 46),1989-077A,2022-01-07T11:37:38.074080,1.00273350,.0001114,12.9044,1.3272,91.5769,260.4200,0,U,20253,999,24434,0,-.85E-6,0
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
        FLTSATCOM 8 (USA 46)
        1 20253U 89077A   22007.48446845 -.00000085  00000+0  00000+0 0  9999
        2 20253  12.9044   1.3272 0001114  91.5769 260.4200  1.00273350244345
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
        assert(parsedList[0].epoch == 21320.51955234)
        assert(parsedList[1].epoch == 24069.23963816)
    }

    @Test
    fun `Given invalid CSV stream returns empty list`() = runTest(testDispatcher) {
        val parsedList = dataParser.parseCSVStream(invalidCSVStream)
        assert(parsedList.isEmpty())
    }

    @Test
    fun `Given valid TLE stream returns valid data`() = runTest(testDispatcher) {
        val parsedList = dataParser.parseTLEStream(validTLEStream)
        assert(parsedList[0].epoch == 21320.51955234)
        assert(parsedList[1].epoch == 24069.23963816)
    }

    @Test
    fun `Given invalid TLE stream returns empty list`() = runTest(testDispatcher) {
        val parsedList = dataParser.parseTLEStream(invalidTLEStream)
        assert(parsedList.isEmpty())
    }

    @Test
    fun `Given valid data streams parsed results match`() = runTest(testDispatcher) {
        val csvResult = dataParser.parseCSVStream(validCSVStream)
        val tleResult = dataParser.parseTLEStream(validTLEStream)
        assert(csvResult == tleResult)
    }

    @Test
    fun `Given valid JSON stream returns valid data`() = runTest(testDispatcher) {
        val parsedList = dataParser.parseJSONStream(validJSONStream)
        assert(parsedList[0].downlink == 136658500L)
    }

    @Test
    fun `Given invalid JSON stream returns empty list`() = runTest(testDispatcher) {
        val parsedList = dataParser.parseJSONStream(invalidJSONStream)
        assert(parsedList.isEmpty())
    }

    @Test
    fun `Function isLeapYear returns correct data`() = runTest(testDispatcher) {
        val years = listOf(1900, 1984, 1994, 2016, 2022, 2024, 2042, 2048)
        val answers = listOf(false, true, false, true, false, true, false, true)
        val results = years.map { dataParser.isLeapYear(it) }
        assert(results == answers)
    }
}
