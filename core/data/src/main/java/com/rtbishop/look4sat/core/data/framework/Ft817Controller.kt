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

class Ft817Controller(
    private val bluetoothManager: BluetoothManager,
    private val deviceAddress: String
) : IRadioController {

    private val tag = "FT817"
    private val sppId: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
    private val ioMutex = Mutex()
    private val commandDelayMs = 200L

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    override var isConnected: Boolean = false
        private set

    override suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        if (isConnected) return@withContext true
        if (deviceAddress.isBlank()) return@withContext false
        try {
            val device = bluetoothManager.adapter.getRemoteDevice(deviceAddress)
            val btSocket = device.createInsecureRfcommSocketToServiceRecord(sppId)
            btSocket.connect()
            socket = btSocket
            outputStream = btSocket.outputStream
            inputStream = btSocket.inputStream
            isConnected = true
            Log.i(tag, "Connected to $deviceAddress")
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
                inputStream = null
                outputStream = null
                socket = null
                isConnected = false
                Log.i(tag, "Disconnected from $deviceAddress")
            }
        }
    }

    override suspend fun setFrequency(frequencyHz: Long): Boolean = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            sendCommandWithAck(Ft817CatProtocol.buildSetFreqCommand(frequencyHz))
        }
    }

    override suspend fun setMode(mode: String): Boolean = withContext(Dispatchers.IO) {
        val cmd = Ft817CatProtocol.buildSetModeCommand(mode) ?: return@withContext false
        ioMutex.withLock { sendCommandWithAck(cmd) }
    }

    override suspend fun setCtcssMode(enabled: Boolean): Boolean = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            sendCommandWithAck(Ft817CatProtocol.buildCtcssModeCommand(enabled))
        }
    }

    override suspend fun setCtcssTone(toneHz: Double): Boolean = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            sendCommandWithAck(Ft817CatProtocol.buildSetCtcssToneCommand(toneHz))
        }
    }

    override suspend fun readFrequencyAndMode(): Pair<Long, String>? = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            val sent = sendCommand(Ft817CatProtocol.buildReadFreqModeCommand())
            if (!sent) return@withContext null
            delay(commandDelayMs)
            val response = readResponse(5) ?: return@withContext null
            Ft817CatProtocol.parseReadResponse(response)
        }
    }

    override suspend fun pttOn(): Boolean = withContext(Dispatchers.IO) {
        ioMutex.withLock { sendCommandWithAck(Ft817CatProtocol.buildPttOnCommand()) }
    }

    override suspend fun pttOff(): Boolean = withContext(Dispatchers.IO) {
        ioMutex.withLock { sendCommandWithAck(Ft817CatProtocol.buildPttOffCommand()) }
    }

    private suspend fun sendCommand(bytes: ByteArray): Boolean {
        return try {
            outputStream?.write(bytes) ?: return false
            outputStream?.flush()
            delay(commandDelayMs)
            true
        } catch (e: Exception) {
            Log.e(tag, "Send error: ${e.message}")
            isConnected = false
            false
        }
    }

    /** Send command and read the 1-byte ACK response (0x00 = OK). */
    private suspend fun sendCommandWithAck(bytes: ByteArray): Boolean {
        if (!sendCommand(bytes)) return false
        return try {
            val ack = inputStream?.read() ?: return false
            ack == 0x00
        } catch (e: Exception) {
            Log.e(tag, "ACK read error: ${e.message}")
            true // command was sent, ACK read failed - continue anyway
        }
    }

    private fun readResponse(length: Int): ByteArray? {
        return try {
            val buffer = ByteArray(length)
            var read = 0
            while (read < length) {
                val count = inputStream?.read(buffer, read, length - read) ?: return null
                if (count < 0) return null
                read += count
            }
            buffer
        } catch (e: Exception) {
            Log.e(tag, "Read error: ${e.message}")
            isConnected = false
            null
        }
    }
}
