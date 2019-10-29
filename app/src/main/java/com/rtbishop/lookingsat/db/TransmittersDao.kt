package com.rtbishop.lookingsat.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rtbishop.lookingsat.repo.Transmitter

@Dao
interface TransmittersDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transmitters: List<Transmitter>)

    @Query("SELECT * FROM transmitters WHERE noradCatId = :id")
    suspend fun getTransmittersForSat(id: Int): List<Transmitter>
}