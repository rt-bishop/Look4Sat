package com.rtbishop.look4sat.repo.local

import androidx.room.*
import com.rtbishop.look4sat.data.SatTrans

@Dao
interface TransmittersDao {

    @Query("SELECT * FROM transmitters WHERE isAlive = 1 and catNum = :satId")
    suspend fun getTransmittersForSatId(satId: Int): List<SatTrans>

    @Transaction
    suspend fun updateTransmitters(transmitters: List<SatTrans>) {
        clearTransmitters()
        insertTransmitters(transmitters)
    }

    @Query("DELETE FROM transmitters")
    suspend fun clearTransmitters()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransmitters(transmitters: List<SatTrans>)
}
