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
import com.rtbishop.look4sat.domain.model.SatRadio
import com.rtbishop.look4sat.domain.predict.OrbitalData
import com.rtbishop.look4sat.domain.repository.IDatabaseRepo
import com.rtbishop.look4sat.domain.repository.ISettingsRepo
import com.rtbishop.look4sat.domain.source.ILocalSource
import com.rtbishop.look4sat.domain.source.IRemoteSource
import com.rtbishop.look4sat.domain.source.Sources
import com.rtbishop.look4sat.domain.utility.DataParser
import java.util.zip.ZipInputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

class DatabaseRepo(
    private val dispatcher: CoroutineDispatcher,
    private val dataParser: DataParser,
    private val localSource: ILocalSource,
    private val remoteSource: IRemoteSource,
    private val settingsRepo: ISettingsRepo
) : IDatabaseRepo {

    override suspend fun updateFromFile(uri: String) = withContext(dispatcher) {
        val importedSatellites = remoteSource.getFileStream(uri)?.let { dataParser.parseTLEStream(it) }
        importedSatellites?.let { localSource.insertEntries(it) }
        setUpdateSuccessful(System.currentTimeMillis())
    }

    override suspend fun updateFromRemote() = withContext(dispatcher) {
        val importedEntries = mutableListOf<OrbitalData>()
        val importedRadios = mutableListOf<SatRadio>()
        // fetch
        val jobsMap = Sources.satelliteDataUrls.mapValues { async { remoteSource.getNetworkStream(it.value) } }
        val jobRadios = async { remoteSource.getNetworkStream(Sources.radioDataUrl) }
        // parse
        jobsMap.mapValues { job -> job.value.await() }.forEach { entry ->
            entry.value?.let { stream ->
                when (val type = entry.key) {
                    "Amsat", "R4UAB" -> {
                        // parse tle stream
                        val satellites = dataParser.parseTLEStream(stream)
                        val catnums = satellites.map { it.catnum }
                        settingsRepo.setSatelliteTypeIds(type, catnums)
                        importedEntries.addAll(satellites)
                    }

                    "McCants", "Classified" -> {
                        // unzip and parse tle stream
                        val unzipped = ZipInputStream(stream).apply { nextEntry }
                        val satellites = dataParser.parseTLEStream(unzipped)
                        val catnums = satellites.map { it.catnum }
                        settingsRepo.setSatelliteTypeIds(type, catnums)
                        importedEntries.addAll(satellites)
                    }

                    else -> {
                        // parse csv stream
                        val satellites = dataParser.parseCSVStream(stream)
                        val catnums = satellites.map { it.catnum }
                        settingsRepo.setSatelliteTypeIds(type, catnums)
                        importedEntries.addAll(satellites)
                    }
                }
            }
        }
        jobRadios.await()?.let { importedRadios.addAll(dataParser.parseJSONStream(it)) }
        // insert
        localSource.insertEntries(importedEntries)
        localSource.insertRadios(importedRadios)
        setUpdateSuccessful(System.currentTimeMillis())
    }

    override suspend fun clearAllData() = withContext(dispatcher) {
        localSource.deleteEntries()
        localSource.deleteRadios()
        setUpdateSuccessful(0L)
    }

    private suspend fun setUpdateSuccessful(timestamp: Long) = withContext(dispatcher) {
        val numberOfRadios = localSource.getRadiosTotal()
        val numberOfSatellites = localSource.getEntriesTotal()
        settingsRepo.updateDatabaseState(DatabaseState(numberOfRadios, numberOfSatellites, timestamp))
    }
}
