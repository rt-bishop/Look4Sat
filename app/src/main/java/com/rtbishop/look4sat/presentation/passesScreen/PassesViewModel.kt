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
import com.rtbishop.look4sat.data.ISettingsHandler
import com.rtbishop.look4sat.domain.IDataRepository
import com.rtbishop.look4sat.domain.model.DataState
import com.rtbishop.look4sat.domain.predict.Predictor
import com.rtbishop.look4sat.domain.predict.SatPass
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import javax.inject.Inject

@HiltViewModel
class PassesViewModel @Inject constructor(
    private val predictor: Predictor,
    private val settings: ISettingsHandler,
    private val repository: IDataRepository
) : ViewModel() {

    private var passesProcessing: Job? = null
    private val _passes = MutableLiveData<DataState<List<SatPass>>>()
    val passes: LiveData<DataState<List<SatPass>>> = _passes

    init {
        viewModelScope.launch {
            predictor.calculatedPasses.collect { passes ->
                passesProcessing?.cancelAndJoin()
                passesProcessing = viewModelScope.launch {
                    while (isActive) {
                        val time = System.currentTimeMillis()
                        val newPasses = predictor.processPasses(passes, time)
                        _passes.postValue(DataState.Success(newPasses))
                        delay(1000)
                    }
                }
            }
        }
    }

    fun forceCalculation(
        hoursAhead: Int = settings.getHoursAhead(),
        minElevation: Double = settings.getMinElevation(),
        timeRef: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            _passes.postValue(DataState.Loading)
            passesProcessing?.cancelAndJoin()
            val satellites = repository.getSelectedEntries()
            val stationPos = settings.loadStationPosition()
            predictor.forceCalculation(satellites, stationPos, timeRef, hoursAhead, minElevation)
        }
    }

    fun shouldUseUTC(): Boolean {
        return settings.getUseUTC()
    }

    fun saveCalculationPrefs(hoursAhead: Int, minElevation: Double) {
        settings.setHoursAhead(hoursAhead)
        settings.setMinElevation(minElevation)
    }
}
