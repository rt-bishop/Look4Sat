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
            showDeepSpace = settingsRepo.passesSettings.value.showDeepSpace,
            modes = settingsRepo.passesSettings.value.selectedModes,
            shouldSeeWhatsNew = settingsRepo.otherSettings.value.shouldSeeWhatsNew
        )
    )
    val uiState: StateFlow<PassesState> = _uiState

    init {
        // Refresh indicator: mirrors the repo's isCalculating state
        viewModelScope.launch {
            satelliteRepo.isCalculating.collect { calculating ->
                _uiState.update { it.copy(isRefreshing = calculating) }
            }
        }
        // Local tick loop — computes pass progress and countdown timer every second
        viewModelScope.launch {
            while (isActive) {
                val timeNow = System.currentTimeMillis()
                val showDeepSpace = _uiState.value.showDeepSpace
                val allPasses = satelliteRepo.passes.value
                val filtered = if (showDeepSpace) allPasses else allPasses.filter { !it.isDeepSpace }
                val processed = computePassProgress(filtered, timeNow)
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
            is PassesAction.FilterPasses ->
                applyFilter(action.hoursAhead, action.minElevation, action.showDeepSpace, _uiState.value.modes)

            is PassesAction.FilterRadios ->
                applyFilter(_uiState.value.hours, _uiState.value.elevation, _uiState.value.showDeepSpace, action.modes)
            PassesAction.RefreshPasses -> refreshPasses()
            PassesAction.TogglePassesDialog ->
                _uiState.update { it.copy(isPassesDialogShown = !it.isPassesDialogShown) }

            PassesAction.ToggleRadiosDialog ->
                _uiState.update { it.copy(isRadiosDialogShown = !it.isRadiosDialogShown) }
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

    private fun applyFilter(
        hoursAhead: Int,
        minElevation: Double,
        showDeepSpace: Boolean,
        modes: List<String>
    ) = viewModelScope.launch {
        settingsRepo.setPassesSettings(PassesSettings(showDeepSpace, hoursAhead, minElevation, modes))
        _uiState.update {
            it.copy(hours = hoursAhead, elevation = minElevation, showDeepSpace = showDeepSpace, modes = modes)
        }
        if (showDeepSpace) satelliteRepo.calculatePasses(System.currentTimeMillis(), hoursAhead, minElevation, modes)
    }

    private fun refreshPasses() = viewModelScope.launch {
        val (_, hoursAhead, minElevation, modes) = settingsRepo.passesSettings.value
        satelliteRepo.calculatePasses(System.currentTimeMillis(), hoursAhead, minElevation, modes)
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
