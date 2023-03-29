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
package com.rtbishop.look4sat.presentation.entries

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rtbishop.look4sat.MainApplication
import com.rtbishop.look4sat.domain.ISelectionRepo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class EntriesViewModel(private val selectionRepo: ISelectionRepo) : ViewModel() {

    private val defaultUiState = EntriesUiState(
        isLoading = true,
        itemsList = emptyList(),
        currentType = selectionRepo.getCurrentType(),
        typesList = selectionRepo.getTypesList()
    )
    val uiState = mutableStateOf(defaultUiState)

    init {
        viewModelScope.launch {
            delay(1000)
            selectionRepo.getEntriesFlow().collect { items ->
                uiState.value = uiState.value.copy(isLoading = false, itemsList = items)
            }
        }
    }

    fun setType(type: String) = viewModelScope.launch {
        selectionRepo.setType(type)
        uiState.value = uiState.value.copy(currentType = type)
    }

    fun setQuery(query: String) = viewModelScope.launch { selectionRepo.setQuery(query) }

    fun setSelection(selectAll: Boolean) = viewModelScope.launch {
        selectionRepo.setSelection(selectAll)
    }

    fun setSelection(ids: List<Int>, isTicked: Boolean) = viewModelScope.launch {
        selectionRepo.setSelection(ids, isTicked)
    }

    fun saveSelection() = viewModelScope.launch { selectionRepo.saveSelection() }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            val applicationKey = ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY
            initializer {
                val container = (this[applicationKey] as MainApplication).container
                EntriesViewModel(container.selectionRepo)
            }
        }
    }
}
