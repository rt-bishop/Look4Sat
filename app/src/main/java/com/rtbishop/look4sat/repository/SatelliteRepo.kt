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
import com.rtbishop.look4sat.data.SatItem
import com.rtbishop.look4sat.data.SatTrans
import com.rtbishop.look4sat.data.TleSource
import com.rtbishop.look4sat.di.IoDispatcher
import com.rtbishop.look4sat.repository.localData.SatelliteDao
import com.rtbishop.look4sat.repository.remoteData.SatelliteService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject

class SatelliteRepo @Inject constructor(
    private val resolver: ContentResolver,
    private val satelliteDao: SatelliteDao,
    private val satelliteService: SatelliteService,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    
    fun getSatItems(): Flow<List<SatItem>> {
        return satelliteDao.getSatItems()
    }
    
    suspend fun getSelectedEntries(): List<SatEntry> {
        return satelliteDao.getSelectedEntries()
    }
    
    suspend fun updateEntriesSelection(catNums: List<Int>) {
        satelliteDao.updateEntriesSelection(catNums)
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
                val stream = satelliteService.fetchFile(source.url).body()?.byteStream()
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
    
    fun getTransmittersForSat(catNum: Int): Flow<List<SatTrans>> {
        return satelliteDao.getTransmittersForSat(catNum)
    }
    
    suspend fun updateTransmitters() {
        satelliteDao.insertTransmitters(satelliteService.fetchTransmitters())
    }
    
    private suspend fun updateAndRestoreSelection(entries: List<SatEntry>) {
        val selectedCatNums = satelliteDao.getSelectedCatNums()
        satelliteDao.insertEntries(entries)
        satelliteDao.updateEntriesSelection(selectedCatNums)
    }
}