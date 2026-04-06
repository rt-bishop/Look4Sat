/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2026 Arty Bishop and contributors.
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
package com.rtbishop.look4sat.core.data.repository

import com.rtbishop.look4sat.core.domain.model.DatabaseState
import com.rtbishop.look4sat.core.domain.predict.OrbitalData
import com.rtbishop.look4sat.core.domain.repository.IDatabaseRepo
import com.rtbishop.look4sat.core.domain.repository.ISettingsRepo
import com.rtbishop.look4sat.core.domain.source.ILocalSource
import com.rtbishop.look4sat.core.domain.source.IRemoteSource
import com.rtbishop.look4sat.core.domain.source.Sources
import com.rtbishop.look4sat.core.domain.utility.DataParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.zip.ZipInputStream

class DatabaseRepo(
    private val dispatcher: CoroutineDispatcher,
    private val dataParser: DataParser,
    private val localSource: ILocalSource,
    private val remoteSource: IRemoteSource,
    private val settingsRepo: ISettingsRepo
) : IDatabaseRepo {

    private companion object {
        val tleTypes = setOf("Amsat", "R4UAB", "Other")
        val zippedTleTypes = setOf("McCants", "Classified")
    }

    override suspend fun updateTLEFromFile(uri: String) = withContext(dispatcher) {
        remoteSource.getFileStream(uri)?.let { stream ->
            val entries = dataParser.parseTLEStream(stream)
            localSource.insertEntries(entries)
            settingsRepo.setSatelliteTypeIds("Other", entries.map { it.catnum })
        }
        setUpdateSuccessful(System.currentTimeMillis())
    }

    override suspend fun updateTransceiversFromFile(uri: String) = withContext(dispatcher) {
        remoteSource.getFileStream(uri)
            ?.let { dataParser.parseJSONStream(it) }
            ?.takeIf { it.isNotEmpty() }
            ?.let {
                localSource.deleteRadios()
                localSource.insertRadios(it)
            }
        setUpdateSuccessful(System.currentTimeMillis())
    }

    override suspend fun updateFromRemote() = withContext(dispatcher) {
        val dataSourcesSettings = settingsRepo.dataSourcesSettings.value
        val tleUrls = buildMap {
            putAll(Sources.satelliteDataUrls)
            if (dataSourcesSettings.useCustomTLE) put("Other", dataSourcesSettings.tleUrl)
        }.filterValues { it.isNotEmpty() }

        val radioUrls = buildList {
            add(Sources.RADIO_DATA_URL)
            if (dataSourcesSettings.useCustomTransceivers) add(dataSourcesSettings.transceiversUrl)
        }

        // launch all network requests concurrently
        val tleJobs = tleUrls.map { (type, url) -> async { type to remoteSource.getNetworkStream(url) } }
        val radioJobs = radioUrls.map { url -> async { remoteSource.getNetworkStream(url) } }

        // parse satellite data
        val importedEntries = tleJobs.awaitAll().flatMap { (type, stream) ->
            stream?.let { parseSatelliteStream(type, it) }.orEmpty().also { satellites ->
                settingsRepo.setSatelliteTypeIds(type, satellites.map { it.catnum })
            }
        }

        // parse radio data
        val importedRadios = radioJobs.awaitAll().filterNotNull().flatMap { dataParser.parseJSONStream(it) }

        localSource.insertEntries(importedEntries)
        localSource.insertRadios(importedRadios)
        setUpdateSuccessful(System.currentTimeMillis())
    }

    override suspend fun clearAllData() = withContext(dispatcher) {
        localSource.deleteEntries()
        localSource.deleteRadios()
        setUpdateSuccessful(0L)
    }

    private suspend fun parseSatelliteStream(type: String, stream: InputStream): List<OrbitalData> = when (type) {
        in tleTypes -> dataParser.parseTLEStream(stream)
        in zippedTleTypes -> dataParser.parseTLEStream(ZipInputStream(stream).apply { nextEntry })
        else -> dataParser.parseCSVStream(stream)
    }

    private suspend fun setUpdateSuccessful(timestamp: Long) {
        settingsRepo.updateDatabaseState(
            DatabaseState(localSource.getRadiosTotal(), localSource.getEntriesTotal(), timestamp)
        )
    }
}
