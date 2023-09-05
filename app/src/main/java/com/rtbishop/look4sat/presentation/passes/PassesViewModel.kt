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
package com.rtbishop.look4sat.presentation.passes

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rtbishop.look4sat.MainApplication
import com.rtbishop.look4sat.domain.model.PassesSettings
import com.rtbishop.look4sat.domain.predict.SatPass
import com.rtbishop.look4sat.domain.repository.ISatelliteRepo
import com.rtbishop.look4sat.domain.repository.ISettingsRepo
import com.rtbishop.look4sat.domain.utility.toTimerString
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PassesViewModel(
    private val satelliteRepo: ISatelliteRepo, private val settingsRepo: ISettingsRepo
) : ViewModel() {

    private val _uiState = mutableStateOf(
        PassesState(
            isDialogShown = false,
            isRefreshing = true,
            isUtc = settingsRepo.otherSettings.value.stateOfUtc,
            nextId = 0,
            nextName = "...",
            nextTime = "00:00:00",
            hours = settingsRepo.passesSettings.value.hoursAhead,
            elevation = settingsRepo.passesSettings.value.minElevation,
            modes = settingsRepo.passesSettings.value.selectedModes,
            itemsList = emptyList(),
            takeAction = ::handleAction
        )
    )
    private var processing: Job? = null
    val uiState: State<PassesState> = _uiState

    init {
        viewModelScope.launch { satelliteRepo.initRepository() }
        viewModelScope.launch {
            satelliteRepo.passes.collect { passes ->
                processing?.cancelAndJoin()
                processing = viewModelScope.launch {
                    while (isActive) {
                        val timeNow = System.currentTimeMillis()
                        val newPasses = satelliteRepo.processPasses(passes, timeNow)
                        setPassInfo(newPasses, timeNow)
                        _uiState.value = _uiState.value.copy(isRefreshing = false, itemsList = newPasses)
                        delay(1000)
                    }
                }
            }
        }
    }

    private fun handleAction(action: PassesAction) {
        when (action) {
            is PassesAction.ApplyFilter -> applyFilter(action.hoursAhead, action.minElevation, action.modes)
            PassesAction.RefreshPasses -> refreshPasses()
            PassesAction.ToggleFilterDialog -> toggleFilterDialog()
        }
    }

    private fun applyFilter(hoursAhead: Int, minElevation: Double, modes: List<String>) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        processing?.cancelAndJoin()
        settingsRepo.setPassesSettings(PassesSettings(hoursAhead, minElevation, modes))
        _uiState.value = _uiState.value.copy(hours = hoursAhead, elevation = minElevation, modes = modes)
        satelliteRepo.calculatePasses(System.currentTimeMillis(), hoursAhead, minElevation)
    }

    private fun refreshPasses() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        processing?.cancelAndJoin()
        val (hoursAhead, minElevation) = settingsRepo.passesSettings.value
        satelliteRepo.calculatePasses(System.currentTimeMillis(), hoursAhead, minElevation)
    }

    private fun setPassInfo(passes: List<SatPass>, timeNow: Long) {
        if (passes.isEmpty()) return
        try {
            val nextPass = passes.first { it.aosTime.minus(timeNow) > 0 }
            val time = nextPass.aosTime.minus(timeNow).toTimerString()
            _uiState.value = _uiState.value.copy(nextId = nextPass.catNum, nextName = nextPass.name, nextTime = time)
        } catch (exception: NoSuchElementException) {
            val lastPass = passes.last()
            val time = lastPass.losTime.minus(timeNow).toTimerString()
            _uiState.value = _uiState.value.copy(nextId = lastPass.catNum, nextName = lastPass.name, nextTime = time)
        }
    }

    private fun toggleFilterDialog() {
        val currentDialogState = _uiState.value.isDialogShown
        _uiState.value = _uiState.value.copy(isDialogShown = currentDialogState.not())
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            val applicationKey = ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY
            initializer {
                val container = (this[applicationKey] as MainApplication).container
                PassesViewModel(container.satelliteRepo, container.settingsRepo)
            }
        }
    }
}
