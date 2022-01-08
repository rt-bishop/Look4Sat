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
import com.rtbishop.look4sat.domain.DataRepository
import com.rtbishop.look4sat.domain.model.DataState
import com.rtbishop.look4sat.domain.model.SatItem
import com.rtbishop.look4sat.framework.SettingsProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import javax.inject.Inject

@HiltViewModel
class EntriesViewModel @Inject constructor(
    private val preferences: SettingsProvider,
    private val resolver: ContentResolver,
    private val dataRepository: DataRepository
) : ViewModel(), SearchView.OnQueryTextListener, EntriesAdapter.EntriesClickListener {

    private val coroutineHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.d(throwable)
        _satData.value = DataState.Error(null)
    }
    private val transModes = MutableLiveData(preferences.loadModesSelection())
    private val currentQuery = MutableLiveData(String())
    private val itemsWithModes = transModes.switchMap { modes ->
        liveData { dataRepository.getSatelliteItems().collect { emit(filterByModes(it, modes)) } }
    }
    private val itemsWithQuery = currentQuery.switchMap { query ->
        itemsWithModes.map { items -> DataState.Success(filterByQuery(items, query)) }
    }
    private val _satData = MediatorLiveData<DataState<List<SatItem>>>().apply {
        addSource(itemsWithQuery) { value -> this.value = value }
    }
    private var shouldSelectAll = true
    val satData: LiveData<DataState<List<SatItem>>> = _satData

    fun updateDataFromFile(uri: Uri) {
        viewModelScope.launch(coroutineHandler) {
            _satData.value = DataState.Loading
            runCatching {
                resolver.openInputStream(uri)?.use { stream ->
                    dataRepository.updateDataFromFile(stream)
                }
            }.onFailure { _satData.value = DataState.Error(null) }
        }
    }

    fun updateDataFromWeb(sources: List<String>) {
        viewModelScope.launch(coroutineHandler) {
            _satData.value = DataState.Loading
            dataRepository.updateDataFromWeb(sources)
        }
    }

    fun selectCurrentItems() {
        val currentValue = _satData.value
        if (currentValue is DataState.Success) {
            updateSelection(currentValue.data.map { it.catnum }, shouldSelectAll)
            shouldSelectAll = shouldSelectAll.not()
        }
    }

    fun loadSelectedModes(): List<String> {
        return preferences.loadModesSelection()
    }

    fun saveSelectedModes(modes: List<String>) {
        transModes.value = modes
        preferences.saveModesSelection(modes)
    }

    fun setQuery(query: String) {
        currentQuery.value = query
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        return true
    }

    override fun onQueryTextChange(newText: String): Boolean {
        currentQuery.value = newText
        return true
    }

    override fun updateSelection(catNums: List<Int>, isSelected: Boolean) {
        viewModelScope.launch { dataRepository.updateSelection(catNums, isSelected) }
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
