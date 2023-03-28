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

import com.rtbishop.look4sat.domain.IDatabaseRepo
import com.rtbishop.look4sat.domain.ISettingsRepo
import com.rtbishop.look4sat.model.DataState
import com.rtbishop.look4sat.model.SatEntry
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import java.util.zip.ZipInputStream

class DatabaseRepo(
    private val dataParser: DataParser,
    private val localStorage: ILocalStorage,
    private val dataSource: IDataSource,
    private val repositoryScope: CoroutineScope,
    private val settingsRepository: ISettingsRepo
) : IDatabaseRepo {

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        _updateState.value = DataState.Exception(exception.message)
    }
    private val updateStateDelay = 875L
    private val _updateState = MutableStateFlow<DataState<Long>>(DataState.Handled)
    override val updateState: StateFlow<DataState<Long>> = _updateState

    override fun getEntriesTotal() = localStorage.getEntriesTotal()

    override fun getRadiosTotal() = localStorage.getRadiosTotal()

    override fun updateFromFile(uri: String) {
        repositoryScope.launch(exceptionHandler) {
            _updateState.value = DataState.Loading
            dataSource.getFileStream(uri)?.let { stream ->
                delay(updateStateDelay)
                localStorage.insertEntries(importSatellites(stream))
            }
            setUpdateSuccessful()
        }
    }

    override fun updateFromWeb() {
        _updateState.value = DataState.Loading
        repositoryScope.launch(exceptionHandler) {
            val importedEntries = mutableListOf<SatEntry>()
            val sourcesMap = settingsRepository.satelliteSourcesMap
            val jobsMap = sourcesMap.mapValues { async { dataSource.getNetworkStream(it.value) } }
            jobsMap.mapValues { job -> job.value.await() }.forEach { entry ->
                entry.value?.let { stream ->
                    when (val type = entry.key) {
                        "AMSAT", "R4UAB" -> {
                            // parse tle stream
                            val satellites = importSatellites(stream)
                            val catnums = satellites.map { it.data.catnum }
                            settingsRepository.saveSatType(type, catnums)
                            importedEntries.addAll(satellites)
                        }

                        "McCants", "Classified" -> {
                            // unzip and parse tle stream
                            val unzipped = ZipInputStream(stream).apply { nextEntry }
                            val satellites = importSatellites(unzipped)
                            val catnums = satellites.map { it.data.catnum }
                            settingsRepository.saveSatType(type, catnums)
                            importedEntries.addAll(satellites)
                        }

                        else -> {
                            // parse csv stream
                            val parsed = dataParser.parseCSVStream(stream)
                            val satellites = parsed.map { data -> SatEntry(data) }
                            val catnums = satellites.map { it.data.catnum }
                            settingsRepository.saveSatType(type, catnums)
                            importedEntries.addAll(satellites)
                        }
                    }
                }
            }
            localStorage.insertEntries(importedEntries)
            setUpdateSuccessful()
        }
        repositoryScope.launch(exceptionHandler) {
            dataSource.getNetworkStream(settingsRepository.radioSourceUrl)?.let { stream ->
                localStorage.insertRadios(dataParser.parseJSONStream(stream))
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
            localStorage.deleteEntries()
            localStorage.deleteRadios()
            setUpdateSuccessful(0L)
        }
    }

    private fun setUpdateSuccessful(updateTime: Long = System.currentTimeMillis()) {
        settingsRepository.setLastUpdateTime(updateTime)
        _updateState.value = DataState.Success(updateTime)
    }

    private suspend fun importSatellites(stream: InputStream): List<SatEntry> {
        return dataParser.parseTLEStream(stream).map { data -> SatEntry(data) }
    }
}
