/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2022 Arty Bishop (bishop.arty@gmail.com)
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
package com.rtbishop.look4sat.data

import com.rtbishop.look4sat.domain.IRepository
import com.rtbishop.look4sat.domain.model.DataState
import com.rtbishop.look4sat.domain.model.SatEntry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.InputStream
import java.util.zip.ZipInputStream

class Repository(
    private val dataParser: DataParser,
    private val storage: IStorage,
    private val provider: IProvider,
    private val repoScope: CoroutineScope
) : IRepository {

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        _updateState.value = DataState.Error(exception.message)
    }
    private val updateStateDelay = 875L
    private val _updateState = MutableStateFlow<DataState<Long>>(DataState.Handled)
    override val updateState: StateFlow<DataState<Long>> = _updateState

    override fun getEntriesTotal() = storage.getEntriesTotal()

    override fun getRadiosTotal() = storage.getRadiosTotal()

    override suspend fun getEntriesWithModes() = storage.getEntriesWithModes()

    override suspend fun getEntriesWithIds(ids: List<Int>) = storage.getEntriesWithIds(ids)

    override suspend fun getRadiosWithId(id: Int) = storage.getRadiosWithId(id)

    override fun updateFromFile(uri: String) {
        repoScope.launch(exceptionHandler) {
            _updateState.value = DataState.Loading
            provider.getLocalFileStream(uri)?.let { fileStream ->
                delay(updateStateDelay)
                storage.insertEntries(importSatellites(fileStream))
            }
            _updateState.value = DataState.Success(0L)
        }
    }

    override fun updateFromWeb(urls: List<String>) {
        _updateState.value = DataState.Loading
        repoScope.launch(exceptionHandler) {
            val jobsMap = mutableMapOf<String, Deferred<InputStream?>>()
            val streamsMap = mutableMapOf<String, InputStream?>()
            val streams = mutableListOf<InputStream>()
            val entries = mutableListOf<SatEntry>()
            urls.forEach { jobsMap[it] = async { provider.getRemoteFileStream(it) } }
            jobsMap.forEach { job -> streamsMap[job.key] = job.value.await() }
            streamsMap.forEach { stream ->
                stream.value?.let { inputStream ->
                    when {
                        stream.key.contains("=csv", true) -> {
                            val tles = dataParser.parseCSVStream(inputStream)
                            entries.addAll(tles.map { tle -> SatEntry(tle) })
                        }
                        stream.key.contains(".zip", true) -> {
                            streams.add(ZipInputStream(inputStream).apply { nextEntry })
                        }
                        else -> streams.add(inputStream)
                    }
                }
            }
            streams.forEach { stream -> entries.addAll(importSatellites(stream)) }
            storage.insertEntries(entries)
        }
        repoScope.launch(exceptionHandler) {
            provider.getRemoteFileStream(provider.radioApi)?.let { stream ->
                storage.insertRadios(dataParser.parseJSONStream(stream))
                _updateState.value = DataState.Success(0L)
            }
        }
    }

    override fun setUpdateStateHandled() {
        _updateState.value = DataState.Handled
    }

    override fun clearAllData() {
        repoScope.launch {
            _updateState.value = DataState.Loading
            delay(updateStateDelay)
            storage.clearAllData()
            _updateState.value = DataState.Success(0L)
        }
    }

    private suspend fun importSatellites(stream: InputStream): List<SatEntry> {
        return dataParser.parseTLEStream(stream).map { tle -> SatEntry(tle) }
    }
}
