/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2022 Arty Bishop (bishop.arty@gmail.com)
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
package com.rtbishop.look4sat.presentation.entriesScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rtbishop.look4sat.domain.IDataRepository
import com.rtbishop.look4sat.domain.ISatelliteManager
import com.rtbishop.look4sat.domain.ISettingsManager
import com.rtbishop.look4sat.domain.model.DataState
import com.rtbishop.look4sat.domain.model.SatItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EntriesViewModel @Inject constructor(
    private val satelliteManager: ISatelliteManager,
    private val repository: IDataRepository,
    private val settings: ISettingsManager
) : ViewModel() {

    private val satType = MutableStateFlow("All")
    private val currentQuery = MutableStateFlow(String())
    private val itemsFromRepo = MutableStateFlow<List<SatItem>>(emptyList())
    private val itemsWithType = satType.flatMapLatest { type ->
        itemsFromRepo.map { items -> filterByType(items, type) }
    }
    private val itemsWithQuery = currentQuery.flatMapLatest { query ->
        itemsWithType.map { items -> filterByQuery(items, query) }
    }
    val satData = itemsWithQuery.map { items -> DataState.Success(items) }
    val satTypes: List<String> = settings.sourcesMap.keys.sorted()

    init {
        viewModelScope.launch {
            delay(250)
            itemsFromRepo.value = loadEntriesWithSelection()
        }
    }

    fun getSatType() = satType.value

    fun setSatType(type: String) {
        satType.value = type
    }

    fun selectCurrentItems(selectAll: Boolean) = viewModelScope.launch {
        updateSelection(itemsWithQuery.first().map { item -> item.catnum }, selectAll)
    }

    fun setQuery(query: String) {
        currentQuery.value = query
    }

    fun saveSelection() = viewModelScope.launch {
        val newSelection = itemsFromRepo.value.filter { it.isSelected }.map { it.catnum }
        settings.saveEntriesSelection(newSelection)
        val selectedIds = settings.loadEntriesSelection()
        val satellites = repository.getEntriesWithIds(selectedIds)
        val stationPos = settings.loadStationPosition()
        val timeRef = System.currentTimeMillis()
        val hoursAhead = settings.getHoursAhead()
        val minElev = settings.getMinElevation()
        satelliteManager.calculatePasses(satellites, stationPos, timeRef, hoursAhead, minElev)
    }

    fun updateSelection(catNums: List<Int>, isSelected: Boolean) {
        itemsFromRepo.value.let { itemsAll ->
            val copiedList = itemsAll.map { item -> item.copy() }
            catNums.forEach { catnum ->
                copiedList.find { item -> item.catnum == catnum }?.isSelected = isSelected
            }
            itemsFromRepo.value = copiedList
        }
    }

    private suspend fun loadEntriesWithSelection(): List<SatItem> {
        val selectedIds = settings.loadEntriesSelection()
        return repository.getEntriesWithModes().onEach { it.isSelected = it.catnum in selectedIds }
    }

    private fun filterByType(items: List<SatItem>, type: String): List<SatItem> {
        if (type == "All") return items
        val catnums = settings.loadSatType(type)
        if (catnums.isEmpty()) return items
        return items.filter { item -> item.catnum in catnums }
    }

    private fun filterByQuery(items: List<SatItem>, query: String): List<SatItem> {
        if (query.isBlank()) return items
        return try {
            items.filter { it.catnum == query.toInt() }
        } catch (e: Exception) {
            items.filter { item ->
                val itemName = item.name.lowercase(Locale.getDefault())
                itemName.contains(query.lowercase(Locale.getDefault()))
            }
        }
    }
}
