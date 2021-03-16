/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2021 Arty Bishop (bishop.arty@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.rtbishop.look4sat.repository

import android.content.ContentResolver
import android.net.Uri
import com.github.amsacode.predict4java.Satellite
import com.github.amsacode.predict4java.TLE
import com.rtbishop.look4sat.data.SatEntry
import com.rtbishop.look4sat.data.SatTrans
import com.rtbishop.look4sat.data.TleSource
import com.rtbishop.look4sat.di.IoDispatcher
import com.rtbishop.look4sat.repository.localData.SatelliteDao
import com.rtbishop.look4sat.repository.remoteData.SatelliteService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SatelliteRepo @Inject constructor(
    private val resolver: ContentResolver,
    private val satelliteDao: SatelliteDao,
    private val satelliteService: SatelliteService,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    val satDataFlow = satelliteDao.getSatItems()

    fun getTransmittersForSat(catNum: Int): Flow<List<SatTrans>> {
        return satelliteDao.getTransmittersForSat(catNum)
    }

    suspend fun getSelectedSatellites(): List<Satellite> {
        return satelliteDao.getSelectedSatellites()
    }

    suspend fun updateEntriesSelection(catNums: List<Int>) {
        satelliteDao.updateEntriesSelection(catNums)
    }

    suspend fun updateSatDataFromFile(uri: Uri) {
        runCatching {
            resolver.openInputStream(uri)?.use { stream ->
                val entries = importEntriesFromStreams(listOf(stream))
                insertEntriesAndRestoreSelection(entries)
            }
        }.onFailure { throwable: Throwable ->
            throw throwable
        }
    }

    suspend fun updateSatDataFromWeb(sources: List<TleSource>) {
        coroutineScope {
            launch { updateEntriesFromSources(sources) }
            launch { updateTransmitters() }
        }
    }

    private suspend fun updateEntriesFromSources(sources: List<TleSource>) {
        val streams = getStreamsForSources(sources)
        val entries = importEntriesFromStreams(streams)
        insertEntriesAndRestoreSelection(entries)
    }

    private suspend fun updateTransmitters() {
        val transmitters = satelliteService.fetchTransmitters()
        satelliteDao.insertTransmitters(transmitters)
    }

    private suspend fun getStreamsForSources(sources: List<TleSource>): List<InputStream> {
        val streams = mutableListOf<InputStream>()
        sources.forEach { tleSource ->
            satelliteService.fetchFile(tleSource.url).body()?.byteStream()?.let { inputStream ->
                if (tleSource.url.contains(".zip", true)) {
                    streams.add(getZipStream(inputStream))
                } else {
                    streams.add(inputStream)
                }
            }
        }
        return streams
    }

    private suspend fun getZipStream(inputStream: InputStream): InputStream {
        var stream: InputStream
        withContext(ioDispatcher) {
            ZipInputStream(inputStream).apply {
                nextEntry
                stream = this
            }
        }
        return stream
    }

    private suspend fun importEntriesFromStreams(streams: List<InputStream>): List<SatEntry> {
        val importedEntries = mutableListOf<SatEntry>()
        withContext(ioDispatcher) {
            streams.forEach { stream ->
                val entries = TLE.importSat(stream).map { tle -> SatEntry(tle) }
                importedEntries.addAll(entries)
            }
        }
        return importedEntries
    }

    private suspend fun insertEntriesAndRestoreSelection(entries: List<SatEntry>) {
        val selectedCatNums = satelliteDao.getSelectedCatNums()
        satelliteDao.insertEntries(entries)
        satelliteDao.updateEntriesSelection(selectedCatNums)
    }
}