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

import com.rtbishop.look4sat.domain.Transmitter
import com.rtbishop.look4sat.domain.Satellite
import com.rtbishop.look4sat.domain.TLE
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.zip.ZipInputStream

class SatelliteRepo(
    private val preferencesSource: PreferencesSource,
    private val localDataSource: LocalDataSource,
    private val remoteDataSource: RemoteDataSource,
    private val ioDispatcher: CoroutineDispatcher
) {

    fun saveSelectedModes(modes: List<String>) {
        preferencesSource.saveModesSelection(modes)
    }

    fun loadSelectedModes(): List<String> {
        return preferencesSource.loadModesSelection()
    }

    fun getSatItems(): Flow<List<SatItem>> {
        return localDataSource.getSatItems()
    }

    fun getSatTransmitters(catNum: Int): Flow<List<Transmitter>> {
        return localDataSource.getSatTransmitters(catNum)
    }

    suspend fun getSelectedSatellites(): List<Satellite> {
        return localDataSource.getSelectedSatellites()
    }

    suspend fun updateEntriesFromFile(stream: InputStream) = withContext(ioDispatcher) {
        localDataSource.updateEntries(importSatEntries(stream))
    }

    suspend fun updateEntriesFromWeb(sources: List<String>) {
        coroutineScope {
            launch(ioDispatcher) {
                val streams = mutableListOf<InputStream>()
                val entries = mutableListOf<SatEntry>()
                sources.forEach { source ->
                    remoteDataSource.fetchDataStream(source)?.let { stream ->
                        if (source.contains(".zip", true)) {
                            val zipStream = ZipInputStream(stream).apply { nextEntry }
                            streams.add(zipStream)
                        } else {
                            streams.add(stream)
                        }
                    }
                }
                streams.forEach { stream -> entries.addAll(importSatEntries(stream)) }
                localDataSource.updateEntries(entries)
            }
            launch(ioDispatcher) {
                val transmitters = remoteDataSource.fetchTransmitters().filter { it.isAlive }
                localDataSource.updateTransmitters(transmitters)
            }
        }
    }

    suspend fun updateEntriesSelection(catNums: List<Int>, isSelected: Boolean) {
        localDataSource.updateEntriesSelection(catNums, isSelected)
    }

    private fun importSatEntries(stream: InputStream): List<SatEntry> {
        return TLE.parseStream(stream).map { tle -> SatEntry(tle) }
    }
}
