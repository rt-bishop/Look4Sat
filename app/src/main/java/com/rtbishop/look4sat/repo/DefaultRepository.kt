/*
 * Look4Sat. Amateur radio and weather satellite tracker and passes predictor for Android.
 * Copyright (C) 2019, 2020 Arty Bishop (bishop.arty@gmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.rtbishop.look4sat.repo

import android.content.ContentResolver
import android.net.Uri
import com.github.amsacode.predict4java.TLE
import com.rtbishop.look4sat.data.SatEntry
import com.rtbishop.look4sat.data.TleSource
import com.rtbishop.look4sat.data.Transmitter
import com.rtbishop.look4sat.network.RemoteSource
import com.rtbishop.look4sat.persistence.LocalSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DefaultRepository @Inject constructor(
    private val resolver: ContentResolver,
    private val localSource: LocalSource,
    private val remoteSource: RemoteSource
) : Repository {

    override suspend fun updateEntriesFromFile(tleUri: Uri) {
        resolver.openInputStream(tleUri)?.use {
            withContext(Dispatchers.IO) {
                val tleList = TLE.importSat(it)
                val entries = tleList.map { SatEntry(it) }
                localSource.insertEntries(entries)
            }
        }
    }

    override suspend fun updateEntriesFromUrl(urlList: List<TleSource>) {
        val stream = remoteSource.fetchTleStream(urlList)
        withContext(Dispatchers.IO) {
            val tleList = TLE.importSat(stream)
            val entries = tleList.map { SatEntry(it) }
            localSource.insertEntries(entries)
        }
    }

    override suspend fun getAllEntries(): List<SatEntry> {
        return localSource.getAllEntries()
    }

    override suspend fun getSelectedEntries(): List<SatEntry> {
        return localSource.getSelectedEntries()
    }

    override suspend fun updateEntriesSelection(catNumList: List<Int>) {
        localSource.updateEntriesSelection(catNumList)
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    override suspend fun updateTransmitters() {
        localSource.insertTransmitters(remoteSource.fetchTransmitters())
    }

    override suspend fun getTransmittersByCatNum(catNum: Int): List<Transmitter> {
        return localSource.getTransmittersByCatNum(catNum)
    }
}
