/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2021 Arty Bishop (bishop.arty@gmail.com)
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

import android.content.ContentResolver
import android.net.Uri
import android.widget.SearchView
import androidx.lifecycle.*
import com.rtbishop.look4sat.data.Preferences
import com.rtbishop.look4sat.data.SatelliteRepo
import com.rtbishop.look4sat.data.SatItem
import com.rtbishop.look4sat.framework.model.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class EntriesViewModel @Inject constructor(
    private val preferences: Preferences,
    private val resolver: ContentResolver,
    private val satelliteRepo: SatelliteRepo,
) : ViewModel(), EntriesAdapter.EntriesClickListener, SearchView.OnQueryTextListener {

    private val transModes = MutableLiveData(satelliteRepo.loadSelectedModes())
    private val currentQuery = MutableLiveData(String())
    private val itemsWithModes = transModes.switchMap { modes ->
        liveData { satelliteRepo.getSatItems().collect { emit(filterByModes(it, modes)) } }
    }
    private val itemsWithQuery = currentQuery.switchMap { query ->
        itemsWithModes.map { items -> Result.Success(filterByQuery(items, query)) }
    }
    private val _satData = MediatorLiveData<Result<List<SatItem>>>().apply {
        addSource(itemsWithQuery) { value -> this.value = value }
    }
    private var shouldSelectAll = true
    val satData: LiveData<Result<List<SatItem>>> = _satData

    fun updateEntriesFromFile(uri: Uri) {
        viewModelScope.launch {
            _satData.value = Result.InProgress
            runCatching {
                resolver.openInputStream(uri)?.use { stream ->
                    satelliteRepo.updateEntriesFromFile(stream)
                }
            }.onFailure { _satData.value = Result.Error(it) }
        }
    }

    fun updateEntriesFromWeb(sources: List<String>) {
        viewModelScope.launch {
            _satData.value = Result.InProgress
            try {
                preferences.saveTleSources(sources)
                satelliteRepo.updateEntriesFromWeb(sources)
            } catch (exception: Exception) {
                _satData.value = Result.Error(exception)
            }
        }
    }

    fun selectCurrentItems() {
        val currentValue = _satData.value
        if (currentValue is Result.Success) {
            updateSelection(currentValue.data.map { it.catNum }, shouldSelectAll)
            shouldSelectAll = shouldSelectAll.not()
        }
    }

    fun loadSelectedModes(): List<String> {
        return satelliteRepo.loadSelectedModes()
    }

    fun saveSelectedModes(modes: List<String>) {
        transModes.value = modes
        satelliteRepo.saveSelectedModes(modes)
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        return true
    }

    override fun onQueryTextChange(newText: String): Boolean {
        currentQuery.value = newText
        return true
    }

    override fun updateSelection(catNums: List<Int>, isSelected: Boolean) {
        viewModelScope.launch { satelliteRepo.updateEntriesSelection(catNums, isSelected) }
    }

    private fun filterByModes(items: List<SatItem>, modes: List<String>): List<SatItem> {
        if (modes.isEmpty()) return items
        return items.filter { item -> item.modes.any { mode -> mode in modes } }
    }

    private fun filterByQuery(items: List<SatItem>, query: String): List<SatItem> {
        if (query.isBlank()) return items
        return try {
            items.filter { it.catNum == query.toInt() }
        } catch (e: Exception) {
            items.filter { item ->
                val itemName = item.name.lowercase(Locale.getDefault())
                itemName.contains(query.lowercase(Locale.getDefault()))
            }
        }
    }
}
