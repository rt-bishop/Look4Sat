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

import java.util.Locale

/**
 * Icom CI-V protocol encoder/decoder for the IC-705.
 *
 * Frame structure:
 *   FE FE <DEST> <SRC> <CMD> [<SUB>] [<DATA...>] FD
 *
 * IC-705 default CI-V address : 0xA4
 * Controller (us) address     : 0xE0
 */
object IcomCivProtocol {

    // ── Framing constants ──────────────────────────────────────────────────
    const val PREAMBLE: Byte      = 0xFE.toByte()
    const val END_OF_MSG: Byte    = 0xFD.toByte()
    const val ACK_OK: Byte        = 0xFB.toByte()
    const val ACK_NG: Byte        = 0xFA.toByte()

    // ── Address constants ──────────────────────────────────────────────────
    /** Default CI-V address of the IC-705. */
    const val ADDR_IC705: Byte    = 0xA4.toByte()
    /** Default CI-V address of the controller (us). */
    const val ADDR_CTRL: Byte     = 0xE0.toByte()

    // ── Command bytes ──────────────────────────────────────────────────────
    /** Read operating frequency (main VFO). */
    const val CMD_READ_FREQ: Byte           = 0x03
    /** Set operating frequency (main VFO). */
    const val CMD_SET_FREQ: Byte            = 0x05
    /** Set operating mode. */
    const val CMD_SET_MODE: Byte            = 0x06
    /** Select VFO / memory. */
    const val CMD_SELECT_VFO: Byte          = 0x07
    /**
     * Select operating mode (VFO vs memory-channel).
     * Sub 0x00 = VFO mode. Must be sent after connect if the radio is in
     * memory-channel mode — frequency/mode commands return FA until it is.
     */
    const val CMD_SELECT_OP_MODE: Byte      = 0x08
    /** Set repeater duplex / SPLIT. */
    const val CMD_DUPLEX_SPLIT: Byte        = 0x0F
    /** Band stacking register / band select (sub 0x00 = select, data = BCD band number). */
    const val CMD_BAND_SELECT: Byte         = 0x1A
    /** Read/write CTCSS tone frequency. */
    const val CMD_CTCSS_TONE: Byte          = 0x1B
    /** Read/write misc settings (used for enabling CTCSS encode). */
    const val CMD_MISC_SETTING: Byte        = 0x16
    /** Read/write selected-VFO frequency (cmd 0x25). */
    const val CMD_SELECTED_VFO_FREQ: Byte   = 0x25

    // ── Sub-command bytes ──────────────────────────────────────────────────
    /** Sub for CMD_SELECT_VFO: select VFO-A (main). */
    const val SUB_VFO_A: Byte   = 0x00
    /** Sub for CMD_SELECT_VFO: select VFO-B (sub). */
    const val SUB_VFO_B: Byte   = 0x01
    /** Sub for CMD_DUPLEX_SPLIT: simplex / split OFF. */
    const val SUB_SPLIT_OFF: Byte = 0x00
    /** Sub for CMD_DUPLEX_SPLIT: SPLIT ON. */
    const val SUB_SPLIT_ON: Byte  = 0x01
    /** Sub for CMD_SELECTED_VFO_FREQ: selected (active) VFO frequency. */
    const val SUB_SELECTED_VFO: Byte = 0x00
    /** Sub for CMD_SELECTED_VFO_FREQ: unselected (inactive / TX in split) VFO frequency. */
    const val SUB_UNSELECTED_VFO: Byte = 0x01
    /** Sub for CMD_MISC_SETTING: CTCSS/DTCS tone squelch. */
    const val SUB_CTCSS_SETTING: Byte = 0x42.toByte()

    // ── Mode bytes ────────────────────────────────────────────────────────
    /** Maps mode strings (upper-case) → IC-705 mode bytes. */
    val MODE_TO_BYTE: Map<String, Byte> = mapOf(
        "LSB"    to 0x00,
        "USB"    to 0x01,
        "AM"     to 0x02,
        "CW"     to 0x03,
        "RTTY"   to 0x04,
        "FM"     to 0x05,
        "WFM"    to 0x06,
        "CW-R"   to 0x07,
        "RTTY-R" to 0x08,
        "DV"     to 0x12,
        "AFSK"   to 0x05  // AFSK uses FM modulation
    )

