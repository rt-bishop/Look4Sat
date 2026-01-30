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
package com.rtbishop.look4sat.domain

import com.rtbishop.look4sat.domain.utility.positionToQth
import com.rtbishop.look4sat.domain.utility.qthToPosition
import org.junit.Test

class QthConverterTest {

    @Test
    fun `Given valid QTH returns correct POS`() {
        var result = qthToPosition("io91VL39FX")
        assert(result?.latitude == 51.4792 && result.longitude == -0.2083)
        result = qthToPosition("gf15vc")
        assert(result?.latitude == -34.8958 && result.longitude == -56.2083)
    }

    @Test
    fun `Given invalid QTH returns null`() {
        assert(qthToPosition("ZZ00zz") == null)
        assert(qthToPosition("JN58") == null)
    }

    @Test
    fun `Given valid POS returns correct QTH`() {
        assert(positionToQth(51.4878, -0.2146) == "IO91vl")
        assert(positionToQth(48.1466, 11.6083) == "JN58td")
    }

    @Test
    fun `Given invalid POS returns null`() {
        assert(positionToQth(91.0542, -170.1142) == null)
        assert(positionToQth(89.0542, -240.1142) == null)
    }
}
