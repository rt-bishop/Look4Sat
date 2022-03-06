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
import com.rtbishop.look4sat.framework.model.SatEntry
import com.rtbishop.look4sat.framework.model.SatItem
import kotlinx.coroutines.flow.Flow

@Dao
interface EntriesDao {

    @Query("SELECT COUNT(*) FROM entries")
    fun getEntriesTotal(): Flow<Int>

    @Transaction
    @Query("SELECT catnum, name FROM entries ORDER BY name ASC")
    suspend fun getEntriesWithModes(): List<SatItem>

    @Transaction
    @Query("SELECT * FROM entries WHERE catnum IN (:selectedIds)")
    suspend fun getEntriesWithIds(selectedIds: List<Int>): List<SatEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<SatEntry>)

    @Query("DELETE FROM entries")
    suspend fun deleteEntries()
}
