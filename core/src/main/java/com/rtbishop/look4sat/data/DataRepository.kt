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
import com.rtbishop.look4sat.domain.model.SatItem
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.InputStream
import java.util.zip.ZipInputStream
import kotlin.system.measureTimeMillis

class DataRepository(
    private val dataParser: DataParser,
    private val settings: ISettingsHandler,
    private val localSource: ILocalSource,
    private val remoteSource: IRemoteSource,
    private val repositoryScope: CoroutineScope
) : IDataRepository {

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        println("DataRepository: handled $exception")
        _updateState.value = DataState.Error(exception.message)
    }
    private val _updateState = MutableStateFlow<DataState<Long>>(DataState.Handled)
    override val dataUpdateState: StateFlow<DataState<Long>> = _updateState

    override fun setDataUpdateHandled() {
        _updateState.value = DataState.Handled
    }

    override fun updateDataFromFile(stream: InputStream) {
        repositoryScope.launch(exceptionHandler) {
            _updateState.value = DataState.Loading
            val importedSatellites = importSatellites(stream)
            localSource.updateEntries(importedSatellites)
            _updateState.value = DataState.Success(0L)
        }
    }

    override fun updateDataFromWeb(sources: List<String>) {
        _updateState.value = DataState.Loading
        repositoryScope.launch(exceptionHandler) {
            settings.saveDataSources(sources)
        }
        repositoryScope.launch(exceptionHandler) {
            val updateTimeMillis = measureTimeMillis {
                val jobsMap = mutableMapOf<String, Deferred<InputStream?>>()
                val streamsMap = mutableMapOf<String, InputStream?>()
                val streams = mutableListOf<InputStream>()
                val entries = mutableListOf<SatEntry>()
                sources.forEach { jobsMap[it] = async { remoteSource.fetchFileStream(it) } }
                jobsMap.forEach { streamsMap[it.key] = it.value.await() }
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
                localSource.updateEntries(entries)
            }
            println("Update from web took $updateTimeMillis ms")
            _updateState.value = DataState.Success(updateTimeMillis)
        }
        repositoryScope.launch(exceptionHandler) {
            remoteSource.fetchFileStream(settings.transmittersSource)?.let { stream ->
                val transmitters = dataParser.parseJSONStream(stream)
                localSource.updateTransmitters(transmitters)
            }
        }
    }

    override fun getSatelliteItems(): Flow<List<SatItem>> = localSource.getSatelliteItems()

    override suspend fun getSelectedSatellites() = localSource.getSelectedSatellites()

    override suspend fun getTransmitters(catnum: Int) = localSource.getTransmitters(catnum)

    override suspend fun updateSelection(catnums: List<Int>, isSelected: Boolean) {
        localSource.updateEntriesSelection(catnums, isSelected)
    }

    private suspend fun importSatellites(stream: InputStream): List<SatEntry> {
        return dataParser.parseTLEStream(stream).map { tle -> SatEntry(tle) }
    }
}
