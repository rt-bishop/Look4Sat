package com.rtbishop.look4sat.presentation.radarScreen

import android.bluetooth.BluetoothManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.util.*
import kotlin.math.abs

class BTReporter(private val manager: BluetoothManager, private val reporterScope: CoroutineScope) {

    private val tag = "BTReporter"
    private val sppid: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
    private lateinit var outputStream: OutputStream
    private var rotationConnectionJob: Job? = null
    private var rotationReportingJob: Job? = null
    private var connected = false
    private var connecting = false

    fun isConnected(): Boolean = connected

    fun isConnecting(): Boolean = connecting

    fun connectBTDevice(deviceId: String) {
        if (!connected) {
            rotationConnectionJob = reporterScope.launch {
                try {
                    manager.adapter.getRemoteDevice(deviceId)?.let { device ->
                        device.createInsecureRfcommSocketToServiceRecord(sppid)?.let { socket ->
                            connecting = true
                            socket.connect()
                            outputStream = socket.outputStream
                            connected = true
                            connecting = false
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

    fun reportRotation(format: String, azimuth: Int, elevation: Int) {
        if (connected) {
            rotationReportingJob = reporterScope.launch {
                val newElevation = if (elevation > 0) elevation else 0
                try {
                    val azimuthString = intToStringWithLeadingZeroes(azimuth)
                    val elevationString = intToStringWithLeadingZeroes(newElevation)
                    val crChar = '\r'
                    val nlChar = '\n'
                    val tbChar = '\t'
                    var buffer = format.replace("\$AZ", azimuthString)
                    buffer = buffer.replace("\$EL", elevationString)
                    buffer = buffer.replace("\\r", crChar.toString())
                    buffer = buffer.replace("\\n", nlChar.toString())
                    buffer = buffer.replace("\\t", tbChar.toString())
                    Log.i(tag, "$tag: Sending $buffer")
                    if (connected) outputStream.write(buffer.toByteArray())
                } catch (e: Exception) {
                    Log.e(tag, "$tag: ${e.message}")
                    connected = false
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
