package com.rtbishop.look4sat.framework.local

import androidx.room.*
import com.rtbishop.look4sat.framework.model.Source
import com.rtbishop.look4sat.framework.model.Transmitter

@Dao
interface SourcesDao {

    @Query("SELECT sourceUrl FROM sources")
    suspend fun getSources(): List<String>

    @Transaction
    suspend fun updateSources(sources: List<Source>) {
        deleteSources()
        insertSources(sources)
    }

    @Query("DELETE from sources")
    suspend fun deleteSources()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSources(sources: List<Source>)
}
