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
package com.rtbishop.look4sat.framework.api

import com.rtbishop.look4sat.data.RemoteSource
import com.rtbishop.look4sat.domain.Transmitter
import com.rtbishop.look4sat.utility.DataMapper
import java.io.InputStream

class RemoteDataSource(private val satelliteApi: SatelliteApi) : RemoteSource {

    override suspend fun fetchFileStream(url: String): InputStream? {
        return satelliteApi.fetchFileStream(url).body()?.byteStream()
    }

    override suspend fun fetchTransmitters(): List<Transmitter> {
        return DataMapper.satTransListToDomainTransList(satelliteApi.fetchTransmitters())
    }
}
