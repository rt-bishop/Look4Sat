package com.rtbishop.look4sat.data

import com.rtbishop.look4sat.domain.SatelliteRepo
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

class DefaultSatelliteRepo(
    private val localSource: LocalDataSource,
    private val remoteSource: RemoteDataSource,
    private val ioDispatcher: CoroutineDispatcher
) : SatelliteRepo {

    override fun getSatItems(): Flow<List<SatItem>> {
        return localSource.getSatItems()
    }

    override fun getTransmittersForSat(catNum: Int): Flow<List<SatTrans>> {
        return localSource.getTransmittersForSat(catNum)
    }

    override suspend fun getSelectedSatellites(): List<Satellite> {
        return localSource.getSelectedSatellites()
    }

    override suspend fun importDataFromFile(stream: InputStream) = withContext(ioDispatcher) {
        val entries = Satellite.importTLE(stream).map { tle -> SatEntry(tle) }
        localSource.updateEntries(entries)
    }

    override suspend fun importDataFromWeb(sources: List<String>) {
        coroutineScope {
            launch(ioDispatcher) {
                val entries = mutableListOf<SatEntry>()
                val streams = mutableListOf<InputStream>()
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
                streams.forEach { stream ->
                    val importedEntries = Satellite.importTLE(stream).map { tle -> SatEntry(tle) }
                    entries.addAll(importedEntries)
                }
                localSource.updateEntries(entries)
            }
            launch(ioDispatcher) {
                val transmitters = remoteSource.fetchTransmitters().filter { it.isAlive }
                localSource.updateTransmitters(transmitters)
            }
        }
    }

    override suspend fun updateEntriesSelection(catNums: List<Int>, isSelected: Boolean) {
        localSource.updateEntriesSelection(catNums, isSelected)
    }
}
