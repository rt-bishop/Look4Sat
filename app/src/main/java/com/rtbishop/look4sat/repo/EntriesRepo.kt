/*******************************************************************************
 Look4Sat. Amateur radio satellite tracker and pass predictor.
 Copyright (C) 2019, 2020 Arty Bishop (bishop.arty@gmail.com)

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along
 with this program; if not, write to the Free Software Foundation, Inc.,
 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 ******************************************************************************/

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

class EntriesRepo @Inject constructor(
    private val resolver: ContentResolver,
    private val client: OkHttpClient,
    private val entriesDao: EntriesDao
) {
    
    fun getEntries(): LiveData<List<SatEntry>> {
        return entriesDao.getEntries()
    }
    
    suspend fun updateEntriesFromFile(fileUri: Uri) {
        withContext(Dispatchers.IO) {
            kotlin.runCatching {
                resolver.openInputStream(fileUri)?.use { stream ->
                    val importedEntries = TLE.importSat(stream).map { SatEntry(it) }
                    val selection = entriesDao.getEntriesSelection()
                    entriesDao.insertEntries(importedEntries)
                    entriesDao.updateEntriesSelection(selection)
                }
            }
        }
    }
    
    suspend fun updateEntriesFromSources(sources: List<TleSource>) {
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
            entriesDao.insertEntries(importedEntries)
            entriesDao.updateEntriesSelection(selection)
        }
    }
    
    suspend fun updateEntriesSelection(satIds: List<Int>) {
        entriesDao.updateEntriesSelection(satIds)
    }
}