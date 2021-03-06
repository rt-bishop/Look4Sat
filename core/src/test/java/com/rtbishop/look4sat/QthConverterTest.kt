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

import com.rtbishop.look4sat.domain.predict4kotlin.QthConverter
import org.junit.Test

class QthConverterTest {

    private val qthConverter = QthConverter()

    @Test
    fun `Given valid QTH returns correct POS`() {
        var result = qthConverter.qthToPosition("io91VL39FX")
        assert(result?.latitude == 51.4792 && result.longitude == -0.2083)
        result = qthConverter.qthToPosition("JN58TD")
        assert(result?.latitude == 48.1458 && result.longitude == 11.6250)
        result = qthConverter.qthToPosition("gf15vc")
        assert(result?.latitude == -34.8958 && result.longitude == -56.2083)
        result = qthConverter.qthToPosition("fm18LW")
        assert(result?.latitude == 38.9375 && result.longitude == -77.0417)
    }

    @Test
    fun `Given invalid QTH returns null`() {
        var result = qthConverter.qthToPosition("ZZ00zz")
        assert(result == null)
        result = qthConverter.qthToPosition("JN58tz")
        assert(result == null)
    }

    @Test
    fun `Given valid POS returns correct QTH`() {
        assert(qthConverter.positionToQTH(51.4878, -0.2146) == "IO91vl")
        assert(qthConverter.positionToQTH(48.1466, 11.6083) == "JN58td")
        assert(qthConverter.positionToQTH(-34.91, -56.2116) == "GF15vc")
        assert(qthConverter.positionToQTH(38.92, -77.065) == "FM18lw")
    }

    @Test
    fun `Given invalid location returns null`() {
        assert(qthConverter.positionToQTH(91.0542, -170.1142) == null)
        assert(qthConverter.positionToQTH(89.0542, -240.1142) == null)
    }
}
