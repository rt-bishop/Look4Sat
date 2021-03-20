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

import com.rtbishop.look4sat.utility.QthConverter
import org.junit.Test

class QthConverterTest {
    
    private val converter = QthConverter()
    
    @Test
    fun `Given valid QTH returns correct location`() {
        var result = converter.qthToLocation("io91VL39FX")
        assert(result?.latitude == 51.4792 && result.longitude == -0.2083)
        result = converter.qthToLocation("JN58TD")
        assert(result?.latitude == 48.1458 && result.longitude == 11.6250)
        result = converter.qthToLocation("gf15vc")
        assert(result?.latitude == -34.8958 && result.longitude == -56.2083)
        result = converter.qthToLocation("fm18LW")
        assert(result?.latitude == 38.9375 && result.longitude == -77.0417)
    }
    
    @Test
    fun `Given invalid QTH returns null`() {
        var result = converter.qthToLocation("ZZ00zz")
        assert(result == null)
        result = converter.qthToLocation("JN58tz")
        assert(result == null)
    }
    
    @Test
    fun `Given valid location returns correct QTH`() {
        assert(converter.locationToQTH(51.4878, -0.2146) == "IO91vl")
        assert(converter.locationToQTH(48.1466, 11.6083) == "JN58td")
        assert(converter.locationToQTH(-34.91, -56.2116) == "GF15vc")
        assert(converter.locationToQTH(38.92, -77.065) == "FM18lw")
    }
    
    @Test
    fun `Given invalid location returns null`() {
        assert(converter.locationToQTH(91.0542, -170.1142) == null)
        assert(converter.locationToQTH(89.0542, -240.1142) == null)
    }
}
