package com.rtbishop.look4sat.framework.db

import com.rtbishop.look4sat.data.SatDataLocalSource
import com.rtbishop.look4sat.domain.model.SatEntry
import com.rtbishop.look4sat.domain.model.SatItem
import com.rtbishop.look4sat.domain.model.SatTrans
import com.rtbishop.look4sat.domain.predict4kotlin.Satellite
import com.rtbishop.look4sat.utility.DataMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SatDataLocal(private val satDataDao: SatDataDao) : SatDataLocalSource {

    override fun getSatItems(): Flow<List<SatItem>> {
        return satDataDao.getSatItems()
            .map { satItems -> DataMapper.satItemsToDomainItems(satItems) }
    }

    override suspend fun getSelectedSatellites(): List<Satellite> {
        return satDataDao.getSelectedSatellites()
    }

    override suspend fun updateEntries(entries: List<SatEntry>) {
        val satEntries = entries.map { entry -> DataMapper.domainEntryToSatEntry(entry) }
        satDataDao.updateEntries(satEntries)
    }

    override suspend fun updateEntriesSelection(catNums: List<Int>, isSelected: Boolean) {
        satDataDao.updateEntriesSelection(catNums, isSelected)
    }

    override fun getSatTransmitters(catNum: Int): Flow<List<SatTrans>> {
        return satDataDao.getSatTransmitters(catNum)
            .map { satTransList -> DataMapper.satTransListToDomainTransList(satTransList) }
    }

    override suspend fun updateTransmitters(satelliteTrans: List<SatTrans>) {
        val satTransList = DataMapper.domainTransListToSatTransList(satelliteTrans)
        satDataDao.updateTransmitters(satTransList)
    }
}
