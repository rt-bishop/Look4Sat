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
package com.rtbishop.look4sat.presentation.passes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rtbishop.look4sat.MainApplication
import com.rtbishop.look4sat.domain.model.PassesSettings
import com.rtbishop.look4sat.domain.predict.OrbitalPass
import com.rtbishop.look4sat.domain.repository.ISatelliteRepo
import com.rtbishop.look4sat.domain.repository.ISettingsRepo
import com.rtbishop.look4sat.domain.utility.toTimerString
import com.rtbishop.look4sat.presentation.common.getDefaultPass
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PassesViewModel(
    private val satelliteRepo: ISatelliteRepo, private val settingsRepo: ISettingsRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PassesState(
            isPassesDialogShown = false,
            isRadiosDialogShown = false,
            isRefreshing = true,
            isUtc = settingsRepo.otherSettings.value.stateOfUtc,
            nextTime = "00:00:00",
            isNextTimeAos = true,
            nextPass = getDefaultPass(),
            hours = settingsRepo.passesSettings.value.hoursAhead,
            elevation = settingsRepo.passesSettings.value.minElevation,
            modes = settingsRepo.passesSettings.value.selectedModes,
            itemsList = emptyList(),
            shouldSeeWelcome = settingsRepo.otherSettings.value.shouldSeeWelcome,
            takeAction = ::handleAction
        )
    )
    private var processing: Job? = null
    val uiState: StateFlow<PassesState> = _uiState

    init {
        viewModelScope.launch {
            delay(1000)
            satelliteRepo.passes.collectLatest { passes ->
                processing?.cancelAndJoin()
                processing = viewModelScope.launch {
                    while (isActive) {
                        val timeNow = System.currentTimeMillis()
                        val newPasses = satelliteRepo.processPasses(passes, timeNow)
                        setPassInfo(newPasses, timeNow)
                        _uiState.update { it.copy(isRefreshing = false, itemsList = newPasses) }
                        delay(1000)
                    }
                }
            }
        }
        viewModelScope.launch {
            settingsRepo.otherSettings.collectLatest { settings ->
                _uiState.update { it.copy(shouldSeeWelcome = settings.shouldSeeWelcome) }
            }
        }
    }

    private fun handleAction(action: PassesAction) {
        when (action) {
            PassesAction.DismissWelcome -> settingsRepo.setWelcomeDismissed()
            is PassesAction.FilterPasses -> applyFilter(action.hoursAhead, action.minElevation, uiState.value.modes)
            is PassesAction.FilterRadios -> applyFilter(uiState.value.hours, uiState.value.elevation, action.modes)
            PassesAction.RefreshPasses -> refreshPasses()
            PassesAction.TogglePassesDialog -> toggleFilterDialog()
            PassesAction.ToggleRadiosDialog -> toggleRadiosDialog()
        }
    }

    private fun applyFilter(hoursAhead: Int, minElevation: Double, modes: List<String>) = viewModelScope.launch {
        _uiState.update { it.copy(isRefreshing = true) }
        processing?.cancelAndJoin()
        settingsRepo.setPassesSettings(PassesSettings(hoursAhead, minElevation, modes))
        _uiState.update { it.copy(hours = hoursAhead, elevation = minElevation, modes = modes) }
        satelliteRepo.calculatePasses(System.currentTimeMillis(), hoursAhead, minElevation, modes)
    }

    private fun refreshPasses() = viewModelScope.launch {
        _uiState.update { it.copy(isRefreshing = true) }
        processing?.cancelAndJoin()
        val (hoursAhead, minElevation, modes) = settingsRepo.passesSettings.value
        satelliteRepo.calculatePasses(System.currentTimeMillis(), hoursAhead, minElevation, modes)
    }

    private fun setPassInfo(passes: List<OrbitalPass>, timeNow: Long) {
        if (passes.isEmpty()) return
        try {
            val nextPass = passes.first { it.aosTime.minus(timeNow) > 0 }
            val time = nextPass.aosTime.minus(timeNow).toTimerString()
            _uiState.update { it.copy(nextPass = nextPass, nextTime = time, isNextTimeAos = true) }
        } catch (_: NoSuchElementException) {
            val lastPass = passes.last()
            val time = lastPass.losTime.minus(timeNow).toTimerString()
            _uiState.update { it.copy(nextPass = lastPass, nextTime = time, isNextTimeAos = false) }
        }
    }

    private fun toggleFilterDialog() {
        val currentDialogState = _uiState.value.isPassesDialogShown
        _uiState.update { it.copy(isPassesDialogShown = currentDialogState.not()) }
    }

    private fun toggleRadiosDialog() {
        val currentDialogState = _uiState.value.isRadiosDialogShown
        _uiState.update { it.copy(isRadiosDialogShown = currentDialogState.not()) }
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
