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
package com.rtbishop.look4sat.feature.passes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rtbishop.look4sat.core.domain.model.PassesSettings
import com.rtbishop.look4sat.core.domain.predict.OrbitalPass
import com.rtbishop.look4sat.core.domain.repository.IContainerProvider
import com.rtbishop.look4sat.core.domain.repository.ISatelliteRepo
import com.rtbishop.look4sat.core.domain.repository.ISettingsRepo
import com.rtbishop.look4sat.core.domain.utility.round
import com.rtbishop.look4sat.core.domain.utility.toTimerString
import com.rtbishop.look4sat.core.presentation.getDefaultPass
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PassesViewModel(
    private val satelliteRepo: ISatelliteRepo,
    private val settingsRepo: ISettingsRepo
) : ViewModel() {

    private val defaultPass = getDefaultPass()
    private val _uiState = MutableStateFlow(
        PassesState(
            isUtc = settingsRepo.otherSettings.value.stateOfUtc,
            nextPass = defaultPass,
            hours = settingsRepo.passesSettings.value.hoursAhead,
            elevation = settingsRepo.passesSettings.value.minElevation,
            modes = settingsRepo.passesSettings.value.selectedModes,
            shouldSeeWhatsNew = settingsRepo.otherSettings.value.shouldSeeWhatsNew
        )
    )
    val uiState: StateFlow<PassesState> = _uiState

    init {
        // Show refreshing indicator whenever the selected satellites list changes
        viewModelScope.launch {
            settingsRepo.selectedIds.collectLatest { selectedIds ->
                _uiState.update { it.copy(isRefreshing = true) }
                if (selectedIds.isEmpty()) {
                    // No satellites selected — show indicator briefly, then stop
                    delay(1000)
                    _uiState.update { it.copy(isRefreshing = false) }
                }
                // For non-empty selections, the passes collector below will clear isRefreshing
            }
        }
        // Stop refreshing whenever new passes arrive (from initial load, selection change, or filter)
        viewModelScope.launch {
            satelliteRepo.passes.drop(1).collect { _ ->
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
        // Local tick loop — computes pass progress and countdown timer every second
        viewModelScope.launch {
            while (isActive) {
                val timeNow = System.currentTimeMillis()
                val processed = computePassProgress(satelliteRepo.passes.value, timeNow)
                val (nextPass, nextTime, isAos) = resolveNextPass(processed, timeNow)
                _uiState.update {
                    it.copy(
                        itemsList = processed,
                        nextPass = nextPass,
                        nextTime = nextTime,
                        isNextTimeAos = isAos
                    )
                }
                delay(1000)
            }
        }
        viewModelScope.launch {
            settingsRepo.otherSettings.collectLatest { settings ->
                _uiState.update {
                    it.copy(
                        isUtc = settings.stateOfUtc,
                        shouldSeeWhatsNew = settings.shouldSeeWhatsNew
                    )
                }
            }
        }
    }

    fun onAction(action: PassesAction) {
        when (action) {
            PassesAction.DismissWhatsNew -> settingsRepo.setWhatsNewDismissed()
            is PassesAction.FilterPasses -> applyFilter(action.hoursAhead, action.minElevation, _uiState.value.modes)
            is PassesAction.FilterRadios -> applyFilter(_uiState.value.hours, _uiState.value.elevation, action.modes)
            PassesAction.RefreshPasses -> refreshPasses()
            PassesAction.TogglePassesDialog -> _uiState.update { it.copy(isPassesDialogShown = !it.isPassesDialogShown) }
            PassesAction.ToggleRadiosDialog -> _uiState.update { it.copy(isRadiosDialogShown = !it.isRadiosDialogShown) }
        }
    }

    /** Computes live progress for each pass, filtering out expired ones. */
    private fun computePassProgress(passList: List<OrbitalPass>, time: Long): List<OrbitalPass> {
        val result = ArrayList<OrbitalPass>(passList.size)
        for (pass in passList) {
            if (!pass.isDeepSpace && time > pass.aosTime) {
                val deltaNow = time.minus(pass.aosTime).toFloat()
                val deltaTotal = pass.losTime.minus(pass.aosTime).toFloat()
                val newProgress = (deltaNow / deltaTotal).round(2)
                if (newProgress >= 1.0f) continue
                if (newProgress != pass.progress) {
                    result.add(pass.copy(progress = newProgress))
                } else {
                    result.add(pass)
                }
            } else {
                result.add(pass)
            }
        }
        return result
    }

    /** Resolves the next upcoming or active pass and its countdown timer. */
    private fun resolveNextPass(
        passes: List<OrbitalPass>,
        timeNow: Long
    ): Triple<OrbitalPass, String, Boolean> {
        val upcoming = passes.firstOrNull { it.aosTime > timeNow }
        if (upcoming != null) {
            return Triple(upcoming, (upcoming.aosTime - timeNow).toTimerString(), true)
        }
        if (passes.isNotEmpty()) {
            val lastPass = passes.last()
            return Triple(lastPass, (lastPass.losTime - timeNow).toTimerString(), false)
        }
        return Triple(defaultPass, "00:00:00", true)
    }

    private fun applyFilter(hoursAhead: Int, minElevation: Double, modes: List<String>) = viewModelScope.launch {
        _uiState.update { it.copy(isRefreshing = true) }
        settingsRepo.setPassesSettings(PassesSettings(hoursAhead, minElevation, modes))
        _uiState.update { it.copy(hours = hoursAhead, elevation = minElevation, modes = modes) }
        satelliteRepo.calculatePasses(System.currentTimeMillis(), hoursAhead, minElevation, modes)
        _uiState.update { it.copy(isRefreshing = false) }
    }

    private fun refreshPasses() = viewModelScope.launch {
        _uiState.update { it.copy(isRefreshing = true) }
        val (hoursAhead, minElevation, modes) = settingsRepo.passesSettings.value
        satelliteRepo.calculatePasses(System.currentTimeMillis(), hoursAhead, minElevation, modes)
        _uiState.update { it.copy(isRefreshing = false) }
    }


    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            val applicationKey = ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY
            initializer {
                val container = (this[applicationKey] as IContainerProvider).getMainContainer()
                PassesViewModel(container.satelliteRepo, container.settingsRepo)
            }
        }
    }
}
