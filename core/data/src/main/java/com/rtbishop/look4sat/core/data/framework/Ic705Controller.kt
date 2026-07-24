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

import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.rtbishop.look4sat.core.domain.repository.IRadioController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Icom IC-705 CI-V controller over Bluetooth SPP.
 *
 * The IC-705 emits broadcast frames continuously (band scope, UTC, signal
 * level, …).  A reply to any command we send may therefore be buried in
 * that noise.  All response reads drain up to [ACK_TIMEOUT_MS] and scan the
 * entire accumulated buffer for the frame we expect rather than assuming
 * the very next byte is the response.
 */
class Ic705Controller(
    private val bluetoothManager: BluetoothManager,
    private val deviceAddress: String
) : IRadioController {

    private val tag = "IC705"
    private val sppId: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
    private val ioMutex = Mutex()

    /** Time budget (ms) to wait for a response amid broadcast noise. */
    private val ACK_TIMEOUT_MS = 500L
    /** Polling interval while draining the input buffer. */
    private val POLL_INTERVAL_MS = 20L
    /** Small pause after writing a command before reading the response. */
    private val WRITE_SETTLE_MS = 50L

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    override var isConnected: Boolean = false
        private set

    // ── Connection ──────────────────────────────────────────────────────────

    override suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        if (isConnected) return@withContext true
        if (deviceAddress.isBlank()) return@withContext false
        try {
            val device  = bluetoothManager.adapter.getRemoteDevice(deviceAddress)
            val btSocket = device.createInsecureRfcommSocketToServiceRecord(sppId)
            btSocket.connect()
            socket       = btSocket
            outputStream = btSocket.outputStream
            inputStream  = btSocket.inputStream
            isConnected  = true
            // Enter VFO mode — frequency/mode commands return FA if the radio
            // is in memory-channel mode. Safe to send regardless of current state.
            Log.i(tag, "Connected to $deviceAddress — entering VFO mode")
            val vfoCmd = IcomCivProtocol.buildEnterVfoModeCommand()
            Log.d(tag, "CMD enterVfoMode → ${IcomCivProtocol.toHex(vfoCmd)}")
            ioMutex.withLock { sendAndWaitAck(vfoCmd) }
            true
        } catch (e: Exception) {
            Log.e(tag, "Connect error: ${e.message}")
            isConnected = false
            false
        }
    }

    override suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            try {
                inputStream?.close()
                outputStream?.close()
                socket?.close()
            } catch (e: Exception) {
                Log.e(tag, "Disconnect error: ${e.message}")
            } finally {
                inputStream  = null
                outputStream = null
                socket       = null
                isConnected  = false
                Log.i(tag, "Disconnected from $deviceAddress")
            }
        }
    }

    // ── IRadioController – standard operations ──────────────────────────────

    override suspend fun setFrequency(frequencyHz: Long): Boolean = withContext(Dispatchers.IO) {
        Log.d(tag, "setFrequency: ${frequencyHz}Hz")
        ioMutex.withLock {
            val cmd = IcomCivProtocol.buildSetFreqCommand(frequencyHz)
            Log.d(tag, "CMD setFreq → ${IcomCivProtocol.toHex(cmd)}")
            sendAndWaitAck(cmd)
        }
    }

    override suspend fun setMode(mode: String): Boolean = withContext(Dispatchers.IO) {
        val cmd = IcomCivProtocol.buildSetModeCommand(mode) ?: run {
            Log.w(tag, "setMode: unknown mode '$mode'")
            return@withContext false
        }
        Log.d(tag, "setMode: $mode")
        Log.d(tag, "CMD setMode → ${IcomCivProtocol.toHex(cmd)}")
        ioMutex.withLock { sendAndWaitAck(cmd) }
    }

    override suspend fun setCtcssMode(enabled: Boolean): Boolean = withContext(Dispatchers.IO) {
        Log.d(tag, "setCtcssMode: $enabled")
        val cmd = IcomCivProtocol.buildCtcssModeCommand(enabled)
        Log.d(tag, "CMD ctcssMode → ${IcomCivProtocol.toHex(cmd)}")
        ioMutex.withLock { sendAndWaitAck(cmd) }
    }

    override suspend fun setCtcssTone(toneHz: Double): Boolean = withContext(Dispatchers.IO) {
        Log.d(tag, "setCtcssTone: ${toneHz}Hz")
        val cmd = IcomCivProtocol.buildSetCtcssToneCommand(toneHz)
        Log.d(tag, "CMD ctcssTone → ${IcomCivProtocol.toHex(cmd)}")
        ioMutex.withLock { sendAndWaitAck(cmd) }
    }

    override suspend fun readFrequencyAndMode(): Pair<Long, String>? = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            val cmd = IcomCivProtocol.buildReadFreqCommand()
            Log.d(tag, "CMD readFreq → ${IcomCivProtocol.toHex(cmd)}")
            val payload = sendAndReadResponse(cmd, IcomCivProtocol.CMD_READ_FREQ) ?: return@withContext null
            // Read-freq reply payload: [cmd byte already stripped by parseResponse] [5 freq bytes] [mode] [filter]
            IcomCivProtocol.parseFreqModePayload(payload).also {
                if (it != null) Log.d(tag, "readFreqMode: ${it.first}Hz, ${it.second}")
                else Log.w(tag, "readFreqMode: parse failed, payload=${IcomCivProtocol.toHex(payload)}")
            }
        }
    }

    override suspend fun pttOn(): Boolean = withContext(Dispatchers.IO) {
        Log.w(tag, "pttOn: not used for IC-705")
        true
    }

    override suspend fun pttOff(): Boolean = withContext(Dispatchers.IO) {
        Log.w(tag, "pttOff: not used for IC-705")
        true
    }

    // ── IRadioController – IC-705 extended operations ───────────────────────

    /** Select the band for [frequencyHz] via CMD 0x1A sub 0x00 (band stacking register). */
    override suspend fun setBand(frequencyHz: Long): Boolean = withContext(Dispatchers.IO) {
        val cmd = IcomCivProtocol.buildBandSelectCommand(frequencyHz) ?: run {
            Log.w(tag, "setBand: no band code for ${frequencyHz}Hz — skipping")
            return@withContext false
        }
        Log.d(tag, "CMD setBand (${frequencyHz}Hz) → ${IcomCivProtocol.toHex(cmd)}")
        ioMutex.withLock { sendAndWaitAck(cmd) }
    }

    /** Select VFO-A (main/RX) or VFO-B (sub/TX). */
    override suspend fun setVfo(vfoA: Boolean): Boolean = withContext(Dispatchers.IO) {
        val cmd = if (vfoA) IcomCivProtocol.buildSelectVfoACommand()
                  else      IcomCivProtocol.buildSelectVfoBCommand()
        Log.d(tag, "CMD selectVFO${if (vfoA) "A" else "B"} → ${IcomCivProtocol.toHex(cmd)}")
        ioMutex.withLock { sendAndWaitAck(cmd) }
    }

    /**
     * Enable or disable SPLIT mode (TX on sub-VFO while listening on main VFO).
     */
    override suspend fun setSplitMode(enabled: Boolean): Boolean = withContext(Dispatchers.IO) {
        val cmd = IcomCivProtocol.buildSplitModeCommand(enabled)
        Log.d(tag, "CMD split ${if (enabled) "ON" else "OFF"} → ${IcomCivProtocol.toHex(cmd)}")
        ioMutex.withLock { sendAndWaitAck(cmd) }
    }

    /**
     * Set the frequency of the **currently active** VFO (CMD 0x25 sub 0x00).
     * In split mode the radio automatically switches active VFO on PTT, so
     * always writing to the active VFO is the correct strategy.
     */
    override suspend fun setWorkingFrequency(frequencyHz: Long): Boolean = withContext(Dispatchers.IO) {
        Log.d(tag, "setWorkingFrequency (0x25/00): ${frequencyHz}Hz")
        val cmd = IcomCivProtocol.buildSetWorkingFreqCommand(frequencyHz)
        Log.d(tag, "CMD setWorkingFreq → ${IcomCivProtocol.toHex(cmd)}")
        ioMutex.withLock { sendAndWaitAck(cmd) }
    }

    /**
     * Set TX VFO frequency via CMD 0x25 sub 0x01 (unselected VFO).
     * Sent every tracking cycle in split mode alongside [setWorkingFrequency].
     */
    override suspend fun setTxVfoFrequency(frequencyHz: Long): Boolean = withContext(Dispatchers.IO) {
        Log.d(tag, "setTxVfoFrequency (0x25/01): ${frequencyHz}Hz")
        val cmd = IcomCivProtocol.buildSetUnselectedVfoFreqCommand(frequencyHz)
        Log.d(tag, "CMD setTxVfoFreq → ${IcomCivProtocol.toHex(cmd)}")
        ioMutex.withLock { sendAndWaitAck(cmd) }
    }

    /**
     * Read the frequency of the currently active VFO (CMD 0x25 sub 0x00).
     * Used for tuning detection in split mode.
     */
    override suspend fun readWorkingFrequency(): Long? = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            val cmd = IcomCivProtocol.buildReadWorkingFreqCommand()
            Log.d(tag, "CMD readWorkingFreq → ${IcomCivProtocol.toHex(cmd)}")
            val payload = sendAndReadResponse(cmd, IcomCivProtocol.CMD_SELECTED_VFO_FREQ) ?: return@withContext null
            // Response payload: [sub] [5 freq bytes] — CMD byte already stripped by parseResponse
            Log.d(tag, "readWorkingFreq: got ${payload.size} bytes: ${IcomCivProtocol.toHex(payload)}")
            if (payload.size < 6) {
                Log.w(tag, "readWorkingFreq: payload too short (${payload.size} bytes)")
                return@withContext null
            }
            val freqBcd = payload.sliceArray(1..5)
            val freq = IcomCivProtocol.decodeFrequencyBcd(freqBcd)
            Log.d(tag, "readWorkingFreq: ${freq}Hz")
            freq
        }
    }

    /**
     * Read the frequency of the inactive/TX VFO (CMD 0x25 sub 0x01).
     * Used for tuning detection in split mode.
     */
    override suspend fun readTxVfoFrequency(): Long? = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            val cmd = IcomCivProtocol.buildReadTxVfoFreqCommand()
            Log.d(tag, "CMD readTxVfoFreq → ${IcomCivProtocol.toHex(cmd)}")
            val payload = sendAndReadResponse(cmd, IcomCivProtocol.CMD_SELECTED_VFO_FREQ) ?: return@withContext null
            // Response payload: [sub] [5 freq bytes] — CMD byte already stripped by parseResponse
            Log.d(tag, "readTxVfoFreq: got ${payload.size} bytes: ${IcomCivProtocol.toHex(payload)}")
            if (payload.size < 6) {
                Log.w(tag, "readTxVfoFreq: payload too short (${payload.size} bytes)")
                return@withContext null
            }
            val freqBcd = payload.sliceArray(1..5)
            val freq = IcomCivProtocol.decodeFrequencyBcd(freqBcd)
            Log.d(tag, "readTxVfoFreq: ${freq}Hz")
            freq
        }
    }

    // ── Internal I/O helpers ────────────────────────────────────────────────

    /**
     * Write [cmd] to the radio and drain the input stream for up to
     * [ACK_TIMEOUT_MS], looking for an OK/NG acknowledgement frame.
     */
    private suspend fun sendAndWaitAck(cmd: ByteArray): Boolean {
        if (!write(cmd)) return false
        delay(WRITE_SETTLE_MS)
        val buf = drainWithTimeout(ACK_TIMEOUT_MS)
        val ok  = IcomCivProtocol.containsAck(buf)
        if (!ok) Log.w(tag, "ACK not found in ${buf.size} bytes: ${IcomCivProtocol.toHex(buf)}")
        return ok
    }

    /**
     * Write [cmd] to the radio and drain the input stream for up to
     * [ACK_TIMEOUT_MS], scanning for a response frame carrying [expectCmd].
     * Returns the payload bytes of that frame, or null on timeout/error.
     */
    private suspend fun sendAndReadResponse(cmd: ByteArray, expectCmd: Byte): ByteArray? {
        if (!write(cmd)) return null
        delay(WRITE_SETTLE_MS)
        val buf      = drainWithTimeout(ACK_TIMEOUT_MS)
        val response = IcomCivProtocol.parseResponse(buf, expectCmd)
        if (response == null) {
            Log.w(tag, "No response for cmd 0x${String.format("%02X", expectCmd.toInt() and 0xFF)} " +
                    "in ${buf.size} bytes: ${IcomCivProtocol.toHex(buf)}")
        }
        return response?.payload
    }

    /**
     * Drain whatever bytes the radio has buffered within a [timeoutMs] window.
     * Exits early as soon as a complete CI-V frame addressed to us is present
     * in the buffer (i.e., FE FE E0 A4 … FD), so we don't waste the remaining
     * timeout on responses that already arrived.
     */
    private suspend fun drainWithTimeout(timeoutMs: Long): ByteArray {
        val result   = mutableListOf<Byte>()
        val deadline = System.currentTimeMillis() + timeoutMs
        val stream   = inputStream ?: return ByteArray(0)
        while (System.currentTimeMillis() < deadline) {
            try {
                val available = stream.available()
                if (available > 0) {
                    val chunk = ByteArray(available)
                    val read  = stream.read(chunk)
                    if (read > 0) {
                        result.addAll(chunk.take(read))
                        // Exit early once we have a complete frame for us
                        if (hasCompleteFrameForUs(result)) break
                    }
                } else {
                    delay(POLL_INTERVAL_MS)
                }
            } catch (e: Exception) {
                Log.e(tag, "Drain error: ${e.message}")
                isConnected = false
                break
            }
        }
        return result.toByteArray()
    }

    /**
     * Returns true if [buf] contains a complete CI-V frame addressed to the
     * controller (FE FE [ADDR_CTRL] [ADDR_IC705] … FD).
     * CI-V data bytes cannot be 0xFD, so the first 0xFD after the header is
     * always the frame terminator.
     */
    private fun hasCompleteFrameForUs(buf: List<Byte>): Boolean {
        var i = 0
        while (i < buf.size - 4) {
            if (buf[i]     == IcomCivProtocol.PREAMBLE    &&
                buf[i + 1] == IcomCivProtocol.PREAMBLE    &&
                buf[i + 2] == IcomCivProtocol.ADDR_CTRL   &&
                buf[i + 3] == IcomCivProtocol.ADDR_IC705
            ) {
                for (k in i + 4 until buf.size) {
                    if (buf[k] == IcomCivProtocol.END_OF_MSG) return true
                }
                return false  // header found but no FD yet
            }
            i++
        }
        return false
    }

    private fun write(bytes: ByteArray): Boolean {
        return try {
            outputStream?.write(bytes)
            outputStream?.flush()
            true
        } catch (e: Exception) {
            Log.e(tag, "Write error: ${e.message}")
            isConnected = false
            false
        }
    }
}
