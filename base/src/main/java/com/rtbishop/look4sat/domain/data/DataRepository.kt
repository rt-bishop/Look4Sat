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
package com.rtbishop.look4sat.domain.data

import com.rtbishop.look4sat.domain.IDataRepository
import com.rtbishop.look4sat.domain.model.DataState
import com.rtbishop.look4sat.domain.model.SatEntry
import com.rtbishop.look4sat.utility.DataParser
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.InputStream
import java.util.zip.ZipInputStream

class DataRepository(
    private val dataParser: DataParser,
    private val fileSource: IFileDataSource,
    private val entrySource: ILocalEntrySource,
    private val radioSource: ILocalRadioSource,
    private val remoteSource: IRemoteDataSource,
    private val repositoryScope: CoroutineScope
) : IDataRepository {

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        _updateState.value = DataState.Error(exception.message)
    }
    private val updateStateDelay = 875L
    private val _updateState = MutableStateFlow<DataState<Long>>(DataState.Handled)
    override val updateState: StateFlow<DataState<Long>> = _updateState

    override fun getEntriesTotal() = entrySource.getEntriesTotal()

    override fun getRadiosTotal() = radioSource.getRadiosTotal()

    override suspend fun getEntriesWithModes() = entrySource.getEntriesWithModes()

    override suspend fun getEntriesWithIds(ids: List<Int>) = entrySource.getEntriesWithIds(ids)

    override suspend fun getRadiosWithId(id: Int) = radioSource.getRadiosWithId(id)

    override fun updateFromFile(uri: String) {
        repositoryScope.launch(exceptionHandler) {
            _updateState.value = DataState.Loading
            fileSource.getDataStream(uri)?.let { stream ->
                delay(updateStateDelay)
                entrySource.insertEntries(importSatellites(stream))
            }
            _updateState.value = DataState.Success(0L)
        }
    }

    override fun updateFromWeb(urls: List<String>) {
        _updateState.value = DataState.Loading
        repositoryScope.launch(exceptionHandler) {
            val streams = mutableListOf<InputStream>()
            val entries = mutableListOf<SatEntry>()
            val jobs = urls.associateWith { url -> async { remoteSource.getDataStream(url) } }
            jobs.mapValues { job -> job.value.await() }.forEach { result ->
                result.value?.let { stream ->
                    when {
                        result.key.contains("=csv", true) -> {
                            val orbitalData = dataParser.parseCSVStream(stream)
                            entries.addAll(orbitalData.map { data -> SatEntry(data) })
                        }
                        result.key.contains(".zip", true) -> {
                            streams.add(ZipInputStream(stream).apply { nextEntry })
                        }
                        else -> streams.add(stream)
                    }
                }
            }
            streams.forEach { stream -> entries.addAll(importSatellites(stream)) }
            entrySource.insertEntries(entries)
        }
        repositoryScope.launch(exceptionHandler) {
            remoteSource.getDataStream(remoteSource.radioApi)?.let { stream ->
                radioSource.insertRadios(dataParser.parseJSONStream(stream))
                _updateState.value = DataState.Success(0L)
            }
        }
    }

    override fun setUpdateStateHandled() {
        _updateState.value = DataState.Handled
    }

    override fun clearAllData() {
        repositoryScope.launch {
            _updateState.value = DataState.Loading
            delay(updateStateDelay)
            entrySource.deleteEntries()
            radioSource.deleteRadios()
            _updateState.value = DataState.Success(0L)
        }
    }

    private suspend fun importSatellites(stream: InputStream): List<SatEntry> {
        return dataParser.parseTLEStream(stream).map { data -> SatEntry(data) }
    }
}