    val BYTE_TO_MODE: Map<Byte, String> = MODE_TO_BYTE.entries.associate { it.value to it.key }

    // ── Frequency BCD encoding ─────────────────────────────────────────────

    /**
     * Encode a frequency in Hz to the IC-705's 5-byte BCD format.
     *
     * The IC-705 uses 5 bytes, LSB pair first, with 1 Hz resolution.
     * Example: 145,500,000 Hz → "0145500000" → pairs LSB→MSB:
     *   [00, 00, 50, 45, 01]
     */
    fun encodeFrequencyBcd(frequencyHz: Long): ByteArray {
        val digits = String.format(Locale.US, "%010d", frequencyHz)
        val bcd = ByteArray(5)
        for (i in 0 until 5) {
            // digits are MSB first; we want pair index 0 = LSB pair
            val pairIndex = 4 - i
            val high = digits[pairIndex * 2] - '0'
            val low  = digits[pairIndex * 2 + 1] - '0'
            bcd[i] = ((high shl 4) or low).toByte()
        }
        return bcd
    }

    /**
     * Decode 5-byte BCD frequency (LSB pair first) to Hz.
     */
    fun decodeFrequencyBcd(bcd: ByteArray): Long {
        // Build digit string MSB→LSB by reversing the byte order
        var freqHz = 0L
        for (i in 4 downTo 0) {
            val b    = bcd[i].toInt() and 0xFF
            val high = b shr 4
            val low  = b and 0x0F
            freqHz   = freqHz * 100 + high * 10 + low
        }
        return freqHz
    }

    /**
     * Encode a CTCSS tone (Hz, e.g. 67.0) to 2-byte BCD (0.1 Hz resolution).
     * 67.0 → 670 (tenths of Hz) → BCD bytes [0x06, 0x70].
     */
    fun encodeCtcssToneBcd(toneHz: Double): ByteArray {
        val tone01 = (toneHz * 10).toLong()
        val digits = String.format(Locale.US, "%04d", tone01)
        return byteArrayOf(
            ((digits[0] - '0') shl 4 or (digits[1] - '0')).toByte(),
            ((digits[2] - '0') shl 4 or (digits[3] - '0')).toByte()
        )
    }

    // ── Message builders ───────────────────────────────────────────────────

    /** Wrap payload bytes in a CI-V frame: FE FE DEST SRC ... FD. */
    private fun frame(vararg payload: Byte): ByteArray {
        return byteArrayOf(PREAMBLE, PREAMBLE, ADDR_IC705, ADDR_CTRL) +
                payload +
                byteArrayOf(END_OF_MSG)
    }

    /** Set operating frequency via CMD 0x05 (main VFO). */
    fun buildSetFreqCommand(frequencyHz: Long): ByteArray {
        return frame(CMD_SET_FREQ, *encodeFrequencyBcd(frequencyHz))
    }

    /**
     * Set selected-VFO frequency via CMD 0x25 sub 0x00.
     * This updates whichever VFO is currently active (RX or TX after split).
     */
    fun buildSetWorkingFreqCommand(frequencyHz: Long): ByteArray {
        return frame(CMD_SELECTED_VFO_FREQ, SUB_SELECTED_VFO, *encodeFrequencyBcd(frequencyHz))
    }

    /**
     * Set unselected-VFO frequency via CMD 0x25 sub 0x01.
     * In split mode while PTT is pressed the IC-705 makes VFO-B active, so
     * this command targets VFO-A (the RX VFO) — and vice-versa when in RX.
     * Use this to update the TX VFO when PTT is on.
     */
    fun buildSetUnselectedVfoFreqCommand(frequencyHz: Long): ByteArray {
        return frame(CMD_SELECTED_VFO_FREQ, SUB_UNSELECTED_VFO, *encodeFrequencyBcd(frequencyHz))
    }

    /** Read operating frequency (CMD 0x03). */
    fun buildReadFreqCommand(): ByteArray = frame(CMD_READ_FREQ)

