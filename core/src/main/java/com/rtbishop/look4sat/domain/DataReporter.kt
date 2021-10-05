/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2021 Arty Bishop (bishop.arty@gmail.com)
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
package com.rtbishop.look4sat.domain

import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

class DataReporter(reporterDispatcher: CoroutineDispatcher) {

    private val reporterScope = CoroutineScope(reporterDispatcher)
    private var frequencySocketChannel: SocketChannel? = null
    private var rotationSocketChannel: SocketChannel? = null
    private var frequencyReporting: Job? = null
    private var rotationReporting: Job? = null

    fun reportFrequency(server: String, port: Int, frequency: Long) {
        frequencyReporting = reporterScope.launch {
            runCatching {
                if (frequencySocketChannel == null) {
                    frequencySocketChannel = SocketChannel.open(InetSocketAddress(server, port))
                } else {
                    val buffer = ByteBuffer.wrap("\\set_freq $frequency\n".toByteArray())
                    frequencySocketChannel?.write(buffer)
                }
            }.onFailure { error ->
                println(error.localizedMessage)
                frequencySocketChannel = null
                frequencyReporting?.cancelAndJoin()
            }
        }
    }

    fun reportRotation(server: String, port: Int, azimuth: Double, elevation: Double) {
        rotationReporting = reporterScope.launch {
            runCatching {
                if (rotationSocketChannel == null) {
                    rotationSocketChannel = SocketChannel.open(InetSocketAddress(server, port))
                } else {
                    val buffer = ByteBuffer.wrap("\\set_pos $azimuth $elevation\n".toByteArray())
                    rotationSocketChannel?.write(buffer)
                }
            }.onFailure { error ->
                println(error.localizedMessage)
                rotationSocketChannel = null
                rotationReporting?.cancelAndJoin()
            }
        }
    }
}
