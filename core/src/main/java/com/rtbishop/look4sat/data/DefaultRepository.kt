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
import com.rtbishop.look4sat.domain.DataRepository
import com.rtbishop.look4sat.domain.model.SatEntry
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.InputStream
import java.util.zip.ZipInputStream
import kotlin.system.measureTimeMillis

class DefaultRepository(
    private val dataParser: DataParser,
    private val localSource: LocalDataSource,
    private val remoteSource: RemoteDataSource
) : DataRepository {

    override val defaultSelection = listOf(43700, 25544, 25338, 28654, 33591, 40069, 27607, 24278)
    override val defaultSources = listOf(
        "https://celestrak.com/NORAD/elements/gp.php?GROUP=active&FORMAT=csv",
        "https://amsat.org/tle/current/nasabare.txt",
        "https://www.prismnet.com/~mmccants/tles/classfd.zip",
        "https://www.prismnet.com/~mmccants/tles/inttles.zip"
    )
    override val transmittersSource = "https://db.satnogs.org/api/transmitters/?format=json"

    override fun getSatelliteItems() = localSource.getSatelliteItems()

    override suspend fun getSelectedSatellites() = localSource.getSelectedSatellites()

    override suspend fun getTransmitters(catnum: Int) = localSource.getTransmitters(catnum)

    override suspend fun getWebSources() = localSource.getSources().also { sources ->
        return if (sources.isNotEmpty()) sources
        else defaultSources
    }

    override suspend fun updateDataFromFile(stream: InputStream) {
        localSource.updateEntries(importSatellites(stream))
    }

    override suspend fun updateDataFromWeb(sources: List<String>) {
        coroutineScope {
            launch {
                localSource.updateSources(sources)
            }
            launch {
                val updateTimeMillis = measureTimeMillis {
                    val fetchesMap = mutableMapOf<String, Deferred<InputStream>>()
                    val streamsMap = mutableMapOf<String, InputStream>()
                    val streams = mutableListOf<InputStream>()
                    val entries = mutableListOf<SatEntry>()
                    sources.forEach { fetchesMap[it] = async { remoteSource.fetchFileStream(it) } }
                    fetchesMap.forEach { streamsMap[it.key] = it.value.await() }
                    streamsMap.forEach { stream ->
                        when {
                            stream.key.contains("=csv", true) -> {
                                val tles = dataParser.parseCSVStream(stream.value)
                                entries.addAll(tles.map { tle -> SatEntry(tle) })
                            }
                            stream.key.contains(".zip", true) -> {
                                streams.add(ZipInputStream(stream.value).apply { nextEntry })
                            }
                            else -> streams.add(stream.value)
                        }
                    }
                    streams.forEach { stream -> entries.addAll(importSatellites(stream)) }
                    localSource.updateEntries(entries)
                }
                println("Update from web took $updateTimeMillis ms")
            }
            launch {
                val jsonStream = remoteSource.fetchFileStream(transmittersSource)
                val transmitters = dataParser.parseJSONStream(jsonStream)
                localSource.updateTransmitters(transmitters)
            }
        }
    }

    override suspend fun updateSelection(catnums: List<Int>, isSelected: Boolean) {
        localSource.updateEntriesSelection(catnums, isSelected)
    }

    private suspend fun importSatellites(stream: InputStream): List<SatEntry> {
        return dataParser.parseTLEStream(stream).map { tle -> SatEntry(tle) }
    }
}
