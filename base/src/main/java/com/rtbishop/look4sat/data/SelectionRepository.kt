package com.rtbishop.look4sat.data

import com.rtbishop.look4sat.domain.IDataRepository
import com.rtbishop.look4sat.domain.ISettingsRepository
import com.rtbishop.look4sat.model.SatItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class SelectionRepository(
    private val dispatcher: CoroutineDispatcher,
    private val dataRepository: IDataRepository,
    private val settingsRepository: ISettingsRepository
) {
    private val currentItems = MutableStateFlow<List<SatItem>>(emptyList())
    private val currentType = MutableStateFlow("All")
    private val currentQuery = MutableStateFlow("")
    private val itemsWithType = currentType.flatMapLatest { type ->
        currentItems.map { items -> items.filterByType(type) }
    }
    private val itemsWithQuery = currentQuery.flatMapLatest { query ->
        itemsWithType.map { items -> items.filterByQuery(query) }
    }

    fun getCurrentType() = currentType.value

    fun getTypesList() = dataRepository.getSatelliteTypes()

    suspend fun getEntriesFlow() = withContext(dispatcher) {
        currentItems.value = dataRepository.getEntriesList()
        return@withContext itemsWithQuery
    }

    suspend fun setType(type: String) = withContext(dispatcher) { currentType.value = type }

    suspend fun setQuery(query: String) = withContext(dispatcher) { currentQuery.value = query }

    suspend fun updateSelection(selectAll: Boolean) = withContext(dispatcher) {
        updateSelection(itemsWithQuery.first().map { item -> item.catnum }, selectAll)
    }

    suspend fun updateSelection(catNums: List<Int>, isSelected: Boolean) = withContext(dispatcher) {
        currentItems.value = currentItems.value.map { item ->
            if (item.catnum in catNums) item.copy(isSelected = isSelected) else item
        }
    }

    suspend fun saveSelection() = withContext(dispatcher) {
        val currentSelection = currentItems.value.filter { it.isSelected }.map { it.catnum }
        settingsRepository.saveEntriesSelection(currentSelection)
    }

    private suspend fun List<SatItem>.filterByType(type: String) = withContext(dispatcher) {
        if (type == "All") return@withContext this@filterByType
        val catnums = settingsRepository.loadSatType(type)
        if (catnums.isEmpty()) return@withContext this@filterByType
        return@withContext this@filterByType.filter { item -> item.catnum in catnums }
    }

    private suspend fun List<SatItem>.filterByQuery(query: String) = withContext(dispatcher) {
        if (query.isBlank()) return@withContext this@filterByQuery
        return@withContext try {
            this@filterByQuery.filter { it.catnum == query.toInt() }
        } catch (e: Exception) {
            this@filterByQuery.filter { item -> item.name.lowercase().contains(query.lowercase()) }
        }
    }
}
