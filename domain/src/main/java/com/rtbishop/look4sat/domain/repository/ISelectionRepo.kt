package com.rtbishop.look4sat.domain.repository

import com.rtbishop.look4sat.domain.model.SatItem
import kotlinx.coroutines.flow.Flow

interface ISelectionRepo {
    fun getCurrentTypes(): List<String>
    fun getTypesList(): List<String>
    suspend fun getEntriesFlow(): Flow<List<SatItem>>
    suspend fun setTypes(types: List<String>)
    suspend fun setQuery(query: String)
    suspend fun setSelection(selectAll: Boolean)
    suspend fun setSelection(ids: List<Int>, isTicked: Boolean)
    suspend fun saveSelection()
}
