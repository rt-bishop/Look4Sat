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


    override suspend fun updateTLEFromFile(uri: String): Int = withContext(dispatcher) {
        var importedCount = 0
        remoteSource.getFileStream(uri)?.let { stream ->
            val entries = parseSatelliteStream(uri, unwrapIfZipped(uri, stream))
            localSource.insertEntries(entries)
            importedCount = entries.size
        }
        setUpdateSuccessful(System.currentTimeMillis())
        importedCount
    }

    override suspend fun updateTransceiversFromFile(uri: String): Int = withContext(dispatcher) {
        var importedCount = 0
        remoteSource.getFileStream(uri)?.let { stream ->
            val transceivers = dataParser.parseJSONStream(unwrapIfZipped(uri, stream))
            localSource.insertRadios(transceivers)
            importedCount = transceivers.size
        }
        setUpdateSuccessful(System.currentTimeMillis())
        importedCount
    }

    override suspend fun updateFromRemote() = withContext(dispatcher) {
        val settings = settingsRepo.dataSourcesSettings.value
        fun normalizeUrl(url: String) = if (url.startsWith("http")) url else "https://$url"
        val tleUrls = settings.satelliteUrls.filterIndexed { i, url -> url.isNotBlank() && settings.isSatelliteEnabled(i) }
        val radioUrls = settings.transceiversUrls.filterIndexed { i, url -> url.isNotBlank() && settings.isTransceiverEnabled(i) }
        // launch all network requests concurrently, keeping the raw url as key for status reporting
        val tleJobs = tleUrls.map { url -> async { url to remoteSource.getNetworkStream(normalizeUrl(url)) } }
        val radioJobs = radioUrls.map { url -> async { url to remoteSource.getNetworkStream(normalizeUrl(url)) } }
        val tleResults = tleJobs.awaitAll()
        val radioResults = radioJobs.awaitAll()
        // report the HTTP status code of every source (200, 404, ...)
        settingsRepo.updateDataSourcesStatus(
            (tleResults + radioResults).associate { (url, result) -> url to result.code }
        )
        // parse fetched data concurrently, keeping the first occurrence per primary key
        // so sources listed higher in the dialog take priority over lower ones
        val importedEntries = tleResults.flatMap { (url, result) ->
            result.stream?.let { val nUrl = normalizeUrl(url); parseSatelliteStream(nUrl, unwrapIfZipped(nUrl, it)) }.orEmpty()
        }.distinctBy { it.catnum }
        val importedRadios = radioResults.flatMap { (url, result) ->
            result.stream?.let { val nUrl = normalizeUrl(url); dataParser.parseJSONStream(unwrapIfZipped(nUrl, it)) }.orEmpty()
        }.filter { it.uuid.isNotBlank() }.distinctBy { it.uuid }
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

    private suspend fun parseSatelliteStream(url: String, stream: InputStream): List<OrbitalData> {
        val bufferedStream = stream.buffered()
        return when {
            hasCsvHint(url) || looksLikeCsv(bufferedStream) -> dataParser.parseCSVStream(bufferedStream)
            else -> dataParser.parseTLEStream(bufferedStream)
        }
    }

    private fun hasCsvHint(url: String): Boolean {
        return url.contains("FORMAT=csv", ignoreCase = true) ||
            url.endsWith(".csv", ignoreCase = true) ||
            url.endsWith(".csv.zip", ignoreCase = true)
    }

    private fun looksLikeCsv(stream: InputStream): Boolean {
        if (!stream.markSupported()) return false
        stream.mark(4096)
        val preview = ByteArray(4096)
        val length = stream.read(preview)
        stream.reset()
        if (length <= 0) return false
        val line = preview.decodeToString(0, length).lineSequence().firstOrNull()?.trim().orEmpty()
        return line.contains("OBJECT_NAME", ignoreCase = true) ||
            line.contains("NORAD_CAT_ID", ignoreCase = true) ||
            line.count { it == ',' } >= 4
    }

    private suspend fun setUpdateSuccessful(timestamp: Long) {
        settingsRepo.updateDatabaseState(
            DatabaseState(localSource.getRadiosTotal(), localSource.getEntriesTotal(), timestamp)
        )
    }

    private fun unwrapIfZipped(url: String, stream: InputStream): InputStream =
        if (url.endsWith(".zip", ignoreCase = true)) ZipInputStream(stream).apply { nextEntry } else stream
}
