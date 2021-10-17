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

import com.rtbishop.look4sat.domain.DataRepository
import com.rtbishop.look4sat.domain.model.SatEntry
import com.rtbishop.look4sat.domain.predict.TLE
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.zip.ZipInputStream

class DefaultRepository(
    private val localSource: LocalDataSource,
    private val remoteSource: RemoteDataSource,
    private val repoDispatcher: CoroutineDispatcher
) : DataRepository {

    override val defaultSelection = listOf(43700, 25544, 25338, 28654, 33591, 40069, 27607, 24278)
    override val defaultSources = listOf(
        "https://celestrak.com/NORAD/elements/active.txt",
        "https://amsat.org/tle/current/nasabare.txt",
        "https://www.prismnet.com/~mmccants/tles/classfd.zip",
        "https://www.prismnet.com/~mmccants/tles/inttles.zip"
    )

    override fun getSatelliteItems() = localSource.getSatelliteItems()

    override suspend fun getSelectedSatellites() = localSource.getSelectedSatellites()

    override suspend fun getTransmitters(catnum: Int) = localSource.getTransmitters(catnum)

    override suspend fun getWebSources() = localSource.getWebSources().also { sources ->
        return if (sources.isNotEmpty()) sources
        else defaultSources
    }

    override suspend fun updateDataFromFile(stream: InputStream) = withContext(repoDispatcher) {
        localSource.updateEntries(importSatellites(stream))
    }

    override suspend fun updateDataFromWeb(sources: List<String>) {
        coroutineScope {
            launch(repoDispatcher) {
                localSource.updateWebSources(sources)
            }
            launch(repoDispatcher) {
                val streams = mutableListOf<InputStream>()
                val entries = mutableListOf<SatEntry>()
                sources.forEach { source ->
                    remoteSource.fetchFileStream(source)?.let { stream ->
                        if (source.contains(".zip", true)) {
                            val zipStream = ZipInputStream(stream).apply { nextEntry }
                            streams.add(zipStream)
                        } else {
                            streams.add(stream)
                        }
                    }
                }
                streams.forEach { stream -> entries.addAll(importSatellites(stream)) }
                localSource.updateEntries(entries)
            }
            launch(repoDispatcher) {
                val transmitters = remoteSource.fetchTransmitters().filter { it.isAlive }
                localSource.updateTransmitters(transmitters)
            }
        }
    }

    override suspend fun updateSelection(catnums: List<Int>, isSelected: Boolean) {
        localSource.updateSelection(catnums, isSelected)
    }

    private fun importSatellites(stream: InputStream): List<SatEntry> {
        return TLE.parseTleStream(stream).map { tle -> SatEntry(tle) }
    }
}
