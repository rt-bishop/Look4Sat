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

import com.rtbishop.look4sat.domain.SatelliteRepo
import com.rtbishop.look4sat.domain.model.SatEntry
import com.rtbishop.look4sat.domain.model.SatItem
import com.rtbishop.look4sat.domain.model.Transmitter
import com.rtbishop.look4sat.domain.predict.Satellite
import com.rtbishop.look4sat.domain.predict.TLE
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.zip.ZipInputStream

class DataRepository(
    private val localSource: LocalDataSource,
    private val remoteSource: RemoteDataSource,
    private val repoDispatcher: CoroutineDispatcher
) : SatelliteRepo {

    override fun getSatItems(): Flow<List<SatItem>> {
        return localSource.getEntriesWithModes()
    }

    override fun getSatTransmitters(catNum: Int): Flow<List<Transmitter>> {
        return localSource.getTransmitters(catNum)
    }

    override suspend fun getSelectedSatellites(): List<Satellite> {
        return localSource.getSelectedSatellites()
    }

    override suspend fun updateEntriesFromFile(stream: InputStream) = withContext(repoDispatcher) {
        localSource.updateEntries(importSatEntries(stream))
    }

    override suspend fun updateEntriesFromWeb(sources: List<String>) {
        coroutineScope {
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
                streams.forEach { stream -> entries.addAll(importSatEntries(stream)) }
                localSource.updateEntries(entries)
            }
            launch(repoDispatcher) {
                val transmitters = remoteSource.fetchTransmitters().filter { it.isAlive }
                localSource.updateTransmitters(transmitters)
            }
        }
    }

    override suspend fun updateEntriesSelection(catNums: List<Int>, isSelected: Boolean) {
        localSource.updateEntriesSelection(catNums, isSelected)
    }

    private fun importSatEntries(stream: InputStream): List<SatEntry> {
        return TLE.parseTleStream(stream).map { tle -> SatEntry(tle) }
    }
}
