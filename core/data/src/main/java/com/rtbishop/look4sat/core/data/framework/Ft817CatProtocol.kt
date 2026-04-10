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
package com.rtbishop.look4sat.core.data.framework

object Ft817CatProtocol {

    const val CMD_SET_FREQ: Byte = 0x01
    const val CMD_READ_FREQ_MODE: Byte = 0x03
    const val CMD_SET_MODE: Byte = 0x07
    const val CMD_PTT_ON: Byte = 0x08
    const val CMD_PTT_OFF: Byte = 0x88.toByte()
    const val CMD_CTCSS_MODE: Byte = 0x0A
    const val CMD_CTCSS_TONE: Byte = 0x0B

    const val CTCSS_ENC_ON: Byte = 0x2A
    const val CTCSS_OFF: Byte = 0x8A.toByte()

    val MODE_TO_BYTE: Map<String, Byte> = mapOf(
        "LSB" to 0x00,
        "USB" to 0x01,
        "CW" to 0x02,
        "CW-R" to 0x03,
        "AM" to 0x04,
        "FM" to 0x08,
        "DIG" to 0x0A,
        "PKT" to 0x0C
    )

    val BYTE_TO_MODE: Map<Byte, String> = MODE_TO_BYTE.entries.associate { it.value to it.key }

    /**
     * Encode frequency in Hz to 4-byte BCD with 10 Hz resolution.
     * Example: 145500000 Hz → [0x14, 0x55, 0x00, 0x00]
     */
    fun encodeFrequencyBcd(frequencyHz: Long): ByteArray {
        val freq10Hz = frequencyHz / 10
        val bcd = ByteArray(4)
        val digits = String.format("%08d", freq10Hz)
        for (i in 0 until 4) {
            val high = digits[i * 2] - '0'
            val low = digits[i * 2 + 1] - '0'
            bcd[i] = ((high shl 4) or low).toByte()
        }
        return bcd
    }

    /**
     * Decode 4-byte BCD frequency to Hz.
     */
    fun decodeFrequencyBcd(bcd: ByteArray): Long {
        var freq10Hz = 0L
        for (i in 0 until 4) {
            val b = bcd[i].toInt() and 0xFF
            val high = b shr 4
            val low = b and 0x0F
            freq10Hz = freq10Hz * 100 + high * 10 + low
        }
        return freq10Hz * 10
    }

    /**
     * Encode CTCSS tone frequency (in Hz, e.g. 67.0) to 2-byte BCD.
     * 67.0 Hz → 670 (in 0.1 Hz) → BCD [0x06, 0x70]
     */
    fun encodeCtcssToneBcd(toneHz: Double): ByteArray {
        val tone01Hz = (toneHz * 10).toLong()
        val digits = String.format("%04d", tone01Hz)
        val bcd = ByteArray(2)
        for (i in 0 until 2) {
            val high = digits[i * 2] - '0'
            val low = digits[i * 2 + 1] - '0'
            bcd[i] = ((high shl 4) or low).toByte()
        }
        return bcd
    }

    fun buildSetFreqCommand(frequencyHz: Long): ByteArray {
        val bcd = encodeFrequencyBcd(frequencyHz)
        return byteArrayOf(bcd[0], bcd[1], bcd[2], bcd[3], CMD_SET_FREQ)
    }

    fun buildSetModeCommand(mode: String): ByteArray? {
        val modeByte = MODE_TO_BYTE[mode.uppercase()] ?: return null
        return byteArrayOf(modeByte, 0x00, 0x00, 0x00, CMD_SET_MODE)
    }

    fun buildReadFreqModeCommand(): ByteArray {
        return byteArrayOf(0x00, 0x00, 0x00, 0x00, CMD_READ_FREQ_MODE)
    }

    fun buildPttOnCommand(): ByteArray {
        return byteArrayOf(0x00, 0x00, 0x00, 0x00, CMD_PTT_ON)
    }

    fun buildPttOffCommand(): ByteArray {
        return byteArrayOf(0x00, 0x00, 0x00, 0x00, CMD_PTT_OFF)
    }

    fun buildCtcssModeCommand(enabled: Boolean): ByteArray {
        val sub = if (enabled) CTCSS_ENC_ON else CTCSS_OFF
        return byteArrayOf(sub, 0x00, 0x00, 0x00, CMD_CTCSS_MODE)
    }

    fun buildSetCtcssToneCommand(toneHz: Double): ByteArray {
        val bcd = encodeCtcssToneBcd(toneHz)
        return byteArrayOf(bcd[0], bcd[1], 0x00, 0x00, CMD_CTCSS_TONE)
    }

    /**
     * Parse the 5-byte response from a READ FREQ+MODE command.
     * Returns (frequencyHz, modeString) or null if parsing fails.
     */
    fun parseReadResponse(response: ByteArray): Pair<Long, String>? {
        if (response.size < 5) return null
        val freqBcd = response.copyOfRange(0, 4)
        val frequencyHz = decodeFrequencyBcd(freqBcd)
        val modeByte = response[4]
        val mode = BYTE_TO_MODE[modeByte] ?: return null
        return Pair(frequencyHz, mode)
    }
}
