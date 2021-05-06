package com.rtbishop.look4sat.data

import com.rtbishop.look4sat.domain.model.SatEntry
import com.rtbishop.look4sat.domain.model.SatItem
import com.rtbishop.look4sat.domain.model.SatTrans
import com.rtbishop.look4sat.domain.predict4kotlin.Satellite
import kotlinx.coroutines.flow.Flow

interface LocalDataSource {

    fun getSatItems(): Flow<List<SatItem>>

    fun getSatTransmitters(catNum: Int): Flow<List<SatTrans>>

    suspend fun getSelectedSatellites(): List<Satellite>

    suspend fun updateEntries(entries: List<SatEntry>)

    suspend fun updateEntriesSelection(catNums: List<Int>, isSelected: Boolean)

    suspend fun updateTransmitters(satelliteTrans: List<SatTrans>)
}
