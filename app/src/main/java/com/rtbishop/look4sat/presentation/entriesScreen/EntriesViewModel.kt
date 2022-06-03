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

import androidx.lifecycle.*
import com.rtbishop.look4sat.domain.IDataRepository
import com.rtbishop.look4sat.domain.ISettingsManager
import com.rtbishop.look4sat.domain.model.DataState
import com.rtbishop.look4sat.domain.model.SatItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import java.util.*
import javax.inject.Inject

@HiltViewModel
class EntriesViewModel @Inject constructor(
    private val repository: IDataRepository,
    private val settings: ISettingsManager
) : ViewModel(), EntriesAdapter.EntriesClickListener {

    private val satType = MutableLiveData(String())
    private val transModes = MutableLiveData(settings.loadModesSelection())
    private val currentQuery = MutableLiveData(String())
    private val itemsFromRepo = liveData {
        delay(125)
        emit(loadEntriesWithSelection())
    } as MutableLiveData
    private val itemsWithType = satType.switchMap { type ->
        itemsFromRepo.map { items -> filterByType(items, type) }
    }
    private val itemsWithModes = transModes.switchMap { modes ->
        itemsWithType.map { items -> filterByModes(items, modes) }
    }
    private val itemsWithQuery = currentQuery.switchMap { query ->
        itemsWithModes.map { items -> filterByQuery(items, query) }
    }
    val satData = itemsWithQuery.map { items -> DataState.Success(items) }
    val satTypes: List<String> = settings.sourcesMap.keys.sorted()

    fun getSatType(): String? = satType.value

    fun setSatType(type: String) {
        satType.value = type
    }

    fun selectCurrentItems(selectAll: Boolean) {
        itemsWithQuery.value?.let { itemsWithQuery ->
            updateSelection(itemsWithQuery.map { item -> item.catnum }, selectAll)
        }
    }

    fun saveSelectedModes(modes: List<String>) {
        transModes.value = modes
        settings.saveModesSelection(modes)
    }

    fun setQuery(query: String) {
        currentQuery.value = query
    }

    fun saveSelection(): List<Int> {
        return itemsFromRepo.value?.let { itemsAll ->
            itemsAll.filter { item -> item.isSelected }.map { item -> item.catnum }
        } ?: emptyList()
    }

    override fun updateSelection(catNums: List<Int>, isSelected: Boolean) {
        itemsFromRepo.value?.let { itemsAll ->
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

    private fun filterByModes(items: List<SatItem>, modes: List<String>): List<SatItem> {
        if (modes.isEmpty()) return items
        return items.filter { item -> item.modes.any { mode -> mode in modes } }
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
