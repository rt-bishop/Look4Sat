package com.rtbishop.look4sat.data.source

import android.content.ContentResolver
import android.net.Uri
import com.rtbishop.look4sat.domain.source.IFileSource
import java.io.InputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class FileSource(
    private val contentResolver: ContentResolver, private val dispatcher: CoroutineDispatcher
) : IFileSource {
    override suspend fun getFileStream(uri: String): InputStream? {
        return withContext(dispatcher) { contentResolver.openInputStream(Uri.parse(uri)) }
    }
}
