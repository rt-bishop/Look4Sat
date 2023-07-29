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
package com.rtbishop.look4sat.data.repository

import com.rtbishop.look4sat.domain.model.DatabaseState
import com.rtbishop.look4sat.domain.model.SatEntry
import com.rtbishop.look4sat.domain.model.SatRadio
import com.rtbishop.look4sat.domain.repository.IDatabaseRepo
import com.rtbishop.look4sat.domain.repository.ISettingsRepo
import com.rtbishop.look4sat.domain.source.IDataSource
import com.rtbishop.look4sat.domain.source.ILocalStorage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.zip.ZipInputStream

class DatabaseRepo(
    private val dispatcher: CoroutineDispatcher,
    private val dataParser: DataParser,
    private val dataSource: IDataSource,
    private val localStorage: ILocalStorage,
    private val settingsRepository: ISettingsRepo
) : IDatabaseRepo {

    override suspend fun updateFromFile(uri: String) = withContext(dispatcher) {
        val importedSatellites = dataSource.getFileStream(uri)?.let { importSatellites(it) }
        importedSatellites?.let { localStorage.insertEntries(it) }
        setUpdateSuccessful(System.currentTimeMillis())
    }

    override suspend fun updateFromWeb() = withContext(dispatcher) {
        val sourcesMap = settingsRepository.satelliteSourcesMap
        val importedEntries = mutableListOf<SatEntry>()
        val importedRadios = mutableListOf<SatRadio>()
        // fetch
        val jobsMap = sourcesMap.mapValues { async { dataSource.getNetworkStream(it.value) } }
        val jobRadios = async { dataSource.getNetworkStream(settingsRepository.radioSourceUrl) }
        // parse
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
        jobRadios.await()?.let { importedRadios.addAll(dataParser.parseJSONStream(it)) }
        // insert
        localStorage.insertEntries(importedEntries)
        localStorage.insertRadios(importedRadios)
        setUpdateSuccessful(System.currentTimeMillis())
    }

    override suspend fun clearAllData() = withContext(dispatcher) {
        localStorage.deleteEntries()
        localStorage.deleteRadios()
        setUpdateSuccessful(0L)
    }

    private suspend fun setUpdateSuccessful(timestamp: Long) = withContext(dispatcher) {
        val satellitesTotal = localStorage.getEntriesTotal()
        val radiosTotal = localStorage.getRadiosTotal()
        settingsRepository.saveDatabaseState(DatabaseState(satellitesTotal, radiosTotal, timestamp))
    }

    private suspend fun importSatellites(stream: InputStream): List<SatEntry> {
        return dataParser.parseTLEStream(stream).map { data -> SatEntry(data) }
    }
}
