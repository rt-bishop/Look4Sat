package com.rtbishop.look4sat.data.repository

import com.rtbishop.look4sat.domain.model.SatItem
import com.rtbishop.look4sat.domain.repository.ISelectionRepo
import com.rtbishop.look4sat.domain.repository.ISettingsRepo
import com.rtbishop.look4sat.domain.source.ILocalSource
import com.rtbishop.look4sat.domain.source.Sources
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class SelectionRepo(
    private val dispatcher: CoroutineDispatcher,
    private val localSource: ILocalSource,
    private val settingsRepo: ISettingsRepo
) : ISelectionRepo {

    private val currentItems = MutableStateFlow<List<SatItem>>(emptyList())
    private val currentType = MutableStateFlow("All")
    private val currentQuery = MutableStateFlow("")
    private val itemsWithType = currentType.flatMapLatest { type ->
        currentItems.map { items -> items.filterByType(type) }
    }
    private val itemsWithQuery = currentQuery.flatMapLatest { query ->
        itemsWithType.map { items -> items.filterByQuery(query) }
    }

    override fun getCurrentType() = currentType.value

    override fun getTypesList() = Sources.satelliteDataUrls.keys.sorted()

    override suspend fun getEntriesFlow() = withContext(dispatcher) {
        val selectedIds = settingsRepo.selectedIds.value
        currentItems.value = localSource.getEntriesList().map { item ->
            item.copy(isSelected = item.catnum in selectedIds)
        }
        return@withContext itemsWithQuery
    }

    override suspend fun setType(type: String) = withContext(dispatcher) {
        currentType.value = type
    }

    override suspend fun setQuery(query: String) = withContext(dispatcher) {
        currentQuery.value = query
    }

    override suspend fun setSelection(selectAll: Boolean) = withContext(dispatcher) {
        setSelection(itemsWithQuery.first().map { item -> item.catnum }, selectAll)
    }

    override suspend fun setSelection(ids: List<Int>, isTicked: Boolean) = withContext(dispatcher) {
        currentItems.value = currentItems.value.map { item ->
            if (item.catnum in ids) item.copy(isSelected = isTicked) else item
        }
    }

    override suspend fun saveSelection() = withContext(dispatcher) {
        val currentSelection = currentItems.value.filter { it.isSelected }.map { it.catnum }
        settingsRepo.setSelectedIds(currentSelection)
    }

    private suspend fun List<SatItem>.filterByType(type: String) = withContext(dispatcher) {
        if (type == "All") return@withContext this@filterByType
        val catnums = settingsRepo.getSatelliteTypeIds(type)
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
