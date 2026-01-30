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
package com.rtbishop.look4sat.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.rtbishop.look4sat.data.database.entity.SatEntry
import com.rtbishop.look4sat.data.database.entity.SatRadio
import com.rtbishop.look4sat.domain.model.SatItem

@Dao
interface Look4SatDao {

    @Query("SELECT COUNT(*) FROM entries")
    suspend fun getEntriesTotal(): Int

    @Query("SELECT catnum, name, 0 as isSelected FROM entries ORDER BY name ASC")
    suspend fun getEntriesList(): List<SatItem>

    @Transaction
    @Query("SELECT * FROM entries WHERE catnum IN (:selectedIds)")
    suspend fun getEntriesWithIds(selectedIds: List<Int>): List<SatEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<SatEntry>)

    @Query("DELETE FROM entries")
    suspend fun deleteEntries()

    @Query("SELECT catnum FROM radios WHERE downlinkMode IN (:modes)")
    suspend fun getIdsWithModes(modes: List<String>): List<Int>

    @Query("SELECT COUNT(*) FROM radios")
    suspend fun getRadiosTotal(): Int

    @Transaction
    @Query("SELECT * FROM radios WHERE catnum = :id AND isAlive = 1")
    suspend fun getRadiosWithId(id: Int): List<SatRadio>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRadios(radios: List<SatRadio>)

    @Query("DELETE FROM radios")
    suspend fun deleteRadios()
}
