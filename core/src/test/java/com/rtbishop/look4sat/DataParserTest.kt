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
package com.rtbishop.look4sat

import com.rtbishop.look4sat.domain.DataParser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test

@ExperimentalCoroutinesApi
class DataParserTest {

    private val dataParser = DataParser(TestCoroutineDispatcher())
    private val validCSVStream = """
        OBJECT_NAME,OBJECT_ID,EPOCH,MEAN_MOTION,ECCENTRICITY,INCLINATION,RA_OF_ASC_NODE,ARG_OF_PERICENTER,MEAN_ANOMALY,EPHEMERIS_TYPE,CLASSIFICATION_TYPE,NORAD_CAT_ID,ELEMENT_SET_NO,REV_AT_EPOCH,BSTAR,MEAN_MOTION_DOT,MEAN_MOTION_DDOT
        CALSPHERE 1,1964-063C,2021-10-23T03:34:39.713664,13.73605115,.0026891,90.1713,36.6548,20.0176,64.9795,0,U,900,999,83808,.30972E-3,.299E-5,0
    """.trimIndent().byteInputStream()
    private val invalidCSVStream = """
        CALSPHERE 1,1964-063C,2021-10-23T03:34:39.713664,13.73605115,.0026891,90.1713,36.6548,20.0176,64.9795,0,U,900,999,83808,.30972E-3,.299E-5,0
        OBJECT_NAME,OBJECT_ID,EPOCH,MEAN_MOTION,ECCENTRICITY,INCLINATION,RA_OF_ASC_NODE,ARG_OF_PERICENTER,MEAN_ANOMALY,EPHEMERIS_TYPE,CLASSIFICATION_TYPE,NORAD_CAT_ID,ELEMENT_SET_NO,REV_AT_EPOCH,BSTAR,MEAN_MOTION_DOT,MEAN_MOTION_DDOT
    """.trimIndent().byteInputStream()
    private val validTLEStream = """
        ISS (ZARYA)
        1 25544U 98067A   21255.21005818 -.00120443  00000-0 -22592-2 0  9998
        2 25544  51.6451 272.4173 0002526  27.1693  64.4213 15.48396490302085
    """.trimIndent().byteInputStream()
    private val invalidTLEStream = """
        1 25544U 98067A   21255.21005818 -.00120443  00000-0 -22592-2 0  9998
        2 25544  51.6451 272.4173 0002526  27.1693  64.4213 15.48396490302085
    """.trimIndent().byteInputStream()
    private val validJSONStream = """
        [{"uuid":"UzPz4gcsNBPKPKAFPmer7g","description":"Upper side band (drifting)","alive":true,"type":"Transmitter","uplink_low":null,"uplink_high":null,"uplink_drift":null,"downlink_low":136658500,"downlink_high":null,"downlink_drift":null,"mode":"USB","mode_id":9,"uplink_mode":null,"invert":false,"baud":null,"sat_id":"SCHX-0895-2361-9925-0309","norad_cat_id":965,"status":"active","updated":"2019-04-18T05:39:53.343316Z","citation":"CITATION NEEDED - https://xkcd.com/285/","service":"Unknown","coordination":"","coordination_url":""}]
    """.trimIndent().byteInputStream()
    private val invalidJSONStream = """
        [{"description":"Upper side band (drifting)","alive":true,"type":"Transmitter","uplink_low":null,"uplink_high":null,"uplink_drift":null,"downlink_low":136658500,"downlink_high":null,"downlink_drift":null,"mode":"USB","mode_id":9,"uplink_mode":null,"invert":false,"baud":null,"sat_id":"SCHX-0895-2361-9925-0309","norad_cat_id":965,"status":"active","updated":"2019-04-18T05:39:53.343316Z","citation":"CITATION NEEDED - https://xkcd.com/285/","service":"Unknown","coordination":"","coordination_url":""}]
    """.trimIndent().byteInputStream()

    @Test
    fun `Given valid CSV stream returns valid data`() = runBlockingTest {
        val parsedList = dataParser.parseCSVStream(validCSVStream)
        assert(parsedList[0].catnum == 900)
    }

    @Test
    fun `Given invalid CSV stream returns empty list`() = runBlockingTest {
        val parsedList = dataParser.parseCSVStream(invalidCSVStream)
        assert(parsedList.isEmpty())
    }

    @Test
    fun `Given valid TLE stream returns valid data`() = runBlockingTest {
        val parsedList = dataParser.parseTLEStream(validTLEStream)
        assert(parsedList[0].catnum == 25544)
    }

    @Test
    fun `Given invalid TLE stream returns empty list`() = runBlockingTest {
        val parsedList = dataParser.parseTLEStream(invalidTLEStream)
        assert(parsedList.isEmpty())
    }

    @Test
    fun `Given valid JSON stream returns valid data`() = runBlockingTest {
        val parsedList = dataParser.parseJSONStream(validJSONStream)
        assert(parsedList[0].catnum == 965)
    }

    @Test
    fun `Given invalid JSON stream returns empty list`() = runBlockingTest {
        val parsedList = dataParser.parseJSONStream(invalidJSONStream)
        assert(parsedList.isEmpty())
    }
}
