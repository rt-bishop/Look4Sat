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
package com.rtbishop.look4sat.presentation.passesScreen

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rtbishop.look4sat.domain.predict.Predictor
import com.rtbishop.look4sat.domain.predict.SatPass
import com.rtbishop.look4sat.domain.model.DataState
import com.rtbishop.look4sat.domain.DataRepository
import com.rtbishop.look4sat.framework.PreferencesSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import java.util.*
import javax.inject.Inject

@HiltViewModel
class PassesViewModel @Inject constructor(
    private val dataRepository: DataRepository,
    private val predictor: Predictor,
    private val preferences: PreferencesSource
) : ViewModel() {

    private val _passes = MutableLiveData<DataState<List<SatPass>>>(DataState.Loading)
    private val _isFirstLaunchDone = MutableLiveData<Boolean>()
    private var passesProcessing: Job? = null
    val passes: LiveData<DataState<List<SatPass>>> = _passes
    val isFirstLaunchDone: LiveData<Boolean> = _isFirstLaunchDone

    init {
        if (preferences.isSetupDone()) {
            viewModelScope.launch {
                _passes.postValue(DataState.Loading)
                val dateNow = Date()
                val satellites = dataRepository.getSelectedSatellites()
                val stationPos = preferences.loadStationPosition()
                val hoursAhead = preferences.getHoursAhead()
                val minElev = preferences.getMinElevation()
                predictor.triggerCalculation(satellites, stationPos, dateNow, hoursAhead, minElev)
            }
        } else {
            _isFirstLaunchDone.value = false
        }
        viewModelScope.launch {
            predictor.passes.collect { passes ->
                passesProcessing?.cancelAndJoin()
                passesProcessing = viewModelScope.launch { tickPasses(passes) }
            }
        }
    }

    fun triggerInitialSetup() {
        preferences.updatePositionFromGPS()
        viewModelScope.launch {
            _passes.postValue(DataState.Loading)
            val satellites = dataRepository.getSelectedSatellites()
            val stationPos = preferences.loadStationPosition()
            val hoursAhead = preferences.getHoursAhead()
            val minElev = preferences.getMinElevation()
            dataRepository.updateDataFromWeb()
            dataRepository.updateSelection(isSelected = true)
            predictor.forceCalculation(satellites, stationPos, Date(), hoursAhead, minElev)
            preferences.setSetupDone()
            _isFirstLaunchDone.value = true
        }
    }

    fun forceCalculation() {
        viewModelScope.launch {
            _passes.postValue(DataState.Loading)
            passesProcessing?.cancelAndJoin()
            val dateNow = Date()
            val satellites = dataRepository.getSelectedSatellites()
            val stationPos = preferences.loadStationPosition()
            val hoursAhead = preferences.getHoursAhead()
            val minElev = preferences.getMinElevation()
            predictor.forceCalculation(satellites, stationPos, dateNow, hoursAhead, minElev)
        }
    }

    fun shouldUseUTC(): Boolean {
        return preferences.shouldUseUTC()
    }

    private suspend fun tickPasses(passes: List<SatPass>) = withContext(Dispatchers.Default) {
        var currentPasses = passes
        while (isActive) {
            val timeNow = System.currentTimeMillis()
            currentPasses.forEach { pass ->
                if (!pass.isDeepSpace) {
                    val timeStart = pass.aosTime
                    if (timeNow > timeStart) {
                        val deltaNow = timeNow.minus(timeStart).toFloat()
                        val deltaTotal = pass.losTime.minus(timeStart).toFloat()
                        pass.progress = ((deltaNow / deltaTotal) * 100).toInt()
                    }
                }
            }
            currentPasses = currentPasses.filter { it.progress < 100 }
            _passes.postValue(DataState.Success(currentPasses.map { it.copy() }))
            delay(1000)
        }
    }
}
