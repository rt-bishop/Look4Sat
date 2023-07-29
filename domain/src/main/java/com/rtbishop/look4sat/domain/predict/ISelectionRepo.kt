package com.rtbishop.look4sat.domain.predict

import com.rtbishop.look4sat.domain.model.SatItem
import kotlinx.coroutines.flow.Flow

interface ISelectionRepo {

    fun getCurrentType(): String

    fun getTypesList(): List<String>

    suspend fun getEntriesFlow(): Flow<List<SatItem>>

    suspend fun setType(type: String)

    suspend fun setQuery(query: String)

    suspend fun setSelection(selectAll: Boolean)

    suspend fun setSelection(ids: List<Int>, isTicked: Boolean)

    suspend fun saveSelection()
}
