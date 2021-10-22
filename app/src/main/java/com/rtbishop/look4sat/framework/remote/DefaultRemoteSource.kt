package com.rtbishop.look4sat.framework.remote

import com.rtbishop.look4sat.data.RemoteDataSource
import com.rtbishop.look4sat.domain.model.Transmitter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.URL

class DefaultRemoteSource(private val dispatcher: CoroutineDispatcher) : RemoteDataSource {

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun fetchFileStream(url: String): InputStream? = withContext(dispatcher) {
        return@withContext URL(url).openStream()
    }

    override suspend fun fetchTransmitters(url: String): List<Transmitter> {
        val stream = URL(url).openStream()
        return emptyList()
    }

    private suspend fun download(url: String): InputStream? = withContext(dispatcher) {
        return@withContext try {
            URL(url).openStream().bufferedReader().useLines { lines ->
                lines.forEachIndexed { index, line ->
                    val items = line.split(",")
                }
            }
            null
        } catch (e: Exception) {
            return@withContext null
        }
    }
}
