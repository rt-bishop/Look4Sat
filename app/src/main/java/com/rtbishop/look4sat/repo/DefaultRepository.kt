/*
 * Look4Sat. Amateur radio and weather satellite tracker and passes predictor for Android.
 * Copyright (C) 2019, 2020 Arty Bishop (bishop.arty@gmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.rtbishop.look4sat.repo

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.LiveData
import com.github.amsacode.predict4java.TLE
import com.rtbishop.look4sat.data.SatEntry
import com.rtbishop.look4sat.data.SatTrans
import com.rtbishop.look4sat.data.TleSource
import com.rtbishop.look4sat.repo.local.EntriesDao
import com.rtbishop.look4sat.repo.local.SourcesDao
import com.rtbishop.look4sat.repo.local.TransmittersDao
import com.rtbishop.look4sat.repo.remote.TransmittersApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import javax.inject.Inject

class DefaultRepository @Inject constructor(
    private val resolver: ContentResolver,
    private val client: OkHttpClient,
    private val api: TransmittersApi,
    private val entriesDao: EntriesDao,
    private val sourcesDao: SourcesDao,
    private val transmittersDao: TransmittersDao
) : Repository {

    override fun getSources(): LiveData<List<TleSource>> {
        return sourcesDao.getSources()
    }

    override suspend fun updateSources(sources: List<TleSource>) {
        sourcesDao.updateSources(sources)
    }

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

    override suspend fun getTransmittersForSatId(satId: Int): List<SatTrans> {
        return transmittersDao.getTransmittersForSatId(satId)
    }

    override suspend fun updateTransmitters() {
        transmittersDao.updateTransmitters(api.getTransmitters())
    }
}
