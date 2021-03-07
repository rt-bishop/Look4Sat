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
package com.rtbishop.look4sat.repository.localData

import androidx.room.*
import com.github.amsacode.predict4java.Satellite
import com.rtbishop.look4sat.data.SatEntry
import com.rtbishop.look4sat.data.SatItem
import com.rtbishop.look4sat.data.SatTrans
import kotlinx.coroutines.flow.Flow

@Dao
interface SatelliteDao {
    
    @Query("SELECT * FROM entries ORDER BY name ASC")
    fun getEntries(): Flow<List<SatEntry>>
    
    @Query("SELECT catNum, name, isSelected FROM entries ORDER BY name ASC")
    fun getSatItems(): Flow<List<SatItem>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<SatEntry>)
    
    @Query("SELECT tle FROM entries WHERE isSelected = 1")
    suspend fun getSelectedSatellites(): List<Satellite>
    
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
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransmitters(transmitters: List<SatTrans>)
    
    @Query("SELECT * FROM transmitters WHERE isAlive = 1 and catNum = :catNum")
    fun getTransmittersForSat(catNum: Int): Flow<List<SatTrans>>
}
