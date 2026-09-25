package com.rtbishop.look4sat.core.data.framework

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class IcomCivProtocolTest {

    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

    @Test
    fun parseFreqModePayload_ic705ReadFreqReply() {
        // Reply captured from an IC-705 to CMD 0x03: frequency only, no mode byte
        val reply = bytes(0xFE, 0xFE, 0xE0, 0xA4, 0x03, 0x60, 0x74, 0x95, 0x45, 0x01, 0xFD)
        val response = IcomCivProtocol.parseResponse(reply, IcomCivProtocol.CMD_READ_FREQ)
        assertNotNull(response)
        assertEquals(145957460L to "", IcomCivProtocol.parseFreqModePayload(response!!.payload))
    }

    @Test
    fun parseFreqModePayload_withModeByte() {
        assertEquals(
            435611000L to "USB",
            IcomCivProtocol.parseFreqModePayload(bytes(0x00, 0x10, 0x61, 0x35, 0x04, 0x01, 0x01))
        )
    }

    @Test
    fun parseFreqModePayload_tooShort() {
        assertNull(IcomCivProtocol.parseFreqModePayload(bytes(0x60, 0x74, 0x95, 0x45)))
    }
}
