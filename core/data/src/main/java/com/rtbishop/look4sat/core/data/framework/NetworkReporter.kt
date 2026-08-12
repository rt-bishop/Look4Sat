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

import com.rtbishop.look4sat.core.domain.model.Constants
import com.rtbishop.look4sat.core.domain.repository.IReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
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
    private val frequencyPort: Int,
    private val frequencyOffsetHz: Long = 0L
) : IReporter {

    private val writeMutex = Mutex()
    private val connectionMutex = Mutex()
    private val frequencyCommands = Channel<String>(Channel.CONFLATED)

    private var rotatorSocket: SocketChannel? = null
    private var rotatorConnected = false

    private var frequencySocket: SocketChannel? = null
    private var frequencyConnected = false

    init {
        // Keep only the latest frequency command to avoid stale backlog and effective lag.
        reporterScope.launch {
            for (command in frequencyCommands) {
                ensureFrequencyConnected()
                if (!frequencyConnected) continue
                write(frequencySocket, command) { resetFrequencyConnection() }
            }
        }
    }

    override fun reportRotation(format: String, azimuth: Double, elevation: Double) {
        reporterScope.launch {
            ensureRotatorConnected()
            if (!rotatorConnected) return@launch
            val el = if (elevation > 0.0) elevation else 0.0
            val command = format
                .replace($$"$AZ", azimuth.toString())
                .replace($$"$EL", el.toString())
                .unescapeControlChars()
            write(rotatorSocket, command) { rotatorConnected = false }
        }
    }

    override fun reportFrequency(format: String, frequency: Long) {
        val clampedOffset = frequencyOffsetHz.coerceIn(
            Constants.FREQ_OFFSET_MIN_HZ,
            Constants.FREQ_OFFSET_MAX_HZ
        )
        val correctedFreq = frequency.coerceAtLeast(0L).safeAdd(clampedOffset).coerceAtLeast(0L)
        val command = format
            .replace($$"$FREQ", correctedFreq.toString())
            .unescapeControlChars()
        frequencyCommands.trySend(command)
    }

    private suspend fun ensureRotatorConnected() {
        connectionMutex.withLock {
            if (rotatorConnected || rotatorServer.isBlank()) return
            try {
                resetRotatorConnection()
                rotatorSocket = SocketChannel.open(InetSocketAddress(rotatorServer, rotatorPort))
                rotatorConnected = true
                println("NetworkReporter: Rotator connected to $rotatorServer:$rotatorPort")
            } catch (e: Exception) {
                println("NetworkReporter rotator connect error: ${e.message}")
                resetRotatorConnection()
            }
        }
    }

    private suspend fun ensureFrequencyConnected() {
        connectionMutex.withLock {
            if (frequencyConnected || frequencyServer.isBlank()) return
            try {
                resetFrequencyConnection()
                frequencySocket = SocketChannel.open(InetSocketAddress(frequencyServer, frequencyPort))
                frequencyConnected = true
                println("NetworkReporter: Frequency connected to $frequencyServer:$frequencyPort")
            } catch (e: Exception) {
                println("NetworkReporter frequency connect error: ${e.message}")
                resetFrequencyConnection()
            }
        }
    }

    private suspend fun write(socket: SocketChannel?, command: String, onError: () -> Unit) {
        try {
            writeMutex.withLock {
                val buffer = ByteBuffer.wrap("$command\n".toByteArray())
                while (buffer.hasRemaining()) {
                    socket?.write(buffer)
                }
            }
        } catch (e: Exception) {
            println("NetworkReporter write error: ${e.message}")
            onError()
        }
    }

    private fun resetRotatorConnection() {
        rotatorConnected = false
        closeQuietly(rotatorSocket)
        rotatorSocket = null
    }

    private fun resetFrequencyConnection() {
        frequencyConnected = false
        closeQuietly(frequencySocket)
        frequencySocket = null
    }

    private fun closeQuietly(socket: SocketChannel?) {
        try {
            socket?.close()
        } catch (_: Exception) {}
    }

    private fun Long.safeAdd(delta: Long): Long {
        return when {
            delta > 0 && this > Long.MAX_VALUE - delta -> Long.MAX_VALUE
            delta < 0 && this < Long.MIN_VALUE - delta -> Long.MIN_VALUE
            else -> this + delta
        }
    }

    private fun String.unescapeControlChars(): String =
        replace("\\r", "\r").replace("\\n", "\n").replace("\\t", "\t")
}
