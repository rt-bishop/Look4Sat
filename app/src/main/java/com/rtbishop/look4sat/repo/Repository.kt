package com.rtbishop.look4sat.repo

import android.net.Uri
import com.rtbishop.look4sat.data.SatEntry
import com.rtbishop.look4sat.data.SatTrans
import com.rtbishop.look4sat.data.TleSource
import java.io.InputStream

interface Repository {

    suspend fun updateEntriesFromFile(tleUri: Uri)

    suspend fun updateEntriesFromUrl(urlList: List<TleSource>)

    ////////////////////////////////////////////////////////////////////////////////////////////////

    suspend fun fetchTleStreams(urlList: List<TleSource>): List<InputStream>

    suspend fun fetchTransmitters(): List<SatTrans>

    suspend fun insertEntries(entries: List<SatEntry>)

    suspend fun getAllEntries(): List<SatEntry>

    suspend fun getSelectedEntries(): List<SatEntry>

    suspend fun updateEntriesSelection(catNumList: List<Int>)

    suspend fun clearEntries()

    ////////////////////////////////////////////////////////////////////////////////////////////////

    suspend fun updateTransmitters()

    suspend fun getTransmittersByCatNum(catNum: Int): List<SatTrans>

    suspend fun clearTransmitters()

    ////////////////////////////////////////////////////////////////////////////////////////////////

    suspend fun updateSources(sources: List<TleSource>)

    suspend fun getSources(): List<TleSource>

    suspend fun clearSources()
}
