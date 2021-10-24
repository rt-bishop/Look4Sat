package com.rtbishop.look4sat.framework.remote

import com.rtbishop.look4sat.data.RemoteDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.URL

class RemoteSource(private val ioDispatcher: CoroutineDispatcher) : RemoteDataSource {

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun fetchFileStream(fileUrl: String): InputStream {
        return withContext(ioDispatcher) { URL(fileUrl).openStream() }
    }
}
