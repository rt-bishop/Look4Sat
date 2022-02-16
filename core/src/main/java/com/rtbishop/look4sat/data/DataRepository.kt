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
package com.rtbishop.look4sat.data

import com.rtbishop.look4sat.domain.DataParser
import com.rtbishop.look4sat.domain.IDataRepository
import com.rtbishop.look4sat.domain.model.DataState
import com.rtbishop.look4sat.domain.model.SatEntry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.InputStream
import java.util.zip.ZipInputStream

class DataRepository(
    private val dataParser: DataParser,
    private val localSource: ILocalSource,
    private val remoteSource: IRemoteSource,
    private val repositoryScope: CoroutineScope
) : IDataRepository {

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        _updateState.value = DataState.Error(exception.message)
    }
    private val updateStateDelay = 875L
    private val _updateState = MutableStateFlow<DataState<Long>>(DataState.Handled)
    override val updateState: StateFlow<DataState<Long>> = _updateState

    override fun setUpdateStateHandled() {
        _updateState.value = DataState.Handled
    }

    override suspend fun getEntriesWithModes() = localSource.getEntriesWithModes()

    override suspend fun getSelectedEntries() = localSource.getSelectedEntries()

    override suspend fun getRadios(catnum: Int) = localSource.getRadios(catnum)

    override fun updateFromFile(uri: String) {
        repositoryScope.launch(exceptionHandler) {
            _updateState.value = DataState.Loading
            localSource.getFileStream(uri)?.let { fileStream ->
                localSource.insertEntries(importSatellites(fileStream))
                delay(updateStateDelay)
            }
            _updateState.value = DataState.Success(0L)
        }
    }

    override fun updateFromWeb(sources: List<String>) {
        _updateState.value = DataState.Loading
        repositoryScope.launch(exceptionHandler) {
            localSource.setDataSources(sources)
        }
        repositoryScope.launch(exceptionHandler) {
            val jobsMap = mutableMapOf<String, Deferred<InputStream?>>()
            val streamsMap = mutableMapOf<String, InputStream?>()
            val streams = mutableListOf<InputStream>()
            val entries = mutableListOf<SatEntry>()
            sources.forEach { jobsMap[it] = async { remoteSource.getFileStream(it) } }
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
            localSource.insertEntries(entries)
            _updateState.value = DataState.Success(0L)
        }
        repositoryScope.launch(exceptionHandler) {
            remoteSource.getFileStream(remoteSource.radioApi)?.let { stream ->
                localSource.insertRadios(dataParser.parseJSONStream(stream))
            }
        }
    }

    override fun clearAllData() {
        repositoryScope.launch {
            _updateState.value = DataState.Loading
            localSource.clearAllData()
            delay(updateStateDelay)
            _updateState.value = DataState.Success(0L)
        }
    }

    override suspend fun getDataSources() = localSource.getDataSources()

    override suspend fun setEntriesSelection(catnums: List<Int>) {
        localSource.setEntriesSelection(catnums)
    }

    private suspend fun importSatellites(stream: InputStream): List<SatEntry> {
        return dataParser.parseTLEStream(stream).map { tle -> SatEntry(tle) }
    }
}
