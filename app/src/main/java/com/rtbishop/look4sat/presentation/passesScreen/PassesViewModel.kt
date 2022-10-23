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
package com.rtbishop.look4sat.presentation.passesScreen

import androidx.lifecycle.*
import com.rtbishop.look4sat.domain.IDataRepository
import com.rtbishop.look4sat.domain.ISatelliteManager
import com.rtbishop.look4sat.domain.ISettingsManager
import com.rtbishop.look4sat.domain.model.DataState
import com.rtbishop.look4sat.domain.predict.SatPass
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import javax.inject.Inject

@HiltViewModel
class PassesViewModel @Inject constructor(
    private val satelliteManager: ISatelliteManager,
    private val repository: IDataRepository,
    private val settings: ISettingsManager
) : ViewModel() {

    private var passesProcessing: Job? = null
    private val _passes = MutableLiveData<DataState<List<SatPass>>>(DataState.Loading)
    val passes: LiveData<DataState<List<SatPass>>> = _passes
    val entriesTotal: LiveData<Int> = repository.getEntriesTotal().asLiveData()

    init {
        viewModelScope.launch {
            satelliteManager.calculatedPasses.collect { passes ->
                passesProcessing?.cancelAndJoin()
                passesProcessing = viewModelScope.launch {
                    while (isActive) {
                        val time = System.currentTimeMillis()
                        val newPasses = satelliteManager.processPasses(passes, time)
                        _passes.postValue(DataState.Success(newPasses))
                        delay(1000)
                    }
                }
            }
        }
        viewModelScope.launch {
            repository.updateState.collect { updateState ->
                if (updateState is DataState.Success) calculatePasses()
            }
        }
        calculatePasses()
    }

    fun shouldUseUTC() = settings.getUseUTC()

    fun calculatePasses(
        hoursAhead: Int = settings.getHoursAhead(),
        minElevation: Double = settings.getMinElevation(),
        timeRef: Long = System.currentTimeMillis(),
        selection: List<Int>? = null
    ) {
        viewModelScope.launch {
            _passes.postValue(DataState.Loading)
            passesProcessing?.cancelAndJoin()
            selection?.let { items -> settings.saveEntriesSelection(items) }
            settings.setHoursAhead(hoursAhead)
            settings.setMinElevation(minElevation)
            val stationPos = settings.loadStationPosition()
            val selectedIds = settings.loadEntriesSelection()
            val satellites = repository.getEntriesWithIds(selectedIds)
            satelliteManager.calculatePasses(satellites, stationPos, timeRef, hoursAhead, minElevation)
        }
    }
}
