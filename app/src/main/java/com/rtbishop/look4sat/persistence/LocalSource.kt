package com.rtbishop.look4sat.persistence

import com.rtbishop.look4sat.data.SatEntry
import com.rtbishop.look4sat.data.Transmitter

interface LocalSource {

    suspend fun insertEntries(entries: List<SatEntry>)

    suspend fun getAllEntries(): List<SatEntry>

    suspend fun getSelectedEntries(): List<SatEntry>

    suspend fun updateEntriesSelection(catNumList: List<Int>)

    ////////////////////////////////////////////////////////////////////////////////////////////////

    suspend fun insertTransmitters(transmitters: List<Transmitter>)

    suspend fun getTransmittersByCatNum(catNum: Int): List<Transmitter>
}
