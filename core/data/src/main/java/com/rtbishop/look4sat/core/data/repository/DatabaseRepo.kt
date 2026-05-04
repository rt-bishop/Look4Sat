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

    private val customSourceType = "Other"

    override suspend fun updateTLEFromFile(uri: String) = withContext(dispatcher) {
        remoteSource.getFileStream(uri)?.let { stream ->
            val entries = dataParser.parseTLEStream(unwrapIfZipped(uri, stream))
            localSource.insertEntries(entries)
            settingsRepo.setSatelliteTypeIds(customSourceType, entries.map { it.catnum })
        }
        setUpdateSuccessful(System.currentTimeMillis())
    }

    override suspend fun updateTransceiversFromFile(uri: String) = withContext(dispatcher) {
        remoteSource.getFileStream(uri)?.let { stream ->
            val transceivers = dataParser.parseJSONStream(unwrapIfZipped(uri, stream))
            localSource.insertRadios(transceivers)
        }
        setUpdateSuccessful(System.currentTimeMillis())
    }

    override suspend fun updateFromRemote() = withContext(dispatcher) {
        val dataSourcesSettings = settingsRepo.dataSourcesSettings.value
        val tleUrls = buildMap {
            putAll(Sources.satelliteDataUrls)
            if (dataSourcesSettings.useCustomTLE) put(customSourceType, dataSourcesSettings.tleUrl)
        }.filterValues { it.isNotBlank() }
        val radioUrls = buildMap {
            putAll(Sources.transceiversDataUrls)
            if (dataSourcesSettings.useCustomTransceivers) put(customSourceType, dataSourcesSettings.transceiversUrl)
        }.filterValues { it.isNotBlank() }
        // launch all network requests concurrently
        val tleJobs = tleUrls.values.map { url -> async { url to remoteSource.getNetworkStream(url) } }
        val radioJobs = radioUrls.values.map { url -> async { url to remoteSource.getNetworkStream(url) } }
        // parse fetched data concurrently and associate with types
        val importedEntries = tleJobs.awaitAll().flatMap { (url, stream) ->
            val type = tleUrls.entries.find { it.value == url }?.key ?: customSourceType
            stream?.let { parseSatelliteStream(url, unwrapIfZipped(url, it)) }.orEmpty().also { entries ->
                settingsRepo.setSatelliteTypeIds(type, entries.map { it.catnum })
            }
        }
        val importedRadios = radioJobs.awaitAll().flatMap { (url, stream) ->
            stream?.let { dataParser.parseJSONStream(unwrapIfZipped(url, it)) }.orEmpty()
        }
        // insert parsed data into the database
        localSource.insertEntries(importedEntries)
        localSource.insertRadios(importedRadios)
        setUpdateSuccessful(System.currentTimeMillis())
    }

    override suspend fun clearAllData() = withContext(dispatcher) {
        localSource.deleteEntries()
        localSource.deleteRadios()
        setUpdateSuccessful(0L)
    }

    private suspend fun parseSatelliteStream(url: String, stream: InputStream): List<OrbitalData> = when {
        url.contains("FORMAT=csv", ignoreCase = true) -> dataParser.parseCSVStream(stream)
        else -> dataParser.parseTLEStream(stream)
    }

    private suspend fun setUpdateSuccessful(timestamp: Long) {
        settingsRepo.updateDatabaseState(
            DatabaseState(localSource.getRadiosTotal(), localSource.getEntriesTotal(), timestamp)
        )
    }

    private fun unwrapIfZipped(url: String, stream: InputStream): InputStream =
        if (url.endsWith(".zip", ignoreCase = true)) ZipInputStream(stream).apply { nextEntry } else stream
}
