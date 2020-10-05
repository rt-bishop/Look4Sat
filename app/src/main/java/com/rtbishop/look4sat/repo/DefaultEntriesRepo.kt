package com.rtbishop.look4sat.repo

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.LiveData
import com.github.amsacode.predict4java.TLE
import com.rtbishop.look4sat.data.SatEntry
import com.rtbishop.look4sat.data.TleSource
import com.rtbishop.look4sat.repo.local.EntriesDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import javax.inject.Inject

class DefaultEntriesRepo @Inject constructor(
    private val resolver: ContentResolver,
    private val client: OkHttpClient,
    private val entriesDao: EntriesDao
) : EntriesRepo {

    override fun getEntries(): LiveData<List<SatEntry>> {
        return entriesDao.getEntries()
    }

    override suspend fun updateEntriesFromFile(fileUri: Uri) {
        withContext(Dispatchers.IO) {
            resolver.openInputStream(fileUri)?.use { stream ->
                val importedEntries = TLE.importSat(stream).map { SatEntry(it) }
                val selection = entriesDao.getEntriesSelection()
                entriesDao.updateEntries(importedEntries)
                entriesDao.updateEntriesSelection(selection)
            }
        }
    }

    override suspend fun updateEntriesFromSources(sources: List<TleSource>) {
        withContext(Dispatchers.IO) {
            val streams = mutableListOf<InputStream>()
            sources.forEach { source ->
                val request = Request.Builder().url(source.url).build()
                val stream = client.newCall(request).execute().body()?.byteStream()
                stream?.let { inputStream -> streams.add(inputStream) }
            }
            val importedEntries = mutableListOf<SatEntry>()
            streams.forEach { stream ->
                val entries = TLE.importSat(stream).map { tle -> SatEntry(tle) }
                importedEntries.addAll(entries)
            }
            val selection = entriesDao.getEntriesSelection()
            entriesDao.updateEntries(importedEntries)
            entriesDao.updateEntriesSelection(selection)
        }
    }

    override suspend fun updateEntriesSelection(satIds: List<Int>) {
        entriesDao.updateEntriesSelection(satIds)
    }
}