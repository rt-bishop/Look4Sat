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
package com.rtbishop.look4sat.data.framework

import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.OutputStream
import java.util.*
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.rtbishop.look4sat.domain.repository.IReporterParams
import com.rtbishop.look4sat.domain.repository.IReporterRepo

data class WithoutExtParams(
    val dummy: Int
) : IReporterParams

enum class BtService { ROTATOR, FREQUENCY }

data class DeviceConnection(
    var socket: BluetoothSocket? = null,
    var outputStream: OutputStream? = null,
    var connected: Boolean = false,
    var connecting: Boolean = false,
    var connectionJob: Job? = null
)

class BluetoothReporter(
    private val bluetoothManager: BluetoothManager,
    private val reporterScope: CoroutineScope
) : IReporterRepo<WithoutExtParams> {

    private val tag = "BTReporter"
    private val sppid: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

    private val serviceToDevice = mutableMapOf<BtService, String>()
    private val deviceConnections = mutableMapOf<String, DeviceConnection>()
    private val writeMutex = Mutex()

    fun isConnected(service: BtService): Boolean {
        val deviceId = serviceToDevice[service] ?: return false
        return deviceConnections[deviceId]?.connected == true
    }
    fun isConnecting(service: BtService): Boolean {
        val deviceId = serviceToDevice[service] ?: return false
        return deviceConnections[deviceId]?.connecting == true
    }

    fun connect(service: BtService, deviceId: String) {
        serviceToDevice[service] = deviceId
        val connection = deviceConnections.getOrPut(deviceId) {
            DeviceConnection()
        }
        if (connection.connected || connection.connecting) return
        connection.connectionJob = reporterScope.launch {
            try {
                connection.connecting = true
                val device = bluetoothManager.adapter.getRemoteDevice(deviceId)
                val socket = device.createInsecureRfcommSocketToServiceRecord(sppid)
                socket.connect()
                connection.socket = socket
                connection.outputStream = socket.outputStream
                connection.connected = true
                Log.i(tag, "$tag: Connected to $deviceId")
            } catch (e: Exception) {
                Log.e(tag, "$tag: ${e.message}")
                connection.connected = false
            } finally {
                connection.connecting = false
            }
        }
    }

    private suspend fun write(service: BtService, buffer: String) {
        val deviceId = serviceToDevice[service] ?: return
        val connection = deviceConnections[deviceId] ?: return
        if (!connection.connected) return
        try {
            writeMutex.withLock {
                connection.outputStream?.write(buffer.toByteArray())
            }
        } catch (e: Exception) {
            Log.e(tag, "$tag: Write failed ${e.message}")
            connection.connected = false
        }
    }

    override fun reportRotation(format: String, azimuth: Double, elevation: Double, params: WithoutExtParams) {
        reporterScope.launch {
            if (!isConnected(BtService.ROTATOR)) return@launch
            val newElevation = if (elevation > 0.0) elevation else 0.0
            val azimuthString = intToStringWithLeadingZeroes(azimuth.toInt())
            val elevationString = intToStringWithLeadingZeroes(newElevation.toInt())
            var buffer = format
                .replace("\$AZ", azimuthString)
                .replace("\$EL", elevationString)
                .replace("\\r", "\r")
                .replace("\\n", "\n")
                .replace("\\t", "\t")
            write(BtService.ROTATOR, buffer)
        }
    }

    override fun reportFrequency(format: String, frequency: Long, params: WithoutExtParams) {
        reporterScope.launch {
            if (!isConnected(BtService.FREQUENCY)) return@launch
            var buffer = format
                .replace("\$FREQ", frequency.toString())
                .replace("\\r", "\r")
                .replace("\\n", "\n")
                .replace("\\t", "\t")
            write(BtService.FREQUENCY, buffer)
        }
    }

    private fun intToStringWithLeadingZeroes(value: Int): String {
        return if (value > 0) {
            if (value < 10) "00$value" else if (value < 100) "0$value" else "$value"
        } else {
            val absValue = abs(value)
            if (value > -10) "-00$absValue" else if (value > -100) "-0$absValue" else "-$absValue"
        }
    }
}
