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

import androidx.room.*
import com.rtbishop.look4sat.framework.model.SatEntry
import com.rtbishop.look4sat.framework.model.SatItem
import kotlinx.coroutines.flow.Flow

@Dao
interface EntriesDao {

    @Transaction
    @Query("SELECT catnum, name, isSelected FROM entries ORDER BY name ASC")
    fun getSatelliteItems(): Flow<List<SatItem>>

    @Query("SELECT catnum FROM entries WHERE isSelected = 1")
    suspend fun getEntriesSelection(): List<Int>

    @Query("SELECT * FROM entries WHERE isSelected = 1")
    suspend fun getSelectedSatellites(): List<SatEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<SatEntry>)

    @Transaction
    suspend fun updateEntries(entries: List<SatEntry>) {
        val entriesSelection = getEntriesSelection()
        insertEntries(entries)
        restoreEntriesSelection(entriesSelection)
    }

    @Transaction
    suspend fun restoreEntriesSelection(catnums: List<Int>) {
        updateEntriesSelection(catnums)
    }

    @Transaction
    suspend fun updateEntriesSelection(catnums: List<Int>) {
        clearEntriesSelection()
        catnums.forEach { catnum -> updateEntrySelection(catnum) }
    }

    @Query("UPDATE entries SET isSelected = 1 WHERE catnum = :catnum")
    suspend fun updateEntrySelection(catnum: Int)

    @Query("UPDATE entries SET isSelected = 0")
    suspend fun clearEntriesSelection()

    @Query("DELETE FROM entries")
    suspend fun deleteEntries()
}
