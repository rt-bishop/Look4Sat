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
import android.util.Log
import java.io.OutputStream
import java.util.*
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.rtbishop.look4sat.domain.repository.IReporterParams
import com.rtbishop.look4sat.domain.repository.IReporterRepo

data class WithoutExtParams(
    val dummy: Int
) : IReporterParams

class BluetoothReporter(
    private val bluetoothManager: BluetoothManager,
    private val reporterScope: CoroutineScope
) : IReporterRepo<WithoutExtParams> {

    private val tag = "BTReporter"
    private val sppid: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
    private lateinit var rotationOutputStream: OutputStream
    private var rotationConnectionJob: Job? = null
    private var rotationReportingJob: Job? = null
    private var rotationConnected = false
    private var rotationConnecting = false
    private lateinit var frequencyOutputStream: OutputStream
    private var frequencyConnectionJob: Job? = null
    private var frequencyReportingJob: Job? = null
    private var frequencyConnected = false
    private var frequencyConnecting = false

    fun isRotationConnected(): Boolean = rotationConnected
    fun isRotationConnecting(): Boolean = rotationConnecting
    fun isFrequencyConnected(): Boolean = frequencyConnected
    fun isFrequencyConnecting(): Boolean = frequencyConnecting

    fun connectBTRotatorDevice(deviceId: String) {
        if (!rotationConnected) {
            rotationConnectionJob = reporterScope.launch {
                try {
                    bluetoothManager.adapter.getRemoteDevice(deviceId)?.let { device ->
                        device.createInsecureRfcommSocketToServiceRecord(sppid)?.let { socket ->
                            rotationConnecting = true
                            socket.connect()
                            rotationOutputStream = socket.outputStream
                            rotationConnected = true
                            rotationConnecting = false
                            Log.i(tag, "$tag: Connected!")
                        }
                    }
                } catch (e: SecurityException) {
                    Log.e(tag, "$tag: ${e.message}")
                } catch (e: Exception) {
                    Log.e(tag, "$tag: ${e.message}")
                }
            }
        }
    }

    fun connectBTFrequencyDevice(deviceId: String) {
        if (!frequencyConnected) {
            frequencyConnectionJob = reporterScope.launch {
                try {
                    bluetoothManager.adapter.getRemoteDevice(deviceId)?.let { device ->
                        device.createInsecureRfcommSocketToServiceRecord(sppid)?.let { socket ->
                            frequencyConnecting = true
                            socket.connect()
                            frequencyOutputStream = socket.outputStream
                            frequencyConnected = true
                            frequencyConnecting = false
                            Log.i(tag, "$tag: Frequency Connected!")
                        }
                    }
                } catch (e: SecurityException) {
                    Log.e(tag, "$tag: ${e.message}")
                } catch (e: Exception) {
                    Log.e(tag, "$tag: ${e.message}")
                    frequencyConnected = false
                    frequencyConnecting = false
                }
            }
        }
    }

    override fun reportRotation(format: String, azimuth: Double, elevation: Double, params: WithoutExtParams) {
        if (rotationConnected) {
            rotationReportingJob = reporterScope.launch {
                val newElevation = if (elevation > 0.0) elevation else 0.0
                try {
                    val azimuthString = intToStringWithLeadingZeroes(azimuth.toInt())
                    val elevationString = intToStringWithLeadingZeroes(newElevation.toInt())
                    val crChar = '\r'
                    val nlChar = '\n'
                    val tbChar = '\t'
                    var buffer = format.replace("\$AZ", azimuthString)
                    buffer = buffer.replace("\$EL", elevationString)
                    buffer = buffer.replace("\\r", crChar.toString())
                    buffer = buffer.replace("\\n", nlChar.toString())
                    buffer = buffer.replace("\\t", tbChar.toString())
                    Log.i(tag, "$tag: Sending $buffer")
                    if (rotationConnected) rotationOutputStream.write(buffer.toByteArray())
                } catch (e: Exception) {
                    Log.e(tag, "$tag: ${e.message}")
                    rotationConnected = false
                }
            }
        }
    }

    override fun reportFrequency(format: String, frequency: Long, params: WithoutExtParams) {
        if (frequencyConnected) {
            frequencyReportingJob = reporterScope.launch {
                try {
                    val crChar = '\r'
                    val nlChar = '\n'
                    val tbChar = '\t'
                    var buffer = format.replace("\$FREQ", frequency.toString())
                    buffer = buffer.replace("\\r", crChar.toString())
                    buffer = buffer.replace("\\n", nlChar.toString())
                    buffer = buffer.replace("\\t", tbChar.toString())
                    Log.i(tag, "$tag: Sending $buffer")
                    if (frequencyConnected) {
                        frequencyOutputStream.write(buffer.toByteArray())
                    }
                } catch (e: Exception) {
                    Log.e(tag, "$tag: ${e.message}")
                    frequencyConnected = false
                }
            }
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
