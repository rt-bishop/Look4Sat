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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.time.Duration.Companion.milliseconds

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
            lowElevation = settingsRepo.otherSettings.value.lowElevation,
            highElevation = settingsRepo.otherSettings.value.highElevation,
            aosStartMinute = settingsRepo.passesSettings.value.aosStartMinute,
            aosEndMinute = settingsRepo.passesSettings.value.aosEndMinute,
            invertAosTimeWindow = settingsRepo.passesSettings.value.invertAosTimeWindow,
            showDeepSpace = settingsRepo.passesSettings.value.showDeepSpace,
            modes = settingsRepo.selectedSatModes.value,
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
                _uiState.update {
                    it.copy(
                        isUtc = settings.stateOfUtc,
                        shouldSeeWhatsNew = settings.shouldSeeWhatsNew,
                        lowElevation = settings.lowElevation,
                        highElevation = settings.highElevation
                    )
                }
            }
        }
        viewModelScope.launch {
            settingsRepo.selectedSatModes.collectLatest { modes ->
                _uiState.update { it.copy(modes = modes) }
            }
        }
        // Tick loop: restarts on passes change, UTC/DeepSpace changes. Grouping/sun-time
        // computations run once per restart, progress/countdown are calculated every second.
        viewModelScope.launch {
            combine(
                satelliteRepo.passes,
                settingsRepo.otherSettings.map { it.stateOfUtc }.distinctUntilChanged(),
                settingsRepo.passesSettings.map { it.showDeepSpace }.distinctUntilChanged()
            ) { passes, isUtc, showDeepSpace -> Triple(passes, isUtc, showDeepSpace) }
                .collectLatest { (allPasses, isUtc, showDeepSpace) ->
                    val filtered = if (showDeepSpace) allPasses
                    else allPasses.filter { !it.isDeepSpace }
                    // Expensive: recompute sun times once per items/UTC change, not every second
                    val sunTimes = computeSunTimes(filtered, isUtc)
                    _uiState.update { it.copy(sunTimes = sunTimes) }
                    while (isActive) {
                        val timeNow = System.currentTimeMillis()
                        val processed = computePassProgress(filtered, timeNow)
                        val grouped = groupPasses(processed, isUtc)
                        val (nextPass, nextTime, isAos) = resolveNextPass(processed, timeNow)
                        _uiState.update {
                            it.copy(
                                itemsList = processed,
                                groupedPasses = grouped,
                                nextPass = nextPass,
                                nextTime = nextTime,
                                isNextTimeAos = isAos
                            )
                        }
                        delay(1000.milliseconds)
                    }
                }
        }
    }

    fun onAction(action: PassesAction) {
        when (action) {
            PassesAction.DismissWhatsNew -> settingsRepo.setWhatsNewDismissed()
            is PassesAction.FilterPasses ->
                applyFilter(
                    hoursAhead = action.hoursAhead,
                    minElevation = action.minElevation,
                    lowElevation = action.lowElevation,
                    highElevation = action.highElevation,
                    aosStartMinute = action.aosStartMinute,
                    aosEndMinute = action.aosEndMinute,
                    invertAosTimeWindow = action.invertAosTimeWindow,
                    showDeepSpace = action.showDeepSpace
                )
            is PassesAction.FilterRadios -> setModesFilter(action.modes)
            PassesAction.RefreshPasses -> refreshPasses()
            PassesAction.TogglePassesDialog ->
                _uiState.update { it.copy(isPassesDialogShown = !it.isPassesDialogShown) }
            PassesAction.ToggleRadiosDialog ->
                _uiState.update { it.copy(isRadiosDialogShown = !it.isRadiosDialogShown) }
            is PassesAction.FocusCatNum -> _uiState.update { it.copy(focusedCatNum = action.catNum) }
            PassesAction.ClearFocus -> _uiState.update { it.copy(focusedCatNum = null) }
        }
    }

    private fun displayLocale(): Locale {
        val locale = Locale.getDefault()
        return if (locale.language == Locale.CHINESE.language) locale else Locale.ENGLISH
    }

    /** Returns the visible pass-date format. Keep non-Chinese locales identical to upstream. */
    private fun dateFormat(tz: TimeZone): SimpleDateFormat {
        val locale = displayLocale()
        val pattern = if (locale.language == Locale.CHINESE.language) {
            "yyyy'年'M'月'd'日' EEEE"
        } else {
            "EEE, dd MMM yyyy"
        }
        return SimpleDateFormat(pattern, locale).also { it.timeZone = tz }
    }

    // Computes sunrise/sunset strings for each unique calendar day in the pass list, plus today for DeepSpace
    private fun computeSunTimes(passes: List<OrbitalPass>, isUtc: Boolean): Map<String, Pair<String, String>> {
        val stationPos = settingsRepo.stationPosition.value
        val tz = if (isUtc) TimeZone.getTimeZone("UTC") else TimeZone.getDefault()
        val sdfDate = dateFormat(tz)
        val sdfTime = SimpleDateFormat("HH:mm", displayLocale()).also { it.timeZone = tz }
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
        val sdfDate = dateFormat(tz)
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
        lowElevation: Double,
        highElevation: Double,
        aosStartMinute: Int,
        aosEndMinute: Int,
        invertAosTimeWindow: Boolean,
        showDeepSpace: Boolean
    ) = viewModelScope.launch {
        settingsRepo.setPassesSettings(
            PassesSettings(
                showDeepSpace,
                hoursAhead,
                minElevation,
                aosStartMinute,
                aosEndMinute,
                invertAosTimeWindow
            )
        )
        settingsRepo.updateOtherSettings { it.copy(lowElevation = lowElevation, highElevation = highElevation) }
        _uiState.update {
            it.copy(
                hours = hoursAhead,
                elevation = minElevation,
                lowElevation = lowElevation,
                highElevation = highElevation,
                aosStartMinute = aosStartMinute,
                aosEndMinute = aosEndMinute,
                invertAosTimeWindow = invertAosTimeWindow,
                showDeepSpace = showDeepSpace
            )
        }
        val modes = settingsRepo.selectedSatModes.value
        satelliteRepo.calculatePasses(
            time = System.currentTimeMillis(),
            hoursAhead = hoursAhead,
            minElevation = minElevation,
            aosStartMinute = aosStartMinute,
            aosEndMinute = aosEndMinute,
            invertAosTimeWindow = invertAosTimeWindow,
            modes = modes
        )
    }

    private fun setModesFilter(modes: List<String>) = viewModelScope.launch {
        settingsRepo.setSelectedSatModes(modes)
        _uiState.update { it.copy(modes = modes) }
        satelliteRepo.calculatePasses(
            time = System.currentTimeMillis(),
            hoursAhead = _uiState.value.hours,
            minElevation = _uiState.value.elevation,
            aosStartMinute = _uiState.value.aosStartMinute,
            aosEndMinute = _uiState.value.aosEndMinute,
            invertAosTimeWindow = _uiState.value.invertAosTimeWindow,
            modes = modes
        )
    }

    private fun refreshPasses() = viewModelScope.launch {
        val settings = settingsRepo.passesSettings.value
        satelliteRepo.calculatePasses(
            time = System.currentTimeMillis(),
            hoursAhead = settings.hoursAhead,
            minElevation = settings.minElevation,
            aosStartMinute = settings.aosStartMinute,
            aosEndMinute = settings.aosEndMinute,
            invertAosTimeWindow = settings.invertAosTimeWindow,
            modes = settingsRepo.selectedSatModes.value
        )
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
