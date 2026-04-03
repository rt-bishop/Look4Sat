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

import com.rtbishop.look4sat.core.domain.repository.IReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

class NetworkReporter(
    private val reporterScope: CoroutineScope,
    private val rotatorServer: String,
    private val rotatorPort: Int,
    private val frequencyServer: String,
    private val frequencyPort: Int
) : IReporter {

    private val writeMutex = Mutex()

    private var rotatorSocket: SocketChannel? = null
    private var rotatorConnected = false
    private var rotatorConnecting = false

    private var frequencySocket: SocketChannel? = null
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
            write(rotatorSocket, command) { rotatorConnected = false }
        }
    }

    override fun reportFrequency(format: String, frequency: Long) {
        reporterScope.launch {
            ensureFrequencyConnected()
            if (!frequencyConnected) return@launch
            val command = format.replace($$"$FREQ", frequency.toString())
            write(frequencySocket, command) { frequencyConnected = false }
        }
    }

    private fun ensureRotatorConnected() {
        if (rotatorConnected || rotatorConnecting || rotatorServer.isBlank()) return
        reporterScope.launch {
            try {
                rotatorConnecting = true
                rotatorSocket = SocketChannel.open(InetSocketAddress(rotatorServer, rotatorPort))
                rotatorConnected = true
                println("NetworkReporter: Rotator connected to $rotatorServer:$rotatorPort")
            } catch (e: Exception) {
                println("NetworkReporter rotator connect error: ${e.message}")
                rotatorConnected = false
            } finally {
                rotatorConnecting = false
            }
        }
    }

    private fun ensureFrequencyConnected() {
        if (frequencyConnected || frequencyConnecting || frequencyServer.isBlank()) return
        reporterScope.launch {
            try {
                frequencyConnecting = true
                frequencySocket = SocketChannel.open(InetSocketAddress(frequencyServer, frequencyPort))
                frequencyConnected = true
                println("NetworkReporter: Frequency connected to $frequencyServer:$frequencyPort")
            } catch (e: Exception) {
                println("NetworkReporter frequency connect error: ${e.message}")
                frequencyConnected = false
            } finally {
                frequencyConnecting = false
            }
        }
    }

    private suspend fun write(socket: SocketChannel?, command: String, onError: () -> Unit) {
        try {
            writeMutex.withLock {
                val buffer = ByteBuffer.wrap("$command\n".toByteArray())
                socket?.write(buffer)
            }
        } catch (e: Exception) {
            println("NetworkReporter write error: ${e.message}")
            onError()
        }
    }
}
