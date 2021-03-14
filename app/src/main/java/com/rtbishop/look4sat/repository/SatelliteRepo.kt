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
import com.rtbishop.look4sat.data.*
import com.rtbishop.look4sat.di.ExternalScope
import com.rtbishop.look4sat.di.IoDispatcher
import com.rtbishop.look4sat.repository.localData.SatelliteDao
import com.rtbishop.look4sat.repository.remoteData.SatelliteService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.measureTimeMillis

@Singleton
class SatelliteRepo @Inject constructor(
    private val resolver: ContentResolver,
    private val satelliteDao: SatelliteDao,
    private val satelliteService: SatelliteService,
    @ExternalScope private val externalScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val _satData = MutableStateFlow<Result<List<SatItem>>>(Result.InProgress)
    val satData: Flow<Result<List<SatItem>>> = _satData

    init {
        loadEntriesFromDb()
    }

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
        _satData.value = Result.InProgress
        externalScope.launch {
            runCatching {
                resolver.openInputStream(uri)?.use { stream ->
                    val entries = importEntriesFromStreams(listOf(stream))
                    insertEntriesAndRestoreSelection(entries)
                }
            }.onFailure { throwable: Throwable ->
                Timber.d("$throwable")
                _satData.value = Result.Error(throwable)
            }
            loadEntriesFromDb()
        }
    }

    suspend fun updateSatDataFromWeb(sources: List<TleSource>) {
        _satData.value = Result.InProgress
        externalScope.launch {
            val updateTimeMillis = measureTimeMillis {
                runCatching {
                    val streams = externalScope.async { getStreamsForSources(sources) }
                    val transmitters = externalScope.async { satelliteService.fetchTransmitters() }
                    val entries = importEntriesFromStreams(streams.await())
                    satelliteDao.insertTransmitters(transmitters.await())
                    insertEntriesAndRestoreSelection(entries)
                }.onFailure { throwable: Throwable ->
                    Timber.d("$throwable")
                    _satData.value = Result.Error(throwable)
                }
                loadEntriesFromDb()
            }
            Timber.d("Update from Web took $updateTimeMillis ms")
        }
    }

    private fun loadEntriesFromDb() {
        _satData.value = Result.InProgress
        externalScope.launch {
            satelliteDao.getSatItems().collect { satItems ->
                delay(250)
                _satData.value = Result.Success(satItems)
            }
        }
    }

    private suspend fun getStreamsForSources(sources: List<TleSource>): List<InputStream> {
        val streams = mutableListOf<InputStream>()
        sources.forEach { tleSource ->
            satelliteService.fetchFile(tleSource.url).body()?.byteStream()?.let { inputStream ->
                if (tleSource.url.contains(".zip", true)) {
                    // Handle zip stream
                    val zipInputStream = ZipInputStream(inputStream)
                    val zipEntry = zipInputStream.nextEntry
                    if (zipEntry != null && !zipEntry.isDirectory) streams.add(zipInputStream)
                } else {
                    streams.add(inputStream)
                }
            }
        }
        return streams
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