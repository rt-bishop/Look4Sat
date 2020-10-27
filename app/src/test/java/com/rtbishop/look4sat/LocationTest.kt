package com.rtbishop.look4sat

import com.rtbishop.look4sat.utility.Utilities
import com.rtbishop.look4sat.utility.Utilities.round
import org.junit.Test

class LocationTest {
    @Test
    fun testLocToQth() {
        assert(Utilities.locToQTH(51.4878, -0.2146) == "IO91vl")
        assert(Utilities.locToQTH(48.1466, 11.6083) == "JN58td")
        assert(Utilities.locToQTH(-34.91, -56.2116) == "GF15vc")
        assert(Utilities.locToQTH(38.92, -77.065) == "FM18lw")
        assert(Utilities.locToQTH(-41.2833, 174.745) == "RE78ir")
        assert(Utilities.locToQTH(41.7147, -72.7272) == "FN31pr")
        assert(Utilities.locToQTH(37.4137, -122.1073) == "CM87wj")
        assert(Utilities.locToQTH(35.0542, -85.1142) == "EM75kb")
    }

    @Test
    fun testQthToLoc() {
        var result = Utilities.qthToGSP("IO91vl")
        assert(result.latitude.round(4) == 51.4792 && result.longitude.round(4) == -0.2083)
        result = Utilities.qthToGSP("JN58td")
        assert(result.latitude.round(4) == 48.1458 && result.longitude.round(4) == 11.625)
        result = Utilities.qthToGSP("GF15vc")
        assert(result.latitude.round(4) == -34.8958 && result.longitude.round(4) == -56.2083)
        result = Utilities.qthToGSP("FM18lw")
        assert(result.latitude.round(4) == 38.9375 && result.longitude.round(4) == -77.0417)
        result = Utilities.qthToGSP("RE78ir")
        assert(result.latitude.round(4) == -41.2708 && result.longitude.round(4) == 174.7083)
        result = Utilities.qthToGSP("FN31pr")
        assert(result.latitude.round(4) == 41.7292 && result.longitude.round(4) == -72.7083)
        result = Utilities.qthToGSP("CM87wj")
        assert(result.latitude.round(4) == 37.3958 && result.longitude.round(4) == -122.125)
        result = Utilities.qthToGSP("EM75kb")
        assert(result.latitude.round(4) == 35.0625 && result.longitude.round(4) == -85.125)
    }
}