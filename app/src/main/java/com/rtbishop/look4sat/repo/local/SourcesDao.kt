package com.rtbishop.look4sat.repo.local

import androidx.lifecycle.LiveData
import androidx.room.*
import com.rtbishop.look4sat.data.TleSource

@Dao
interface SourcesDao {

    @Query("SELECT * FROM sources")
    fun getSources(): LiveData<List<TleSource>>

    @Transaction
    suspend fun updateSources(sources: List<TleSource>) {
        clearSources()
        insertSources(sources)
    }

    @Query("DELETE FROM sources")
    suspend fun clearSources()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSources(sources: List<TleSource>)
}