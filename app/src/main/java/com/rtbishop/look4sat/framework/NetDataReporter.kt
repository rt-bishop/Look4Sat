package com.rtbishop.look4sat.framework

import com.rtbishop.look4sat.data.DataReporter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.net.Socket

class NetDataReporter(private val reporterDispatcher: CoroutineDispatcher) : DataReporter {

    private var rotatorSocket: Socket? = null
    private var rotatorWriter: BufferedWriter? = null

    override suspend fun setRotatorSocket(server: String, port: Int) {
        withContext(reporterDispatcher) {
            runCatching {
                rotatorSocket = Socket(server, port)
                rotatorWriter = rotatorSocket?.getOutputStream()?.bufferedWriter()
            }.onFailure { println(it.localizedMessage) }
        }
    }

    override suspend fun reportRotation(azim: Double, elev: Double) {
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
