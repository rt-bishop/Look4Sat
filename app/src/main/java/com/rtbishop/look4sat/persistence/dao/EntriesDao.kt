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

package com.rtbishop.look4sat.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rtbishop.look4sat.data.SatEntry

@Dao
interface EntriesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<SatEntry>)

    @Query("SELECT * FROM entries ORDER BY name ASC")
    suspend fun getAllEntries(): List<SatEntry>

    @Query("SELECT * FROM entries WHERE isSelected = 1 ORDER BY name ASC")
    suspend fun getSelectedEntries(): List<SatEntry>

    @Query("UPDATE entries SET isSelected = 1 WHERE catNum == :catNum")
    suspend fun updateEntrySelection(catNum: Int)

    @Query("UPDATE entries SET isSelected = 0")
    suspend fun clearEntriesSelection()
}
