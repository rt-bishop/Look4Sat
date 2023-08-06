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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rtbishop.look4sat.MainApplication
import com.rtbishop.look4sat.domain.model.DataState
import com.rtbishop.look4sat.domain.model.PassesSettings
import com.rtbishop.look4sat.domain.predict.SatPass
import com.rtbishop.look4sat.domain.repository.ISatelliteRepo
import com.rtbishop.look4sat.domain.repository.ISettingsRepo
import com.rtbishop.look4sat.domain.utility.toTimerString
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PassesViewModel(
    private val satelliteRepo: ISatelliteRepo, private val settingsRepo: ISettingsRepo
) : ViewModel() {

    private val _passes = MutableStateFlow<DataState<List<SatPass>>>(DataState.Loading)
    private val timerData = Triple("Next - Id:Null", "Name: Null", "00:00:00")
    private val _timerText = MutableStateFlow(timerData)
    private var passesProcessing: Job? = null
    val passes: StateFlow<DataState<List<SatPass>>> = _passes
    val timerText: StateFlow<Triple<String, String, String>?> = _timerText

    fun getFilterSettings() = settingsRepo.passesSettings.value

//    fun shouldUseUTC() = settingsRepo.otherSettings.value.utcState

    init {
        viewModelScope.launch { satelliteRepo.initRepository() }
        viewModelScope.launch {
            satelliteRepo.passes.collect { passes ->
                passesProcessing?.cancelAndJoin()
                passesProcessing = viewModelScope.launch {
                    while (isActive) {
                        val timeNow = System.currentTimeMillis()
                        val newPasses = satelliteRepo.processPasses(passes, timeNow)

                        if (newPasses.isNotEmpty()) {
                            try {
                                val nextPass = newPasses.first { it.aosTime.minus(timeNow) > 0 }
                                val catNum = nextPass.catNum
                                val name = nextPass.name
                                val millisBeforeStart = nextPass.aosTime.minus(timeNow)
                                val timerString = millisBeforeStart.toTimerString()
                                _timerText.emit(Triple("Next - Id:$catNum", name, timerString))
                            } catch (e: NoSuchElementException) {
                                val lastPass = newPasses.last()
                                val catNum = lastPass.catNum
                                val name = lastPass.name
                                val millisBeforeEnd = lastPass.losTime.minus(timeNow)
                                val timerString = millisBeforeEnd.toTimerString()
                                _timerText.emit(Triple("Next - Id:$catNum", name, timerString))
                            }
                        } else _timerText.emit(timerData)

                        _passes.emit(DataState.Success(newPasses))
                        delay(1000)
                    }
                }
            }
        }
    }

    fun calculatePasses() = viewModelScope.launch {
        _passes.emit(DataState.Loading)
        passesProcessing?.cancelAndJoin()
        val (hoursAhead, minElevation) = settingsRepo.passesSettings.value
        satelliteRepo.calculatePasses(System.currentTimeMillis(), hoursAhead, minElevation)
    }

    fun calculatePasses(timeRef: Long, hoursAhead: Int, minElev: Double) = viewModelScope.launch {
        _passes.emit(DataState.Loading)
        passesProcessing?.cancelAndJoin()
        settingsRepo.savePassesSettings(PassesSettings(hoursAhead, minElev))
        satelliteRepo.calculatePasses(timeRef, hoursAhead, minElev)
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
