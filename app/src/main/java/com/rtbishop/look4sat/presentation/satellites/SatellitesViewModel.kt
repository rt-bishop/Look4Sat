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
package com.rtbishop.look4sat.presentation.satellites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rtbishop.look4sat.MainApplication
import com.rtbishop.look4sat.domain.repository.ISelectionRepo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SatellitesViewModel(private val selectionRepo: ISelectionRepo) : ViewModel() {

    private val defaultType = "All"
    private val _uiState = MutableStateFlow(
        SatellitesState(
            isDialogShown = false,
            isLoading = true,
            itemsList = emptyList(),
            currentType = defaultType,
            typesList = selectionRepo.getTypesList(),
            takeAction = ::handleAction
        )
    )
    val uiState: StateFlow<SatellitesState> = _uiState

    init {
        viewModelScope.launch {
            delay(1000)
            selectionRepo.setType(defaultType)
            selectionRepo.getEntriesFlow().collect { items ->
                _uiState.value = _uiState.value.copy(isLoading = false, itemsList = items)
            }
        }
    }

    private fun handleAction(action: SatellitesAction) {
        when (action) {
            SatellitesAction.SaveSelection -> saveSelection()
            is SatellitesAction.SearchFor -> searchFor(action.query)
            SatellitesAction.SelectAll -> selectAll(true)
            is SatellitesAction.SelectSingle -> selectSingle(action.id, action.isTicked)
            is SatellitesAction.SelectType -> selectType(action.type)
            SatellitesAction.ToggleTypesDialog -> toggleTypesDialog()
            SatellitesAction.UnselectAll -> selectAll(false)
        }
    }

    private fun saveSelection() = viewModelScope.launch { selectionRepo.saveSelection() }

    private fun searchFor(query: String) = viewModelScope.launch { selectionRepo.setQuery(query) }

    private fun selectAll(selectAll: Boolean) = viewModelScope.launch {
        selectionRepo.setSelection(selectAll)
    }

    private fun selectSingle(id: Int, isTicked: Boolean) = viewModelScope.launch {
        selectionRepo.setSelection(listOf(id), isTicked.not())
    }

    private fun selectType(type: String) = viewModelScope.launch {
        selectionRepo.setType(type)
        _uiState.value = _uiState.value.copy(currentType = type, isDialogShown = false)
    }

    private fun toggleTypesDialog() {
        val currentDialogState = _uiState.value.isDialogShown
        _uiState.value = _uiState.value.copy(isDialogShown = currentDialogState.not())
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            val applicationKey = ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY
            initializer {
                val container = (this[applicationKey] as MainApplication).container
                SatellitesViewModel(container.selectionRepo)
            }
        }
    }
}
