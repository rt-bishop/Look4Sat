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

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.launch
import com.rtbishop.look4sat.domain.repository.IReporterParams
import com.rtbishop.look4sat.domain.repository.IReporterRepo

data class ExtendedParams(
    val server: String,
    val port: Int
) : IReporterParams

enum class NetworkService { ROTATOR, FREQUENCY }

data class SocketConnection(
    var socket: SocketChannel? = null,
    var connected: Boolean = false,
    var connecting: Boolean = false,
    var connectionJob: Job? = null
)

class NetworkReporter(private val reporterScope: CoroutineScope) : IReporterRepo<ExtendedParams> {
    private val serviceToAddress = mutableMapOf<NetworkService, String>()
    private val connections = mutableMapOf<String, SocketConnection>()
    private val writeMutex = Mutex()

    fun isConnected(service: NetworkService): Boolean {
        val addr = serviceToAddress[service] ?: return false
        return connections[addr]?.connected == true
    }

    private fun getOrCreateConnection(addr: String): SocketConnection {
        return connections.getOrPut(addr) { SocketConnection() }
    }

    fun connect(service: NetworkService, server: String, port: Int) {
        val addr = "$server:$port"
        serviceToAddress[service] = addr
        val connection = getOrCreateConnection(addr)
        if (connection.connected || connection.connecting) return
        connection.connectionJob = reporterScope.launch {
            try {
                connection.connecting = true
                val socket = SocketChannel.open(InetSocketAddress(server, port))
                connection.socket = socket
                connection.connected = true
                println("NetworkReporter: $service connected to $addr")
            } catch (e: Exception) {
                println("NetworkReporter connect error: ${e.message}")
                connection.connected = false
            } finally {
                connection.connecting = false
            }
        }
    }

    private suspend fun write(service: NetworkService, command: String) {
        val addr = serviceToAddress[service] ?: return
        val connection = connections[addr] ?: return
        if (!connection.connected) return
        try {
            writeMutex.withLock {
                val buffer = ByteBuffer.wrap("\\$command\n".toByteArray())
                connection.socket?.write(buffer)
            }
        } catch (e: Exception) {
            println("NetworkReporter write error: ${e.message}")
            connection.connected = false
        }
    }

    override fun reportRotation(format: String, azimuth: Double, elevation: Double, params: ExtendedParams) {
        reporterScope.launch {
            connect(NetworkService.ROTATOR, params.server, params.port)
            if (!isConnected(NetworkService.ROTATOR)) return@launch
            val el = if (elevation > 0.0) elevation else 0.0
            val command = format
                .replace("\$AZ", azimuth.toString())
                .replace("\$EL", el.toString())
            write(NetworkService.ROTATOR, command)
        }
    }

    override fun reportFrequency(format: String, frequency: Long, params: ExtendedParams) {
        reporterScope.launch {
            connect(NetworkService.FREQUENCY, params.server, params.port)
            if (!isConnected(NetworkService.FREQUENCY)) return@launch
            val command = format.replace("\$FREQ", frequency.toString())
            write(NetworkService.FREQUENCY, command)
        }
    }
}
