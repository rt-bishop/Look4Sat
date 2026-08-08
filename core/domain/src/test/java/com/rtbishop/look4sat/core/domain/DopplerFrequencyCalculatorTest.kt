package com.rtbishop.look4sat.core.domain

import com.rtbishop.look4sat.core.domain.model.SatRadio
import com.rtbishop.look4sat.core.domain.predict.OrbitalPos
import com.rtbishop.look4sat.core.domain.utility.DopplerFrequencyCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DopplerFrequencyCalculatorTest {

    private fun linearTransponder(
        uuid: String = "linear",
        upLow: Long = 145_000_000L,
        upHigh: Long = 145_500_000L,
        downLow: Long = 435_000_000L,
        downHigh: Long? = 435_500_000L,
        inverted: Boolean = false,
        info: String = "Linear Transponder",
        downlinkMode: String? = "USB",
        uplinkMode: String? = "LSB"
    ) = SatRadio(
        uuid = uuid, info = info, isAlive = true,
        downlinkLow = downLow, downlinkHigh = downHigh,
        downlinkMode = downlinkMode, uplinkLow = upLow, uplinkHigh = upHigh,
        uplinkMode = uplinkMode, isInverted = inverted, catnum = 12345
    )

    private fun fmTransponder() = SatRadio(
        uuid = "fm", info = "FM Repeater", isAlive = true,
        downlinkLow = 435_600_000L, downlinkHigh = null,
        downlinkMode = "FM", uplinkLow = 145_900_000L, uplinkHigh = null,
        uplinkMode = "FM", isInverted = false, catnum = 99999
    )

    private fun pos(distanceRateKmS: Double = 0.0) = OrbitalPos().apply {
        this.distanceRate = distanceRateKmS
    }

    @Test
    fun isLinearTransponder_returnsTrueForLinear() {
        assertTrue(DopplerFrequencyCalculator.isLinearTransponder(linearTransponder()))
    }

    @Test
    fun isLinearTransponder_returnsFalseForFM() {
        assertFalse(DopplerFrequencyCalculator.isLinearTransponder(fmTransponder()))
    }

    @Test
    fun isLinearTransponder_returnsFalseForNullDownlinkHigh() {
        val xpdr = linearTransponder(downHigh = null)
        assertFalse(DopplerFrequencyCalculator.isLinearTransponder(xpdr))
    }

    @Test
    fun isNamedLinearTransponder_returnsTrueForLinearTransponderName() {
        assertTrue(DopplerFrequencyCalculator.isNamedLinearTransponder(linearTransponder()))
    }

    @Test
    fun isNamedLinearTransponder_returnsTrueForSsbTransponderName() {
        val xpdr = linearTransponder(info = "Mode V/U SSB Transponder", downlinkMode = "USB", uplinkMode = "LSB")
        assertTrue(DopplerFrequencyCalculator.isNamedLinearTransponder(xpdr))
    }

    @Test
    fun isNamedLinearTransponder_returnsFalseForRangeEntryWithoutTransponderName() {
        val driftingRangeEntry = linearTransponder(info = "Upper side band (drifting)")
        assertFalse(DopplerFrequencyCalculator.isNamedLinearTransponder(driftingRangeEntry))
    }

    @Test
    fun isNamedLinearTransponder_returnsTrueForAbbreviatedLinName() {
        // AO-7 style: "Mode V/A (A) Lin SSB" — "Lin" abbreviation, no "transponder" word
        val ao7Entry = linearTransponder(info = "Mode V/A (A) Lin SSB", downlinkMode = "USB", uplinkMode = "USB")
        assertTrue(DopplerFrequencyCalculator.isNamedLinearTransponder(ao7Entry))
        val ao7CwEntry = linearTransponder(info = "Mode V/A (A) Lin CW", downlinkMode = "CW", uplinkMode = "CW")
        assertTrue(DopplerFrequencyCalculator.isNamedLinearTransponder(ao7CwEntry))
        val ao7ModeBEntry = linearTransponder(info = "Mode U/V (B) Lin", downlinkMode = "USB", uplinkMode = "LSB")
        assertTrue(DopplerFrequencyCalculator.isNamedLinearTransponder(ao7ModeBEntry))
    }

    @Test
    fun isNamedLinearTransponder_returnsTrueForLinearWithoutTransponderWord() {
        // AO-73 style: "Mode U/V Linear" — has "Linear" but no "transponder"
        val ao73Entry = linearTransponder(info = "Mode U/V Linear", downlinkMode = "USB", uplinkMode = "LSB")
        assertTrue(DopplerFrequencyCalculator.isNamedLinearTransponder(ao73Entry))
    }

    @Test
    fun isNamedLinearTransponder_returnsFalseForDownlinkContainingLinInsideWord() {
        // "Downlink" contains "lin" but is not a linear-transponder name
        val downlinkEntry = linearTransponder(info = "Mode U Downlink", downlinkMode = "FM", uplinkMode = "FM")
        assertFalse(DopplerFrequencyCalculator.isNamedLinearTransponder(downlinkEntry))
    }

    @Test
    fun isNamedLinearTransponder_returnsFalseForFmRepeater() {
        assertFalse(DopplerFrequencyCalculator.isNamedLinearTransponder(fmTransponder()))
    }

    @Test
    fun deduplicateTransponders_mergesSameFrequencyRange() {
        // AO-7's Mode A: same range, SSB and CW entries
        val ssb = linearTransponder(
            uuid = "ssb-uuid", info = "Mode V/A (A) Lin SSB",
            downlinkMode = "USB", uplinkMode = "USB"
        )
        val cw = linearTransponder(
            uuid = "cw-uuid", info = "Mode V/A (A) Lin CW",
            downlinkMode = "CW", uplinkMode = "CW"
        )
        val modeB = linearTransponder(
            uuid = "modeb-uuid", info = "Mode U/V (B) Lin",
            upLow = 432_125_000L, upHigh = 432_175_000L,
            downLow = 145_925_000L, downHigh = 145_975_000L,
            downlinkMode = "USB", uplinkMode = "LSB"
        )
        val result = DopplerFrequencyCalculator.deduplicateTransponders(listOf(ssb, cw, modeB))
        assertEquals(2, result.size)
        // SSB entry should be preferred over CW (same range)
        assertEquals("ssb-uuid", result[0].uuid)
        assertEquals("modeb-uuid", result[1].uuid)
    }

    @Test
    fun deduplicateTransponders_prefersNonCwEntry() {
        // JO-97: CW entry has invert=false (wrong), SSB has invert=true (correct)
        val cw = linearTransponder(
            uuid = "cw-uuid", info = "U/V CW Transponder",
            downlinkMode = "CW", uplinkMode = "CW",
            upLow = 435_100_000L, upHigh = 435_120_000L,
            downLow = 145_855_000L, downHigh = 145_875_000L
        )
        val ssb = linearTransponder(
            uuid = "ssb-uuid", info = "U/V SSB Transponder",
            downlinkMode = "USB", uplinkMode = "LSB",
            upLow = 435_100_000L, upHigh = 435_120_000L,
            downLow = 145_855_000L, downHigh = 145_875_000L,
            inverted = true
        )
        val result = DopplerFrequencyCalculator.deduplicateTransponders(listOf(cw, ssb))
        assertEquals(1, result.size)
        assertEquals("ssb-uuid", result[0].uuid)
        // Verify the correct invert flag is preserved
        assertTrue(result[0].isInverted)
    }

    @Test
    fun deduplicateTransponders_preservesUniqueEntries() {
        val t1 = linearTransponder(uuid = "t1", upLow = 145_000_000L, upHigh = 145_500_000L,
            downLow = 435_000_000L, downHigh = 435_500_000L)
        val t2 = linearTransponder(uuid = "t2", upLow = 435_000_000L, upHigh = 435_500_000L,
            downLow = 145_000_000L, downHigh = 145_500_000L)
        val result = DopplerFrequencyCalculator.deduplicateTransponders(listOf(t1, t2))
        assertEquals(2, result.size)
    }

    @Test
    fun computeUplinkFromDownlink_linear_noDoppler() {
        val xpdr = linearTransponder()
        val orbitalPos = pos(0.0)
        val uplink = DopplerFrequencyCalculator.computeUplinkFromDownlink(435_200_000L, xpdr, orbitalPos)
        assertNotNull(uplink)
        assertEquals(145_200_000L, uplink)
    }

    @Test
    fun computeDownlinkFromUplink_linear_noDoppler() {
        val xpdr = linearTransponder()
        val orbitalPos = pos(0.0)
        val downlink = DopplerFrequencyCalculator.computeDownlinkFromUplink(145_200_000L, xpdr, orbitalPos)
        assertNotNull(downlink)
        assertEquals(435_200_000L, downlink)
    }

    @Test
    fun computeUplinkFromDownlink_withDoppler_positiveRangeRate() {
        // Satellite receding (positive range rate) → ground must transmit higher freq to compensate.
        val xpdr = linearTransponder()
        val orbitalPos = pos(7.0)
        val uplink = DopplerFrequencyCalculator.computeUplinkFromDownlink(435_200_000L, xpdr, orbitalPos)
        assertNotNull(uplink)
        assertTrue(uplink!! > 145_200_000L)
    }

    @Test
    fun computeUplinkFromDownlink_fmTransponder_returnsNull() {
        val orbitalPos = pos()
        val result = DopplerFrequencyCalculator.computeUplinkFromDownlink(435_600_000L, fmTransponder(), orbitalPos)
        assertNull(result)
    }

    @Test
    fun computeDownlinkFromUplink_fmTransponder_returnsNull() {
        val orbitalPos = pos()
        val result = DopplerFrequencyCalculator.computeDownlinkFromUplink(145_900_000L, fmTransponder(), orbitalPos)
        assertNull(result)
    }

    @Test
    fun computeDownlinkFromUplink_withPositiveOffset_addsOffsetToDownlink() {
        val xpdr = linearTransponder()
        val orbitalPos = pos(0.0)
        val downlink = DopplerFrequencyCalculator.computeDownlinkFromUplinkWithOffset(
            uplinkHz = 145_200_000L,
            transponder = xpdr,
            orbitalPos = orbitalPos,
            offsetHz = 2_500L
        )
        assertEquals(435_202_500L, downlink)
    }

    @Test
    fun computeUplinkFromDownlink_withPositiveOffset_subtractsOffsetBeforeMapping() {
        val xpdr = linearTransponder()
        val orbitalPos = pos(0.0)
        val uplink = DopplerFrequencyCalculator.computeUplinkFromDownlinkWithOffset(
            downlinkHz = 435_202_500L,
            transponder = xpdr,
            orbitalPos = orbitalPos,
            offsetHz = 2_500L
        )
        assertEquals(145_200_000L, uplink)
    }

    @Test
    fun computeOffsetRoundTrip_handlesNegativeOffset() {
        val xpdr = linearTransponder()
        val orbitalPos = pos(0.0)
        val downlink = DopplerFrequencyCalculator.computeDownlinkFromUplinkWithOffset(
            uplinkHz = 145_200_000L,
            transponder = xpdr,
            orbitalPos = orbitalPos,
            offsetHz = -2_500L
        )
        assertEquals(435_197_500L, downlink)

        val uplink = DopplerFrequencyCalculator.computeUplinkFromDownlinkWithOffset(
            downlinkHz = downlink!!,
            transponder = xpdr,
            orbitalPos = orbitalPos,
            offsetHz = -2_500L
        )
        assertEquals(145_200_000L, uplink)
    }

    @Test
    fun computeUplinkFromDownlink_invertedTransponder() {
        val xpdr = linearTransponder(inverted = true, downHigh = 435_500_000L)
        val orbitalPos = pos(0.0)
        val uplink = DopplerFrequencyCalculator.computeUplinkFromDownlink(435_200_000L, xpdr, orbitalPos)
        assertNotNull(uplink)
        assertEquals(145_300_000L, uplink)
    }

    @Test
    fun computeUplinkFromDownlink_roundTrip() {
        val xpdr = linearTransponder()
        val orbitalPos = pos(3.5)
        val originalDownlink = 435_250_000L
        val uplink = DopplerFrequencyCalculator.computeUplinkFromDownlink(originalDownlink, xpdr, orbitalPos)
        assertNotNull(uplink)
        val roundTripDownlink = DopplerFrequencyCalculator.computeDownlinkFromUplink(uplink!!, xpdr, orbitalPos)
        assertNotNull(roundTripDownlink)
        val error = kotlin.math.abs(roundTripDownlink!! - originalDownlink)
        assertTrue("Round-trip error too large: $error", error < 10000)
    }
}
