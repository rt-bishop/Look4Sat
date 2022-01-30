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
package com.rtbishop.look4sat.domain

import com.rtbishop.look4sat.domain.model.SatItem
import com.rtbishop.look4sat.domain.model.Transmitter
import com.rtbishop.look4sat.domain.predict.Satellite
import kotlinx.coroutines.flow.Flow
import java.io.InputStream

interface IDataRepository {

    fun getSatelliteItems(): Flow<List<SatItem>>

    suspend fun getSelectedSatellites(): List<Satellite>

    suspend fun getTransmitters(catnum: Int): List<Transmitter>

    suspend fun updateDataFromFile(stream: InputStream)

    suspend fun updateDataFromWeb(sources: List<String>)

    suspend fun updateSelection(catnums: List<Int>, isSelected: Boolean = true)
}
