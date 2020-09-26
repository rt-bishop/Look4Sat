package com.rtbishop.look4sat.repo.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rtbishop.look4sat.data.TleSource

@Dao
interface SourcesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSources(sources: List<TleSource>)

    @Query("SELECT * FROM sources")
    suspend fun getSources(): List<TleSource>

    @Query("DELETE FROM sources")
    suspend fun clearSources()
}