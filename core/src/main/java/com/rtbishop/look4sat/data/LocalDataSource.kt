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
package com.rtbishop.look4sat.data

import com.rtbishop.look4sat.domain.model.SatEntry
import com.rtbishop.look4sat.domain.model.SatItem
import com.rtbishop.look4sat.domain.model.Transmitter
import com.rtbishop.look4sat.domain.predict.Satellite
import kotlinx.coroutines.flow.Flow

interface LocalDataSource {

    fun getEntriesWithModes(): Flow<List<SatItem>>

    fun getTransmitters(catNum: Int): Flow<List<Transmitter>>

    suspend fun getSources(): List<String>

    suspend fun getSelectedSatellites(): List<Satellite>

    suspend fun updateEntries(entries: List<SatEntry>)

    suspend fun updateEntriesSelection(catNums: List<Int>, isSelected: Boolean)

    suspend fun updateSources(sources: List<String>)

    suspend fun updateTransmitters(transmitters: List<Transmitter>)
}
