/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2026 Arty Bishop and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.rtbishop.look4sat.core.data.repository

import com.rtbishop.look4sat.core.domain.model.SatItem
import com.rtbishop.look4sat.core.domain.repository.ISelectionRepo
import com.rtbishop.look4sat.core.domain.repository.ISettingsRepo
import com.rtbishop.look4sat.core.domain.source.ILocalSource
import com.rtbishop.look4sat.core.domain.source.Sources
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
    private val currentQuery = MutableStateFlow("")

    // Resolve sat IDs once when modes change, then filter items reactively.
    // The HashSet gives O(1) catnum lookups instead of O(n) with a List.
    // Directly observe settingsRepo.selectedSatModes to ensure real-time sync across screens.
    private val itemsWithModes = settingsRepo.selectedSatModes.flatMapLatest { list: List<String> ->
        val catnumSet: Set<Int>? = if (list.isEmpty()) {
            null // null = no filtering
        } else {
            val ids = localSource.getIdsWithModes(list)
            if (ids.isEmpty()) null else ids.toHashSet()
        }
        currentItems.map { items ->
            if (catnumSet == null) items else items.filter { it.catnum in catnumSet }
        }
    }

    private val itemsWithQuery = currentQuery.flatMapLatest { query ->
        itemsWithModes.map { items ->
            filterByQuery(items, query).sortedWith(
                compareByDescending<SatItem> { it.isSelected }
                    .thenBy { it.name }
                    .thenBy { it.catnum }
            )
        }
    }

    override fun getCurrentModes() = settingsRepo.selectedSatModes.value

    override fun getModesList() = Sources.satelliteModes

    override suspend fun getEntriesFlow() = withContext(dispatcher) {
        val selectedIds = settingsRepo.selectedIds.value.toHashSet()
        currentItems.value = localSource.getEntriesList().map { item ->
            item.copy(isSelected = item.catnum in selectedIds)
        }
        return@withContext itemsWithQuery
    }

    override suspend fun setModes(modes: List<String>) {
        settingsRepo.setSelectedSatModes(modes)
    }

    override suspend fun setQuery(query: String) {
        currentQuery.value = query
    }

    override suspend fun setSelection(selectAll: Boolean) = withContext(dispatcher) {
        val visibleIds = itemsWithQuery.first().mapTo(HashSet()) { it.catnum }
        setSelection(visibleIds, selectAll)
    }

    override suspend fun setSelection(ids: List<Int>, isTicked: Boolean) = withContext(dispatcher) {
        val idSet = ids.toHashSet()
        currentItems.value = currentItems.value.map { item ->
            if (item.catnum in idSet) item.copy(isSelected = isTicked) else item
        }
    }

    override suspend fun saveSelection() = withContext(dispatcher) {
        val currentSelection = currentItems.value.filter { it.isSelected }.map { it.catnum }
        settingsRepo.setSelectedIds(currentSelection)
    }

    /**
     * Bulk selection using a pre-built Set for O(1) lookups.
     */
    private suspend fun setSelection(idSet: Set<Int>, isTicked: Boolean) = withContext(dispatcher) {
        currentItems.value = currentItems.value.map { item ->
            if (item.catnum in idSet) item.copy(isSelected = isTicked) else item
        }
    }

    /**
     * Filters items by query. Uses toIntOrNull() instead of exception-based flow,
     * and lowercases the query once up front instead of per-item.
     */
    private fun filterByQuery(items: List<SatItem>, query: String): List<SatItem> {
        if (query.isBlank()) return items
        val catnum = query.toIntOrNull()
        if (catnum != null) return items.filter { it.catnum == catnum }
        val lowerQuery = query.lowercase()
        return items.filter { it.name.lowercase().contains(lowerQuery) }
    }
}
