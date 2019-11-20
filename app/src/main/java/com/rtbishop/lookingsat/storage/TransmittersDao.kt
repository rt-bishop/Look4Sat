package com.rtbishop.lookingsat.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rtbishop.lookingsat.repo.Transmitter

@Dao
interface TransmittersDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransmitters(transmitters: List<Transmitter>)

    @Query("SELECT * FROM transmitters WHERE isAlive = 1 and noradCatId = :id")
    suspend fun getTransmittersForSat(id: Int): List<Transmitter>
}