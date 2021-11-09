package com.rtbishop.look4sat.framework.remote

import com.rtbishop.look4sat.data.RemoteDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.URL

class RemoteSource(private val ioDispatcher: CoroutineDispatcher) : RemoteDataSource {

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun fetchFileStream(url: String): InputStream? {
        return try {
            withContext(ioDispatcher) { URL(url).openStream() }
        } catch (e: Exception) {
            null
        }
    }
}
