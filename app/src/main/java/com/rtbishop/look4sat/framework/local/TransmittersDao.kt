package com.rtbishop.look4sat.framework.local

import androidx.room.*
import com.rtbishop.look4sat.framework.model.Transmitter
import kotlinx.coroutines.flow.Flow

@Dao
interface TransmittersDao {

    @Query("SELECT * FROM transmitters WHERE catnum = :catnum")
    fun getTransmitters(catnum: Int): Flow<List<Transmitter>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateTransmitters(transmitters: List<Transmitter>)
}
