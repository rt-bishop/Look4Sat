/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2026 Arty Bishop and contributors.
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
package com.rtbishop.look4sat.core.data.source

import com.rtbishop.look4sat.core.data.database.Look4SatDao
import com.rtbishop.look4sat.core.domain.model.SatRadio
import com.rtbishop.look4sat.core.domain.predict.OrbitalData
import com.rtbishop.look4sat.core.domain.predict.OrbitalObject
import com.rtbishop.look4sat.core.domain.source.ILocalSource
import com.rtbishop.look4sat.core.data.database.entity.SatEntry as FrameworkEntry
import com.rtbishop.look4sat.core.data.database.entity.SatRadio as FrameworkRadio
import com.rtbishop.look4sat.core.domain.model.SatRadio as DomainRadio

class LocalSource(private val look4SatDao: Look4SatDao) : ILocalSource {

    //region # Entries region

    override suspend fun getEntriesTotal() = look4SatDao.getEntriesTotal()

    override suspend fun getEntriesList() = look4SatDao.getEntriesList()

    override suspend fun getEntriesWithIds(ids: List<Int>): List<OrbitalObject> {
        val selectedOrbitalObjects = mutableListOf<OrbitalObject>()
        ids.chunked(999).forEach { idsPart ->
            val entries = look4SatDao.getEntriesWithIds(idsPart)
            selectedOrbitalObjects.addAll(entries.toDomain().map { entry -> entry.getObject() })
        }
        return selectedOrbitalObjects
    }

    override suspend fun getEntriesEpochs() = look4SatDao.getEntriesEpochs()

    override suspend fun getEntriesNames() = look4SatDao.getEntriesNames()

    override suspend fun renameEntries(names: Map<Int, String>) =
        names.forEach { (catnum, name) -> look4SatDao.renameEntry(catnum, name) }

    override suspend fun deleteEntriesWithIds(ids: List<Int>) =
        ids.chunked(999).forEach { idsPart -> look4SatDao.deleteEntriesWithIds(idsPart) }

    override suspend fun insertEntries(entries: List<OrbitalData>) = look4SatDao.insertEntries(entries.toEntity())

    override suspend fun deleteEntries() = look4SatDao.deleteEntries()

    override suspend fun getIdsWithModes(modes: List<String>) = look4SatDao.getIdsWithModes(modes)

    private fun FrameworkEntry.toDomain() = OrbitalData(
        this.name, this.epoch, this.meanmo, this.eccn, this.incl,
        this.raan, this.argper, this.meanan, this.catnum, this.bstar, this.ndot
    )

    private fun OrbitalData.toEntity() = FrameworkEntry(
        this.name, this.epoch, this.meanmo, this.eccn, this.incl,
        this.raan, this.argper, this.meanan, this.catnum, this.bstar, this.ndot
    )

    private fun List<OrbitalData>.toEntity() = this.map { item -> item.toEntity() }

    private fun List<FrameworkEntry>.toDomain() = this.map { item -> item.toDomain() }

    //endregion

    //region # Radios region

    override suspend fun getRadiosTotal() = look4SatDao.getRadiosTotal()

    override suspend fun getRadiosWithId(id: Int): List<SatRadio> {
        return look4SatDao.getRadiosWithId(id).toDomainRadios()
    }

    override suspend fun insertRadios(radios: List<SatRadio>, isCustom: Boolean) {
        look4SatDao.insertRadios(radios.map { radio -> radio.toFramework(isCustom) })
    }

    override suspend fun deleteRadios() = look4SatDao.deleteRadios()

    override suspend fun deleteManagedRadios() = look4SatDao.deleteManagedRadios()

    private fun DomainRadio.toFramework(isCustom: Boolean = false) = FrameworkRadio(
        this.uuid, this.info, this.isAlive, this.downlinkLow, this.downlinkHigh, this.downlinkMode,
        this.uplinkLow, this.uplinkHigh, this.uplinkMode, this.isInverted, this.catnum, isCustom
    )

    private fun FrameworkRadio.toDomain() = DomainRadio(
        this.uuid, this.info, this.isAlive, this.downlinkLow, this.downlinkHigh, this.downlinkMode,
        this.uplinkLow, this.uplinkHigh, this.uplinkMode, this.isInverted, this.catnum
    )

    private fun List<FrameworkRadio>.toDomainRadios() = this.map { radio -> radio.toDomain() }

    //endregion
}
