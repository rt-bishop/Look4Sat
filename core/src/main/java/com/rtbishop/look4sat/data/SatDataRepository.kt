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

class SatDataRepository(
    private val preferencesSource: PreferencesSource,
    private val satLocalSource: SatDataLocalSource,
    private val satRemoteSource: SatDataRemoteSource,
    private val ioDispatcher: CoroutineDispatcher
) {
    private val defaultSources = listOf(
        "https://celestrak.com/NORAD/elements/active.txt",
        "https://amsat.org/tle/current/nasabare.txt",
        "https://www.prismnet.com/~mmccants/tles/classfd.zip",
        "https://www.prismnet.com/~mmccants/tles/inttles.zip"
    )

    private val transmittersModes = arrayOf(
        "AFSK", "AFSK S-Net", "AFSK SALSAT", "AHRPT", "AM", "APT", "BPSK", "BPSK PMT-A3",
        "CERTO", "CW", "DQPSK", "DSTAR", "DUV", "FFSK", "FM", "FMN", "FSK", "FSK AX.100 Mode 5",
        "FSK AX.100 Mode 6", "FSK AX.25 G3RUH", "GFSK", "GFSK Rktr", "GMSK", "HRPT", "LoRa",
        "LRPT", "LSB", "MFSK", "MSK", "MSK AX.100 Mode 5", "MSK AX.100 Mode 6", "OFDM", "OQPSK",
        "PSK", "PSK31", "PSK63", "QPSK", "QPSK31", "QPSK63", "SSTV", "USB", "WSJT"
    )

    fun getAllModes(): Array<String> {
        return transmittersModes
    }

    fun saveModesSelection(modes: List<String>) {
        preferencesSource.saveModesSelection(modes)
    }

    fun loadModesSelection(): List<String> {
        return preferencesSource.loadModesSelection()
    }

    fun getSatItems(): Flow<List<SatItem>> {
        return satLocalSource.getSatItems()
    }

    fun getSatTransmitters(catNum: Int): Flow<List<SatTrans>> {
        return satLocalSource.getSatTransmitters(catNum)
    }

    suspend fun getSelectedSatellites(): List<Satellite> {
        return satLocalSource.getSelectedSatellites()
    }

    suspend fun updateEntriesFromFile(stream: InputStream) = withContext(ioDispatcher) {
        satLocalSource.updateEntries(importSatEntries(stream))
    }

    suspend fun updateEntriesFromWeb(sources: List<String> = defaultSources) {
        coroutineScope {
            launch(ioDispatcher) {
                val streams = mutableListOf<InputStream>()
                val entries = mutableListOf<SatEntry>()
                sources.forEach { source ->
                    satRemoteSource.fetchDataStream(source)?.let { stream ->
                        if (source.contains(".zip", true)) {
                            val zipStream = ZipInputStream(stream).apply { nextEntry }
                            streams.add(zipStream)
                        } else {
                            streams.add(stream)
                        }
                    }
                }
                streams.forEach { stream -> entries.addAll(importSatEntries(stream)) }
                satLocalSource.updateEntries(entries)
            }
            launch(ioDispatcher) {
                val transmitters = satRemoteSource.fetchTransmitters().filter { it.isAlive }
                satLocalSource.updateTransmitters(transmitters)
            }
        }
    }

    suspend fun updateEntriesSelection(catNums: List<Int>, isSelected: Boolean) {
        satLocalSource.updateEntriesSelection(catNums, isSelected)
    }

    private fun importSatEntries(stream: InputStream): List<SatEntry> {
        return Satellite.importElements(stream).map { tle -> SatEntry(tle) }
    }
}
