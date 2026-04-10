package com.rtbishop.look4sat.core.data

import com.rtbishop.look4sat.core.data.framework.Ft817CatProtocol
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class Ft817CatProtocolTest {

    @Test
    fun encodeFrequencyBcd_145500000() {
        val bcd = Ft817CatProtocol.encodeFrequencyBcd(145500000L)
        assertContentEquals(byteArrayOf(0x14, 0x55, 0x00, 0x00), bcd)
    }

    @Test
    fun encodeFrequencyBcd_435100000() {
        val bcd = Ft817CatProtocol.encodeFrequencyBcd(435100000L)
        assertContentEquals(byteArrayOf(0x43, 0x51, 0x00, 0x00), bcd)
    }

    @Test
    fun encodeFrequencyBcd_7074000() {
        val bcd = Ft817CatProtocol.encodeFrequencyBcd(7074000L)
        assertContentEquals(byteArrayOf(0x00, 0x70, 0x74, 0x00), bcd)
    }

    @Test
    fun decodeFrequencyBcd_roundTrips() {
        val testFreqs = listOf(145500000L, 435100000L, 7074000L, 14200000L, 28500000L)
        for (freq in testFreqs) {
            val rounded = (freq / 10) * 10
            val bcd = Ft817CatProtocol.encodeFrequencyBcd(freq)
            assertEquals(rounded, Ft817CatProtocol.decodeFrequencyBcd(bcd))
        }
    }

    @Test
    fun buildSetFreqCommand_correctFormat() {
        val cmd = Ft817CatProtocol.buildSetFreqCommand(145500000L)
        assertEquals(5, cmd.size)
        assertEquals(0x01.toByte(), cmd[4])
        assertContentEquals(byteArrayOf(0x14, 0x55, 0x00, 0x00, 0x01), cmd)
    }

    @Test
    fun buildSetModeCommand_usb() {
        val cmd = Ft817CatProtocol.buildSetModeCommand("USB")
        assertNotNull(cmd)
        assertContentEquals(byteArrayOf(0x01, 0x00, 0x00, 0x00, 0x07), cmd)
    }

    @Test
    fun buildSetModeCommand_fm() {
        val cmd = Ft817CatProtocol.buildSetModeCommand("FM")
        assertNotNull(cmd)
        assertContentEquals(byteArrayOf(0x08, 0x00, 0x00, 0x00, 0x07), cmd)
    }

    @Test
    fun buildSetModeCommand_unknownReturnsNull() {
        assertNull(Ft817CatProtocol.buildSetModeCommand("INVALID"))
    }

    @Test
    fun encodeCtcssTone_67_0() {
        val bcd = Ft817CatProtocol.encodeCtcssToneBcd(67.0)
        assertContentEquals(byteArrayOf(0x06, 0x70), bcd)
    }

    @Test
    fun encodeCtcssTone_74_4() {
        val bcd = Ft817CatProtocol.encodeCtcssToneBcd(74.4)
        assertContentEquals(byteArrayOf(0x07, 0x44), bcd)
    }

    @Test
    fun encodeCtcssTone_141_3() {
        val bcd = Ft817CatProtocol.encodeCtcssToneBcd(141.3)
        assertContentEquals(byteArrayOf(0x14, 0x13), bcd)
    }

    @Test
    fun buildSetCtcssToneCommand_correctFormat() {
        val cmd = Ft817CatProtocol.buildSetCtcssToneCommand(67.0)
        assertEquals(5, cmd.size)
        assertEquals(0x0B.toByte(), cmd[4])
        assertContentEquals(byteArrayOf(0x06, 0x70, 0x00, 0x00, 0x0B), cmd)
    }

    @Test
    fun buildCtcssModeCommand_enable() {
        val cmd = Ft817CatProtocol.buildCtcssModeCommand(true)
        assertContentEquals(byteArrayOf(0x2A, 0x00, 0x00, 0x00, 0x0A), cmd)
    }

    @Test
    fun buildCtcssModeCommand_disable() {
        val cmd = Ft817CatProtocol.buildCtcssModeCommand(false)
        assertEquals(0x8A.toByte(), cmd[0])
        assertEquals(0x0A.toByte(), cmd[4])
    }

    @Test
    fun parseReadResponse_validResponse() {
        val response = byteArrayOf(0x14, 0x55, 0x00, 0x00, 0x01)
        val result = Ft817CatProtocol.parseReadResponse(response)
        assertNotNull(result)
        assertEquals(145500000L, result.first)
        assertEquals("USB", result.second)
    }

    @Test
    fun parseReadResponse_fmMode() {
        val response = byteArrayOf(0x14, 0x60, 0x00, 0x00, 0x08)
        val result = Ft817CatProtocol.parseReadResponse(response)
        assertNotNull(result)
        assertEquals(146000000L, result.first)
        assertEquals("FM", result.second)
    }

    @Test
    fun parseReadResponse_tooShort() {
        assertNull(Ft817CatProtocol.parseReadResponse(byteArrayOf(0x14, 0x55, 0x00)))
    }

    @Test
    fun parseReadResponse_unknownMode() {
        val response = byteArrayOf(0x14, 0x55, 0x00, 0x00, 0x0F)
        assertNull(Ft817CatProtocol.parseReadResponse(response))
    }

    @Test
    fun buildPttCommands() {
        val on = Ft817CatProtocol.buildPttOnCommand()
        assertContentEquals(byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x08), on)

        val off = Ft817CatProtocol.buildPttOffCommand()
        assertEquals(0x88.toByte(), off[4])
    }

    @Test
    fun buildReadCommand() {
        val cmd = Ft817CatProtocol.buildReadFreqModeCommand()
        assertContentEquals(byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x03), cmd)
    }
}
