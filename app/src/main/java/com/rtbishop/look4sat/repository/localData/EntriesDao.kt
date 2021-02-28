/*******************************************************************************
 Look4Sat. Amateur radio satellite tracker and pass predictor.
 Copyright (C) 2019, 2020 Arty Bishop (bishop.arty@gmail.com)

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along
 with this program; if not, write to the Free Software Foundation, Inc.,
 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 ******************************************************************************/

package com.rtbishop.look4sat.repository.localData

import androidx.room.*
import com.rtbishop.look4sat.data.SatEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface EntriesDao {
    
    @Query("SELECT * FROM entries ORDER BY name ASC")
    fun getEntries(): Flow<List<SatEntry>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<SatEntry>)
    
    @Query("SELECT * FROM entries WHERE isSelected = 1")
    suspend fun getSelectedEntries(): List<SatEntry>
    
    @Query("SELECT catNum FROM entries WHERE isSelected = 1")
    suspend fun getSelectedCatNums(): List<Int>
    
    @Transaction
    suspend fun updateEntriesSelection(catNums: List<Int>) {
        clearSelection()
        catNums.forEach { catNum -> updateSelection(catNum) }
    }
    
    @Query("UPDATE entries SET isSelected = 0")
    suspend fun clearSelection()
    
    @Query("UPDATE entries SET isSelected = 1 WHERE catNum = :catNum")
    suspend fun updateSelection(catNum: Int)
}
