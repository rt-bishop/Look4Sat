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
package com.rtbishop.look4sat.framework.local

import android.content.ContentResolver
import android.net.Uri
import com.rtbishop.look4sat.data.ILocalSource
import com.rtbishop.look4sat.data.ISettingsHandler
import com.rtbishop.look4sat.domain.model.SatEntry
import com.rtbishop.look4sat.domain.model.SatItem
import com.rtbishop.look4sat.domain.model.SatRadio
import com.rtbishop.look4sat.domain.predict.Satellite
import com.rtbishop.look4sat.framework.toDomainItems
import com.rtbishop.look4sat.framework.toDomainRadios
import com.rtbishop.look4sat.framework.toFrameworkEntries
import com.rtbishop.look4sat.framework.toFrameworkRadios
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.InputStream

class LocalSource(
    private val entriesDao: SatEntriesDao,
    private val radiosDao: SatRadiosDao,
    private val settings: ISettingsHandler,
    private val resolver: ContentResolver,
    private val ioDispatcher: CoroutineDispatcher
) : ILocalSource {

    override fun getEntriesNumber() = entriesDao.getEntriesNumber()

    override fun getRadiosNumber() = radiosDao.getRadiosNumber()

    override suspend fun getEntriesWithModes(): List<SatItem> {
        val selectedCatnums = getEntriesSelection()
        val entriesWithModes = entriesDao.getEntriesWithModes().toDomainItems()
        entriesWithModes.forEach { entry -> entry.isSelected = entry.catnum in selectedCatnums }
        return entriesWithModes
    }

    override suspend fun getSelectedEntries(): List<Satellite> {
        val selectedSatellites = mutableListOf<Satellite>()
        getEntriesSelection().chunked(999).forEach { catnums ->
            val entries = entriesDao.getSelectedEntries(catnums)
            selectedSatellites.addAll(entries.map { entry -> entry.data.createSat() })
        }
        return selectedSatellites
    }

    override suspend fun getRadios(catnum: Int): List<SatRadio> {
        return radiosDao.getRadios(catnum).toDomainRadios()
    }

    override suspend fun getFileStream(uri: String): InputStream? {
        return withContext(ioDispatcher) { resolver.openInputStream(Uri.parse(uri)) }
    }

    override suspend fun insertEntries(entries: List<SatEntry>) {
        entriesDao.insertEntries(entries.toFrameworkEntries())
    }

    override suspend fun insertRadios(radios: List<SatRadio>) {
        radiosDao.insertRadios(radios.toFrameworkRadios())
    }

    override suspend fun clearAllData() {
        entriesDao.deleteEntries()
        radiosDao.deleteRadios()
    }

    override suspend fun getDataSources(): List<String> {
        return withContext(ioDispatcher) { settings.loadDataSources() }
    }

    override suspend fun setDataSources(sources: List<String>) {
        withContext(ioDispatcher) { settings.saveDataSources(sources) }
    }

    override suspend fun getEntriesSelection(): List<Int> {
        return withContext(ioDispatcher) { settings.loadEntriesSelection() }
    }

    override suspend fun setEntriesSelection(catnums: List<Int>) {
        withContext(ioDispatcher) { settings.saveEntriesSelection(catnums) }
    }
}
