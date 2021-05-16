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
