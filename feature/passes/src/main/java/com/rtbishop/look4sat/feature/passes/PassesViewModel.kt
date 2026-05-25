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
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rtbishop.look4sat.core.domain.model.PassesSettings
import com.rtbishop.look4sat.core.domain.predict.CelestialComputer
import com.rtbishop.look4sat.core.domain.predict.OrbitalPass
import com.rtbishop.look4sat.core.domain.repository.IMainContainer
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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
        // React to settings changes: update UTC flag and whatsNew
        viewModelScope.launch {
            settingsRepo.otherSettings.collectLatest { settings ->
                _uiState.update { it.copy(isUtc = settings.stateOfUtc, shouldSeeWhatsNew = settings.shouldSeeWhatsNew) }
            }
        }
        // Main tick loop: reacts to new passes, then ticks every second
        viewModelScope.launch {
            satelliteRepo.passes.collectLatest { allPasses ->
                while (isActive) {
                    val timeNow = System.currentTimeMillis()
                    val isUtc = _uiState.value.isUtc
                    val showDeepSpace = _uiState.value.showDeepSpace
                    val filtered = allPasses
                        .let { if (showDeepSpace) it else it.filter { pass -> !pass.isDeepSpace } }
                    val processed = computePassProgress(filtered, timeNow)
                    val (nextPass, nextTime, isAos) = resolveNextPass(processed, timeNow)
                    val sunTimes = computeSunTimes(processed, isUtc)
                    val grouped = groupPasses(processed, isUtc)
                    _uiState.update {
                        it.copy(
                            itemsList = processed,
                            groupedPasses = grouped,
                            sunTimes = sunTimes,
                            nextPass = nextPass,
                            nextTime = nextTime,
                            isNextTimeAos = isAos
                        )
                    }
                    delay(1000)
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
            is PassesAction.FocusCatNum -> _uiState.update { it.copy(focusedCatNum = action.catNum) }
            PassesAction.ClearFocus -> _uiState.update { it.copy(focusedCatNum = null) }
        }
    }

    // Computes sunrise/sunset strings for each unique calendar day in the pass list, plus today for DeepSpace
    private fun computeSunTimes(passes: List<OrbitalPass>, isUtc: Boolean): Map<String, Pair<String, String>> {
        val stationPos = settingsRepo.stationPosition.value
        val tz = if (isUtc) TimeZone.getTimeZone("UTC") else TimeZone.getDefault()
        val sdfDate = SimpleDateFormat("EEE, dd MMM yyyy", Locale.ENGLISH).also { it.timeZone = tz }
        val sdfTime = SimpleDateFormat("HH:mm", Locale.ENGLISH).also { it.timeZone = tz }
        val result = LinkedHashMap<String, Pair<String, String>>()
        // DeepSpace group always shows today's sun times
        if (passes.any { it.isDeepSpace }) {
            val riseSet = CelestialComputer.findSunRiseSet(stationPos, System.currentTimeMillis())
            val rise = if (riseSet.riseTimeMillis > 0) sdfTime.format(Date(riseSet.riseTimeMillis)) else "--:--"
            val set = if (riseSet.setTimeMillis > 0) sdfTime.format(Date(riseSet.setTimeMillis)) else "--:--"
            result["DeepSpace (period >225min)"] = rise to set
        }
        for (pass in passes) {
            if (pass.isDeepSpace) continue
            val label = sdfDate.format(Date(pass.aosTime))
            if (label in result) continue
            val riseSet = CelestialComputer.findSunRiseSet(stationPos, pass.aosTime)
            val rise = if (riseSet.riseTimeMillis > 0) sdfTime.format(Date(riseSet.riseTimeMillis)) else "--:--"
            val set = if (riseSet.setTimeMillis > 0) sdfTime.format(Date(riseSet.setTimeMillis)) else "--:--"
            result[label] = rise to set
        }
        return result
    }

    private fun groupPasses(passes: List<OrbitalPass>, isUtc: Boolean): Map<String, List<OrbitalPass>> {
        val tz = if (isUtc) TimeZone.getTimeZone("UTC") else TimeZone.getDefault()
        val sdfDate = SimpleDateFormat("EEE, dd MMM yyyy", Locale.ENGLISH).also { it.timeZone = tz }
        val ordered = LinkedHashMap<String, List<OrbitalPass>>()
        val deepSpace = passes.filter { it.isDeepSpace }
        if (deepSpace.isNotEmpty()) ordered["DeepSpace (period >225min)"] = deepSpace
        passes.filter { !it.isDeepSpace }
            .groupByTo(LinkedHashMap()) { sdfDate.format(Date(it.aosTime)) }
            .forEach { (k, v) -> ordered[k] = v }
        return ordered
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
        satelliteRepo.calculatePasses(System.currentTimeMillis(), hoursAhead, minElevation, modes)
    }

    private fun refreshPasses() = viewModelScope.launch {
        val (_, hoursAhead, minElevation, modes) = settingsRepo.passesSettings.value
        satelliteRepo.calculatePasses(System.currentTimeMillis(), hoursAhead, minElevation, modes)
    }

    companion object {
        fun factory(container: IMainContainer) = viewModelFactory {
            initializer {
                PassesViewModel(
                    satelliteRepo = container.satelliteRepo,
                    settingsRepo = container.settingsRepo
                )
            }
        }
    }
}
