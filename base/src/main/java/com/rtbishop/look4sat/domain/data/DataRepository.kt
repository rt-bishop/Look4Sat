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
import com.rtbishop.look4sat.domain.ISettingsManager
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
    private val repositoryScope: CoroutineScope,
    private val settingsManager: ISettingsManager
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
            setUpdateSuccessful()
        }
    }

    override fun updateFromWeb() {
        _updateState.value = DataState.Loading
        repositoryScope.launch(exceptionHandler) {
            val importedEntries = mutableListOf<SatEntry>()
            val sourcesMap = settingsManager.sourcesMap
            val jobsMap = sourcesMap.mapValues { async { remoteSource.getDataStream(it.value) } }
            jobsMap.mapValues { job -> job.value.await() }.forEach { entry ->
                entry.value?.let { stream ->
                    when (val type = entry.key) {
                        "Amsat", "R4UAB" -> {
                            // parse tle stream
                            val satellites = importSatellites(stream)
                            val catnums = satellites.map { it.data.catnum }
                            settingsManager.saveSatType(type, catnums)
                            importedEntries.addAll(satellites)
                        }
                        "McCants", "Classified" -> {
                            // unzip and parse tle stream
                            val unzipped = ZipInputStream(stream).apply { nextEntry }
                            val satellites = importSatellites(unzipped)
                            val catnums = satellites.map { it.data.catnum }
                            settingsManager.saveSatType(type, catnums)
                            importedEntries.addAll(satellites)
                        }
                        else -> {
                            // parse csv stream
                            val parsed = dataParser.parseCSVStream(stream)
                            val satellites = parsed.map { data -> SatEntry(data) }
                            val catnums = satellites.map { it.data.catnum }
                            settingsManager.saveSatType(type, catnums)
                            importedEntries.addAll(satellites)
                        }
                    }
                }
            }
            entrySource.insertEntries(importedEntries)
            setUpdateSuccessful()
        }
        repositoryScope.launch(exceptionHandler) {
            remoteSource.getDataStream(remoteSource.radioApi)?.let { stream ->
                radioSource.insertRadios(dataParser.parseJSONStream(stream))
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
            setUpdateSuccessful(0L)
        }
    }

    private fun setUpdateSuccessful(updateTime: Long = System.currentTimeMillis()) {
        settingsManager.setLastUpdateTime(updateTime)
        _updateState.value = DataState.Success(updateTime)
    }

    private suspend fun importSatellites(stream: InputStream): List<SatEntry> {
        return dataParser.parseTLEStream(stream).map { data -> SatEntry(data) }
    }
}
