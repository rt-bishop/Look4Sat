package com.rtbishop.look4sat.repo.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rtbishop.look4sat.data.SatTrans

@Dao
interface TransDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransmitters(satTrans: List<SatTrans>)

    @Query("SELECT * FROM transmitters WHERE isAlive = 1 and catNum = :catNum")
    suspend fun getTransmittersForCatNum(catNum: Int): List<SatTrans>

    @Query("DELETE FROM transmitters")
    suspend fun clearTransmitters()
}
