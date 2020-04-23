package com.rtbishop.look4sat.repo

import com.rtbishop.look4sat.data.SatEntry
import com.rtbishop.look4sat.data.Transmitter

interface Repository {

    suspend fun updateEntriesFrom(urlList: List<String>)

    suspend fun getAllEntries(): List<SatEntry>

    suspend fun getSelectedEntries(): List<SatEntry>

    suspend fun updateEntriesSelection(catNumList: List<Int>)

    ////////////////////////////////////////////////////////////////////////////////////////////////

    suspend fun updateTransmitters()

    suspend fun getTransmittersByCatNum(catNum: Int): List<Transmitter>
}
