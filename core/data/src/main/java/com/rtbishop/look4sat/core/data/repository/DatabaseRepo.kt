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
            val entries = mergeEntries(listOf(parseSatelliteStream(uri, unwrapIfZipped(uri, stream))))
            insertFresherEntries(entries)
            // report what the file contained: a valid file holding only stale data is not an error
            importedCount = entries.size
        }
        setUpdateSuccessful(System.currentTimeMillis())
        importedCount
    }

    override suspend fun updateTransceiversFromFile(uri: String): Int = withContext(dispatcher) {
        var importedCount = 0
        remoteSource.getFileStream(uri)?.let { stream ->
            val transceivers = dataParser.parseJSONStream(unwrapIfZipped(uri, stream))
            localSource.insertRadios(transceivers, isCustom = true)
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
        // parse fetched data concurrently, merging satellites by freshness instead of source order
        val parsedPerSource = tleResults.map { (url, result) ->
            result.stream?.let { val nUrl = normalizeUrl(url); parseSatelliteStream(nUrl, unwrapIfZipped(nUrl, it)) }.orEmpty()
        }
        val importedRadios = radioResults.flatMap { (url, result) ->
            result.stream?.let { val nUrl = normalizeUrl(url); dataParser.parseJSONStream(unwrapIfZipped(nUrl, it)) }.orEmpty()
        }.filter { it.uuid.isNotBlank() }.distinctBy { it.uuid }
        // insert parsed data into the database
        insertFresherEntries(mergeEntries(parsedPerSource))
        // transceivers are a full snapshot: sources publish active entries only, so a retired
        // transceiver simply disappears from the feed and has to be dropped locally as well.
        // Imported ones are kept: no source can refresh them, so nothing would bring them back
        if (importedRadios.isNotEmpty()) {
            localSource.deleteManagedRadios()
            localSource.insertRadios(importedRadios, isCustom = false)
        }
        // keep the previous timestamp when every source failed, so the next launch retries
        val hasFetchedData = parsedPerSource.any { entries -> entries.isNotEmpty() } || importedRadios.isNotEmpty()
        val previousTimestamp = settingsRepo.databaseState.value.updateTimestamp
        if (hasFetchedData) pruneStaleEntries()
        setUpdateSuccessful(if (hasFetchedData) System.currentTimeMillis() else previousTimestamp)
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

    /**
     * Merges the data of every source: orbital elements always come from the set with the newest
     * epoch, while the name comes from the first source that provides it. Source order is thus a
     * naming preference only, which also keeps names stable when sources leapfrog each other.
     */
    private fun mergeEntries(sourceEntries: List<List<OrbitalData>>): List<OrbitalData> {
        val preferredNames = mutableMapOf<Int, String>()
        val freshestEntries = mutableMapOf<Int, OrbitalData>()
        sourceEntries.forEach { entries ->
            entries.forEach { entry ->
                val name = entry.name.trim()
                if (name.isNotBlank()) preferredNames.getOrPut(entry.catnum) { name }
                val current = freshestEntries[entry.catnum]
                if (current == null || entry.epochDaynum > current.epochDaynum) {
                    freshestEntries[entry.catnum] = entry
                }
            }
        }
        return freshestEntries.values.map { entry ->
            val name = preferredNames[entry.catnum]
            if (name == null || name == entry.name) entry else entry.copy(name = name)
        }
    }

    /** Stores the entries that are newer than the ones already saved, renaming the rest in place. */
    private suspend fun insertFresherEntries(entries: List<OrbitalData>) {
        val storedEpochs = localSource.getEntriesEpochs()
        val (fresherEntries, staleEntries) = entries.partition { entry ->
            val storedEpoch = storedEpochs[entry.catnum]
            storedEpoch == null || entry.epochDaynum > OrbitalData.epochToDaynum(storedEpoch)
        }
        localSource.insertEntries(fresherEntries)
        // a reordered source list has to rename satellites right away, even the ones holding
        // elements that are newer than the ones just parsed
        if (staleEntries.isNotEmpty()) {
            val storedNames = localSource.getEntriesNames()
            val renamedEntries = staleEntries.filter { entry -> entry.name != storedNames[entry.catnum] }
            if (renamedEntries.isNotEmpty()) {
                localSource.renameEntries(renamedEntries.associate { it.catnum to it.name })
            }
        }
    }

    /**
     * Drops satellites that no enabled source has refreshed for a month: they either decayed or
     * disappeared from every catalog. Manually imported data ages out the same way, and every
     * source republishes active satellites well within that window.
     */
    private suspend fun pruneStaleEntries() {
        val currentDaynum = OrbitalData.timeToDaynum(System.currentTimeMillis())
        val staleIds = localSource.getEntriesEpochs()
            .filterValues { epoch -> currentDaynum - OrbitalData.epochToDaynum(epoch) > 30.0 }
            .keys.toList()
        if (staleIds.isNotEmpty()) localSource.deleteEntriesWithIds(staleIds)
    }

    private suspend fun setUpdateSuccessful(timestamp: Long) {
        settingsRepo.updateDatabaseState(
            DatabaseState(localSource.getRadiosTotal(), localSource.getEntriesTotal(), timestamp)
        )
    }

    private fun unwrapIfZipped(url: String, stream: InputStream): InputStream =
        if (url.endsWith(".zip", ignoreCase = true)) ZipInputStream(stream).apply { nextEntry } else stream
}
