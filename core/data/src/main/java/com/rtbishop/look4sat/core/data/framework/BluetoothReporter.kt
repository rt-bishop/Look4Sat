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
import com.rtbishop.look4sat.core.domain.repository.IReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.OutputStream
import java.util.UUID

class BluetoothReporter(
    private val bluetoothManager: BluetoothManager,
    private val reporterScope: CoroutineScope,
    private val rotatorDeviceId: String,
    private val frequencyDeviceId: String
) : IReporter {

    private val tag = "BTReporter"
    private val sppId: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
    private val writeMutex = Mutex()

    private var rotatorSocket: BluetoothSocket? = null
    private var rotatorStream: OutputStream? = null
    private var rotatorConnected = false
    private var rotatorConnecting = false

    private var frequencySocket: BluetoothSocket? = null
    private var frequencyStream: OutputStream? = null
    private var frequencyConnected = false
    private var frequencyConnecting = false

    override fun reportRotation(format: String, azimuth: Double, elevation: Double) {
        reporterScope.launch {
            ensureRotatorConnected()
            if (!rotatorConnected) return@launch
            val el = if (elevation > 0.0) elevation else 0.0
            val command = format
                .replace($$"$AZ", azimuth.toString())
                .replace($$"$EL", el.toString())
                .unescapeControlChars()
            write(rotatorStream, command) { rotatorConnected = false }
        }
    }

    override fun reportFrequency(format: String, frequency: Long) {
        reporterScope.launch {
            ensureFrequencyConnected()
            if (!frequencyConnected) return@launch
            val command = format
                .replace($$"$FREQ", frequency.toString())
                .unescapeControlChars()
            write(frequencyStream, command) { frequencyConnected = false }
        }
    }

    private fun ensureRotatorConnected() {
        if (rotatorConnected || rotatorConnecting || rotatorDeviceId.isBlank()) return
        reporterScope.launch {
            try {
                rotatorConnecting = true
                val device = bluetoothManager.adapter.getRemoteDevice(rotatorDeviceId)
                val socket = device.createInsecureRfcommSocketToServiceRecord(sppId)
                socket.connect()
                rotatorSocket = socket
                rotatorStream = socket.outputStream
                rotatorConnected = true
                Log.i(tag, "Rotator connected to $rotatorDeviceId")
            } catch (e: Exception) {
                Log.e(tag, "Rotator connect error: ${e.message}")
                rotatorConnected = false
            } finally {
                rotatorConnecting = false
            }
        }
    }

    private fun ensureFrequencyConnected() {
        if (frequencyConnected || frequencyConnecting || frequencyDeviceId.isBlank()) return
        reporterScope.launch {
            try {
                frequencyConnecting = true
                val device = bluetoothManager.adapter.getRemoteDevice(frequencyDeviceId)
                val socket = device.createInsecureRfcommSocketToServiceRecord(sppId)
                socket.connect()
                frequencySocket = socket
                frequencyStream = socket.outputStream
                frequencyConnected = true
                Log.i(tag, "Frequency connected to $frequencyDeviceId")
            } catch (e: Exception) {
                Log.e(tag, "Frequency connect error: ${e.message}")
                frequencyConnected = false
            } finally {
                frequencyConnecting = false
            }
        }
    }

    private suspend fun write(stream: OutputStream?, data: String, onError: () -> Unit) {
        try {
            writeMutex.withLock {
                stream?.write(data.toByteArray())
            }
        } catch (e: Exception) {
            Log.e(tag, "Write error: ${e.message}")
            onError()
        }
    }

    private fun String.unescapeControlChars(): String =
        replace("\\r", "\r").replace("\\n", "\n").replace("\\t", "\t")
}
