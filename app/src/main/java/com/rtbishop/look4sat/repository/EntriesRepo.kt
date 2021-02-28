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

package com.rtbishop.look4sat.repository

import android.content.ContentResolver
import android.net.Uri
import com.github.amsacode.predict4java.TLE
import com.rtbishop.look4sat.data.SatEntry
import com.rtbishop.look4sat.data.TleSource
import com.rtbishop.look4sat.di.IoDispatcher
import com.rtbishop.look4sat.repository.localData.EntriesDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import javax.inject.Inject

class EntriesRepo @Inject constructor(
    private val entriesDao: EntriesDao,
    private val client: OkHttpClient,
    private val resolver: ContentResolver,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    
    fun getEntries(): Flow<List<SatEntry>> {
        return entriesDao.getEntries()
    }
    
    suspend fun getSelectedEntries(): List<SatEntry> {
        return entriesDao.getSelectedEntries()
    }
    
    suspend fun updateEntriesSelection(catNums: List<Int>) {
        entriesDao.updateEntriesSelection(catNums)
    }
    
    suspend fun updateEntriesFromFile(uri: Uri) {
        withContext(ioDispatcher) {
            runCatching {
                resolver.openInputStream(uri)?.use { stream ->
                    val importedEntries = TLE.importSat(stream).map { tle -> SatEntry(tle) }
                    updateAndRestoreSelection(importedEntries)
                }
            }
        }
    }
    
    suspend fun updateEntriesFromWeb(sources: List<TleSource>) {
        withContext(ioDispatcher) {
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
            updateAndRestoreSelection(importedEntries)
        }
    }
    
    private suspend fun updateAndRestoreSelection(entries: List<SatEntry>) {
        val selectedCatNums = entriesDao.getSelectedCatNums()
        entriesDao.insertEntries(entries)
        entriesDao.updateEntriesSelection(selectedCatNums)
    }
}