    /** Read selected (active) VFO frequency (CMD 0x25 sub 0x00). */
    fun buildReadWorkingFreqCommand(): ByteArray = frame(CMD_SELECTED_VFO_FREQ, SUB_SELECTED_VFO)

    /** Read unselected (inactive/TX in split) VFO frequency (CMD 0x25 sub 0x01). */
    fun buildReadTxVfoFreqCommand(): ByteArray = frame(CMD_SELECTED_VFO_FREQ, SUB_UNSELECTED_VFO)

    /**
     * Select band via CMD 0x1A sub 0x00.
     * Band codes are BCD-numbered: 1=160m, 2=80m, …, 9=10m, 0x10=6m, 0x11=2m, 0x12=70cm, 0x13=23cm.
     * Returns null if [frequencyHz] doesn't fall in a known amateur band.
     */
    fun buildBandSelectCommand(frequencyHz: Long): ByteArray? {
        val code = bandCodeForFrequency(frequencyHz) ?: return null
        return frame(CMD_BAND_SELECT, 0x00, code)
    }

    /**
     * Map a frequency in Hz to the IC-705 band stacking register code.
     * Codes are BCD (band number in decimal expressed as hex nibbles).
     */
    fun bandCodeForFrequency(frequencyHz: Long): Byte? = when {
        frequencyHz in 1_800_000L     ..1_999_999L     -> 0x01 // 160 m
        frequencyHz in 3_500_000L     ..3_999_999L     -> 0x02 // 80 m
        frequencyHz in 7_000_000L     ..7_299_999L     -> 0x03 // 40 m
        frequencyHz in 10_100_000L    ..10_149_999L    -> 0x04 // 30 m
        frequencyHz in 14_000_000L    ..14_349_999L    -> 0x05 // 20 m
        frequencyHz in 18_068_000L    ..18_167_999L    -> 0x06 // 17 m
        frequencyHz in 21_000_000L    ..21_449_999L    -> 0x07 // 15 m
        frequencyHz in 24_890_000L    ..24_989_999L    -> 0x08 // 12 m
        frequencyHz in 28_000_000L    ..29_699_999L    -> 0x09 // 10 m
        frequencyHz in 50_000_000L    ..53_999_999L    -> 0x10 // 6 m  (BCD 10)
        frequencyHz in 144_000_000L   ..147_999_999L   -> 0x11 // 2 m  (BCD 11)
        frequencyHz in 420_000_000L   ..449_999_999L   -> 0x12 // 70 cm (BCD 12)
        frequencyHz in 1_240_000_000L ..1_299_999_999L -> 0x13 // 23 cm (BCD 13)
        else -> null
    }

    /** Set operating mode (CMD 0x06). Filter byte is omitted — radio uses its default filter for the mode. */
    fun buildSetModeCommand(mode: String): ByteArray? {
        val modeByte = MODE_TO_BYTE[mode.uppercase(Locale.US)] ?: return null
        return frame(CMD_SET_MODE, modeByte)
    }

    /** Select VFO-A (CMD 0x07 sub 0x00). */
    fun buildSelectVfoACommand(): ByteArray = frame(CMD_SELECT_VFO, SUB_VFO_A)

    /** Select VFO-B (CMD 0x07 sub 0x01). */
    fun buildSelectVfoBCommand(): ByteArray = frame(CMD_SELECT_VFO, SUB_VFO_B)

    /**
     * Enter VFO operating mode (CMD 0x08 sub 0x00).
     * Sent after connect — if the radio is in memory-channel mode frequency
     * and mode commands return FA until this is issued.
     */
    fun buildEnterVfoModeCommand(): ByteArray = frame(CMD_SELECT_OP_MODE, 0x00)

    /** Enable or disable SPLIT mode (CMD 0x0F). */
    fun buildSplitModeCommand(enable: Boolean): ByteArray {
        val sub = if (enable) SUB_SPLIT_ON else SUB_SPLIT_OFF
        return frame(CMD_DUPLEX_SPLIT, sub)
    }

