package com.rtbishop.look4sat.data

import com.rtbishop.look4sat.domain.model.SatEntry
import com.rtbishop.look4sat.domain.model.SatItem
import com.rtbishop.look4sat.domain.model.SatTrans
import com.rtbishop.look4sat.predict4kotlin.Satellite
import kotlinx.coroutines.flow.Flow

interface LocalDataSource {

    fun getSatItems(): Flow<List<SatItem>>

    suspend fun getSelectedSatellites(): List<Satellite>

    suspend fun updateEntries(entries: List<SatEntry>)

    suspend fun updateEntriesSelection(catNums: List<Int>, isSelected: Boolean)

    fun getTransmittersForSat(catNum: Int): Flow<List<SatTrans>>

    suspend fun updateTransmitters(satelliteTrans: List<SatTrans>)
}
