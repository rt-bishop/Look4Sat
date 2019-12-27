/*
 * LookingSat. Amateur radio & weather satellite tracker and passes calculator.
 * Copyright (C) 2019 Arty Bishop (bishop.arty@gmail.com)
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

import com.rtbishop.look4sat.network.RemoteDataSource
import com.rtbishop.look4sat.storage.LocalDataSource
import java.io.InputStream
import javax.inject.Inject

class Repository @Inject constructor(
    private val localSource: LocalDataSource,
    private val remoteSource: RemoteDataSource
) {
    suspend fun getStreamForUrl(urlList: List<String>): InputStream {
        return remoteSource.getStreamForUrl(urlList)
    }

    suspend fun updateTransmittersDatabase() {
        localSource.insertTransmitters(remoteSource.fetchTransmittersList())
    }

    suspend fun getTransmittersForSat(id: Int): List<Transmitter> {
        return localSource.getTransmittersForSat(id)
    }
}