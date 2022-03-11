/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2022 Arty Bishop (bishop.arty@gmail.com)
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
package com.rtbishop.look4sat.framework.data

import com.rtbishop.look4sat.domain.data.ILocalEntrySource
import com.rtbishop.look4sat.domain.predict.Satellite
import com.rtbishop.look4sat.framework.data.dao.EntriesDao
import com.rtbishop.look4sat.domain.model.SatEntry as DomainEntry
import com.rtbishop.look4sat.domain.model.SatItem as DomainItem
import com.rtbishop.look4sat.framework.model.SatEntry as FrameworkEntry
import com.rtbishop.look4sat.framework.model.SatItem as FrameworkItem

class LocalEntrySource(private val entriesDao: EntriesDao) : ILocalEntrySource {

    override fun getEntriesTotal() = entriesDao.getEntriesTotal()

    override suspend fun getEntriesWithModes(): List<DomainItem> {
        return entriesDao.getEntriesWithModes().toDomainItems()
    }

    override suspend fun getEntriesWithIds(ids: List<Int>): List<Satellite> {
        val selectedSatellites = mutableListOf<Satellite>()
        ids.chunked(999).forEach { idsPart ->
            val entries = entriesDao.getEntriesWithIds(idsPart)
            selectedSatellites.addAll(entries.map { entry -> entry.data.getSatellite() })
        }
        return selectedSatellites
    }

    override suspend fun insertEntries(entries: List<DomainEntry>) {
        entriesDao.insertEntries(entries.toFrameworkEntries())
    }

    override suspend fun deleteEntries() = entriesDao.deleteEntries()

    private fun DomainEntry.toFramework() = FrameworkEntry(this.data, this.comment)

    private fun FrameworkItem.toDomain() = DomainItem(this.catnum, this.name, this.modes, false)

    private fun List<DomainEntry>.toFrameworkEntries() = this.map { entry -> entry.toFramework() }

    private fun List<FrameworkItem>.toDomainItems() = this.map { item -> item.toDomain() }
}
