package com.rtbishop.look4sat.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rtbishop.look4sat.data.Transmitter

@Dao
interface TransmittersDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransmitters(transmitters: List<Transmitter>)

    @Query("SELECT * FROM transmitters WHERE isAlive = 1 and catNum = :catNum")
    suspend fun getTransmittersByCatNum(catNum: Int): List<Transmitter>
}
