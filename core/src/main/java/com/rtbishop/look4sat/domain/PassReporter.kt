package com.rtbishop.look4sat.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.net.Socket

class PassReporter(private val reporterDispatcher: CoroutineDispatcher) {

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
