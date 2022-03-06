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
package com.rtbishop.look4sat.framework.data.dao

import androidx.room.*
import com.rtbishop.look4sat.framework.model.SatRadio
import kotlinx.coroutines.flow.Flow

@Dao
interface RadiosDao {

    @Query("SELECT COUNT(*) FROM radios")
    fun getRadiosTotal(): Flow<Int>

    @Transaction
    @Query("SELECT * FROM radios WHERE catnum = :id AND isAlive = 1")
    suspend fun getRadiosWithId(id: Int): List<SatRadio>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRadios(radios: List<SatRadio>)

    @Query("DELETE FROM radios")
    suspend fun deleteRadios()
}
