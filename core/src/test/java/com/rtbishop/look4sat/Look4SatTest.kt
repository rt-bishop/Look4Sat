package com.rtbishop.look4sat

import com.rtbishop.look4sat.domain.predict4kotlin.TLE
import org.junit.Test

class Look4SatTest {

    @Test
    fun `Given correct TLE array returns Satellite`() {
        val elementArray = arrayOf(
            "ISS (ZARYA)",
            "1 25544U 98067A   21242.56000419  .00070558  00000-0  12956-2 0  9996",
            "2 25544  51.6433 334.9559 0003020 334.9496 106.9882 15.48593918300128"
        )
        assert(TLE.createSat(elementArray) != null)
    }

    @Test
    fun `Given incorrect TLE array returns null`() {
        val elementArray = arrayOf(
            "1 25544U 98067A   21242.56000419  .00070558  00000-0  12956-2 0  9996",
            "2 25544  51.6433 334.9559 0003020 334.9496 106.9882 15.48593918300128"
        )
        assert(TLE.createSat(elementArray) == null)
    }
}
