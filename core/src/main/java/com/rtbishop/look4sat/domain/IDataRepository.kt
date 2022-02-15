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

import com.rtbishop.look4sat.domain.model.DataState
import com.rtbishop.look4sat.domain.model.SatItem
import com.rtbishop.look4sat.domain.model.Transmitter
import com.rtbishop.look4sat.domain.predict.Satellite
import kotlinx.coroutines.flow.StateFlow

interface IDataRepository {

    val updateState: StateFlow<DataState<Long>>

    suspend fun getAllSatellites(): List<SatItem>

    suspend fun getSelectedSatellites(): List<Satellite>

    suspend fun getTransmitters(catnum: Int): List<Transmitter>

    fun updateFromFile(uri: String)

    fun updateFromWeb(sources: List<String>)

    fun updatesSelection(catnums: List<Int>)

    fun setUpdateStateHandled()

    fun clearAllData()
}
