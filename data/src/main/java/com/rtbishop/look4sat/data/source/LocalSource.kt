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
package com.rtbishop.look4sat.data.source

import com.rtbishop.look4sat.data.database.entity.SatEntry as FrameworkEntry
import com.rtbishop.look4sat.data.database.entity.SatItem as FrameworkItem
import com.rtbishop.look4sat.data.database.entity.SatRadio as FrameworkRadio
import com.rtbishop.look4sat.domain.model.SatEntry as DomainEntry
import com.rtbishop.look4sat.domain.model.SatItem as DomainItem
import com.rtbishop.look4sat.domain.model.SatRadio as DomainRadio
import com.rtbishop.look4sat.data.database.dao.StorageDao
import com.rtbishop.look4sat.domain.model.SatRadio
import com.rtbishop.look4sat.domain.predict.Satellite
import com.rtbishop.look4sat.domain.source.ILocalSource

class LocalSource(private val storageDao: StorageDao) : ILocalSource {

    //region # Entries region

    override suspend fun getEntriesTotal() = storageDao.getEntriesTotal()

    override suspend fun getEntriesList(): List<DomainItem> {
        return storageDao.getEntriesList().toDomainItems()
    }

    override suspend fun getEntriesWithIds(ids: List<Int>): List<Satellite> {
        val selectedSatellites = mutableListOf<Satellite>()
        ids.chunked(999).forEach { idsPart ->
            val entries = storageDao.getEntriesWithIds(idsPart)
            selectedSatellites.addAll(entries.map { entry -> entry.data.getSatellite() })
        }
        return selectedSatellites
    }

    override suspend fun insertEntries(entries: List<DomainEntry>) {
        storageDao.insertEntries(entries.toFrameworkEntries())
    }

    override suspend fun deleteEntries() = storageDao.deleteEntries()

    override suspend fun getIdsWithModes(modes: List<String>) = storageDao.getIdsWithModes(modes)

    private fun DomainEntry.toFramework() = FrameworkEntry(this.data, this.comment)

    private fun FrameworkItem.toDomain() = DomainItem(this.catnum, this.name, false)

    private fun List<DomainEntry>.toFrameworkEntries() = this.map { entry -> entry.toFramework() }

    private fun List<FrameworkItem>.toDomainItems() = this.map { item -> item.toDomain() }

    //endregion

    //region # Radios region

    override suspend fun getRadiosTotal() = storageDao.getRadiosTotal()

    override suspend fun getRadiosWithId(id: Int): List<SatRadio> {
        return storageDao.getRadiosWithId(id).toDomainRadios()
    }

    override suspend fun insertRadios(radios: List<SatRadio>) {
        storageDao.insertRadios(radios.toFrameworkRadios())
    }

    override suspend fun deleteRadios() = storageDao.deleteRadios()

    private fun DomainRadio.toFramework() = FrameworkRadio(
        this.uuid, this.info, this.isAlive, this.downlinkLow, this.downlinkHigh, this.downlinkMode,
        this.uplinkLow, this.uplinkHigh, this.uplinkMode, this.isInverted, this.catnum, this.comment
    )

    private fun FrameworkRadio.toDomain() = DomainRadio(
        this.uuid, this.info, this.isAlive, this.downlinkLow, this.downlinkHigh, this.downlinkMode,
        this.uplinkLow, this.uplinkHigh, this.uplinkMode, this.isInverted, this.catnum, this.comment
    )

    private fun List<DomainRadio>.toFrameworkRadios() = this.map { radio -> radio.toFramework() }

    private fun List<FrameworkRadio>.toDomainRadios() = this.map { radio -> radio.toDomain() }

    //endregion
}
