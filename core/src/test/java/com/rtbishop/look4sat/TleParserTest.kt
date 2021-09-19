package com.rtbishop.look4sat

import com.rtbishop.look4sat.domain.TLE
import org.junit.Test

class TleParserTest {

    private val validTleStream = """
        ISS (ZARYA)             
        1 25544U 98067A   21255.21005818 -.00120443  00000-0 -22592-2 0  9998
        2 25544  51.6451 272.4173 0002526  27.1693  64.4213 15.48396490302085
        SHENZHOU-12             
        1 48852U 21053A   21254.65783594  .00009779  00000-0  11349-3 0  9992
        2 48852  41.4714 115.4555 0001945 272.0324 187.3218 15.61782249 21227
        PROGRESS-MS 17          
        1 48869U 21057A   21254.57379002  .00002620  00000-0  56399-4 0  9993
        2 48869  51.6436 275.5624 0003404  23.4371 118.9508 15.48629868301984
    """.trimIndent().byteInputStream()
    private val invalidTleStream = """
        1 25544U 98067A   21255.21005818 -.00120443  00000-0 -22592-2 0  9998
        2 25544  51.6451 272.4173 0002526  27.1693  64.4213 15.48396490302085
        1 48852U 21053A   21254.65783594  .00009779  00000-0  11349-3 0  9992
        2 48852  41.4714 115.4555 0001945 272.0324 187.3218 15.61782249 21227
        1 48869U 21057A   21254.57379002  .00002620  00000-0  56399-4 0  9993
        2 48869  51.6436 275.5624 0003404  23.4371 118.9508 15.48629868301984
    """.trimIndent().byteInputStream()

    @Test
    fun `Given valid TLE stream returns valid data`() {
        val parsedList = TLE.parseTleStream(validTleStream)
        assert(parsedList[2].catnum == 48869)
    }

    @Test
    fun `Given invalid TLE stream returns empty list`() {
        val parsedList = TLE.parseTleStream(invalidTleStream)
        assert(parsedList.isEmpty())
    }
}