    /**
     * Enable/disable CTCSS encode (CMD 0x16 sub 0x42).
     * 0x01 = CTCSS encoder ON, 0x00 = OFF.
     */
    fun buildCtcssModeCommand(enabled: Boolean): ByteArray {
        val value: Byte = if (enabled) 0x01 else 0x00
        return frame(CMD_MISC_SETTING, SUB_CTCSS_SETTING, value)
    }

    /**
     * Set CTCSS tone frequency (CMD 0x1B sub 0x00).
     */
    fun buildSetCtcssToneCommand(toneHz: Double): ByteArray {
        val bcd = encodeCtcssToneBcd(toneHz)
        return frame(CMD_CTCSS_TONE, 0x00, *bcd)
    }

    // ── Response parsing ───────────────────────────────────────────────────

    /**
     * Find and parse a complete CI-V response frame from a buffer.
     *
     * Returns the bytes between "FE FE E0 A4 <CMD>" and FD, or null if no
     * complete frame was found. The search is tolerant of interleaved
     * broadcast traffic.
     *
     * @param buf       bytes accumulated from the radio
     * @param expectCmd the command byte we are looking for in the reply, or
     *                  null to accept any command response from the radio
     */
    fun parseResponse(buf: ByteArray, expectCmd: Byte?): ParsedResponse? {
        var i = 0
        while (i < buf.size - 5) {
            // Look for FE FE preamble
            if (buf[i] != PREAMBLE || buf[i + 1] != PREAMBLE) { i++; continue }
            val dest = buf[i + 2]
            val src  = buf[i + 3]
            val cmd  = buf[i + 4]
            // We only care about frames addressed to us from the radio
            if (dest != ADDR_CTRL || src != ADDR_IC705) { i++; continue }
            // Find the terminating FD
            val fdIdx = buf.indexOf(END_OF_MSG, startIndex = i + 5)
            if (fdIdx < 0) break  // incomplete frame, wait for more data
            val payload = buf.copyOfRange(i + 5, fdIdx)
            if (expectCmd == null || cmd == expectCmd) {
                return ParsedResponse(cmd, payload, fdIdx + 1)
            }
            i = fdIdx + 1
        }
        return null
    }

    private fun ByteArray.indexOf(b: Byte, startIndex: Int): Int {
        for (k in startIndex until size) if (this[k] == b) return k
        return -1
    }

    /**
     * Check whether a buffer contains an OK acknowledgement (FB FD) from
     * the radio. Tolerates broadcast noise before the ACK.
     */
    fun containsAck(buf: ByteArray): Boolean {
        var i = 0
        while (i < buf.size - 5) {
            if (buf[i] != PREAMBLE || buf[i + 1] != PREAMBLE) { i++; continue }
            val dest = buf[i + 2]
            val src  = buf[i + 3]
            val cmd  = buf[i + 4]
            if (dest != ADDR_CTRL || src != ADDR_IC705) { i++; continue }
            // Skip to FD
            val fdIdx = buf.indexOf(END_OF_MSG, startIndex = i + 5)
            if (fdIdx < 0) break
            if (cmd == ACK_OK) return true
            if (cmd == ACK_NG) return false
            i = fdIdx + 1
        }
        return false
    }

    /**
     * Parse frequency + mode from a CMD_READ_FREQ reply payload.
     * Payload layout after stripping command byte: [5 freq bytes] [mode byte] [filter byte]
     */
    fun parseFreqModePayload(payload: ByteArray): Pair<Long, String>? {
        if (payload.size < 6) return null
        val freqHz = decodeFrequencyBcd(payload.copyOfRange(0, 5))
        val mode   = BYTE_TO_MODE[payload[5]] ?: return null
        return freqHz to mode
    }

    /** Hex dump of bytes, useful for debug logging. */
    fun toHex(bytes: ByteArray): String =
        bytes.joinToString(" ") { String.format(Locale.US, "%02X", it.toInt() and 0xFF) }

    data class ParsedResponse(
        val cmd: Byte,
        val payload: ByteArray,
        /** Index in the source buffer immediately after the FD terminator. */
        val nextOffset: Int
    )
}
