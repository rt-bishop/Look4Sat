package com.rtbishop.look4sat.data

import com.rtbishop.look4sat.domain.model.SatEntry
import com.rtbishop.look4sat.domain.model.SatItem
import com.rtbishop.look4sat.domain.model.SatTrans
import com.rtbishop.look4sat.domain.predict4kotlin.Satellite
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.zip.ZipInputStream

class SatelliteRepo(
    private val localSource: LocalDataSource,
    private val remoteSource: RemoteDataSource,
    private val ioDispatcher: CoroutineDispatcher
) {
    private val defaultSources = listOf(
        "https://celestrak.com/NORAD/elements/active.txt",
        "https://amsat.org/tle/current/nasabare.txt",
        "https://www.prismnet.com/~mmccants/tles/classfd.zip",
        "https://www.prismnet.com/~mmccants/tles/inttles.zip"
    )

    fun getSatItems(): Flow<List<SatItem>> {
        return localSource.getSatItems()
    }

    fun getSatTransmitters(catNum: Int): Flow<List<SatTrans>> {
        return localSource.getSatTransmitters(catNum)
    }

    suspend fun getSelectedSatellites(): List<Satellite> {
        return localSource.getSelectedSatellites()
    }

    suspend fun updateEntriesFromFile(stream: InputStream) = withContext(ioDispatcher) {
        localSource.updateEntries(importSatEntries(stream))
    }

    suspend fun updateEntriesFromWeb(sources: List<String> = defaultSources) {
        coroutineScope {
            launch(ioDispatcher) {
                val streams = mutableListOf<InputStream>()
                val entries = mutableListOf<SatEntry>()
                sources.forEach { source ->
                    remoteSource.fetchDataStream(source)?.let { stream ->
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
            launch(ioDispatcher) {
                val transmitters = remoteSource.fetchTransmitters().filter { it.isAlive }
                localSource.updateTransmitters(transmitters)
            }
        }
    }

    suspend fun updateEntriesSelection(catNums: List<Int>, isSelected: Boolean) {
        localSource.updateEntriesSelection(catNums, isSelected)
    }

    private fun importSatEntries(stream: InputStream): List<SatEntry> {
        return Satellite.importElements(stream).map { tle -> SatEntry(tle) }
    }
}
