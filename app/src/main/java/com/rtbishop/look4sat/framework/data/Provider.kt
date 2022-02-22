package com.rtbishop.look4sat.framework.data

import android.content.ContentResolver
import android.net.Uri
import com.rtbishop.look4sat.data.IProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.URL

class Provider(
    private val contentResolver: ContentResolver,
    private val ioDispatcher: CoroutineDispatcher
) : IProvider {

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun getLocalFileStream(uri: String): InputStream? {
        return withContext(ioDispatcher) { contentResolver.openInputStream(Uri.parse(uri)) }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun getRemoteFileStream(url: String): InputStream? {
        return withContext(ioDispatcher) { URL(url).openStream() }
    }
}
