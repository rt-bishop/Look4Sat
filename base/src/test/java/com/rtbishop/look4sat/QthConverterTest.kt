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

import com.rtbishop.look4sat.utility.QthConverter
import org.junit.Test

class QthConverterTest {

    @Test
    fun `Given valid QTH returns correct POS`() {
        var result = QthConverter.qthToPosition("io91VL39FX")
        assert(result?.lat == 51.4792 && result.lon == -0.2083)
        result = QthConverter.qthToPosition("JN58TD")
        assert(result?.lat == 48.1458 && result.lon == 11.6250)
        result = QthConverter.qthToPosition("gf15vc")
        assert(result?.lat == -34.8958 && result.lon == -56.2083)
    }

    @Test
    fun `Given invalid QTH returns null`() {
        assert(QthConverter.qthToPosition("ZZ00zz") == null)
        assert(QthConverter.qthToPosition("JN58") == null)
    }

    @Test
    fun `Given valid POS returns correct QTH`() {
        assert(QthConverter.positionToQth(51.4878, -0.2146) == "IO91vl")
        assert(QthConverter.positionToQth(48.1466, 11.6083) == "JN58td")
        assert(QthConverter.positionToQth(-34.91, -56.2116) == "GF15vc")
    }

    @Test
    fun `Given invalid POS returns null`() {
        assert(QthConverter.positionToQth(91.0542, -170.1142) == null)
        assert(QthConverter.positionToQth(89.0542, -240.1142) == null)
    }
}
