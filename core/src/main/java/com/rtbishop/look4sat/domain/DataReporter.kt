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

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.net.Socket

class DataReporter(private val reporterDispatcher: CoroutineDispatcher) {

    private var radioSocket: Socket? = null
    private var radioWriter: BufferedWriter? = null
    private var rotatorSocket: Socket? = null
    private var rotatorWriter: BufferedWriter? = null

    suspend fun setupRadioSocket(server: String, port: Int) {
        withContext(reporterDispatcher) {
            runCatching {
                radioSocket = Socket(server, port)
                radioWriter = rotatorSocket?.getOutputStream()?.bufferedWriter()
            }.onFailure { println(it.localizedMessage) }
        }
    }

    suspend fun reportFrequency(freq: Long) {
        withContext(reporterDispatcher) {
            runCatching {
                rotatorWriter?.let { writer ->
                    writer.write("\\set_freq $freq")
                    writer.newLine()
                    writer.flush()
                }
            }.onFailure { println(it.localizedMessage) }
        }
    }

    suspend fun setupRotatorSocket(server: String, port: Int) {
        withContext(reporterDispatcher) {
            runCatching {
                rotatorSocket = Socket(server, port)
                rotatorWriter = rotatorSocket?.getOutputStream()?.bufferedWriter()
            }.onFailure { println(it.localizedMessage) }
        }
    }

    suspend fun reportRotation(azim: Double, elev: Double) {
        withContext(reporterDispatcher) {
            runCatching {
                rotatorWriter?.let { writer ->
                    writer.write("\\set_pos $azim $elev")
                    writer.newLine()
                    writer.flush()
                }
            }.onFailure { println(it.localizedMessage) }
        }
    }
}
