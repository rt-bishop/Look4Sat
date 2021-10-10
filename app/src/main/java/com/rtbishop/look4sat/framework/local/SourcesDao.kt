package com.rtbishop.look4sat.framework.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rtbishop.look4sat.framework.model.DataSource
import kotlinx.coroutines.flow.Flow

@Dao
interface SourcesDao {

    @Query("SELECT sourceUrl FROM sources")
    suspend fun getSources(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSources(sources: List<DataSource>)

    @Query("DELETE from sources")
    suspend fun deleteSources()
}
