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
import com.rtbishop.look4sat.utility.toTimerString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import javax.inject.Inject

@HiltViewModel
class PassesViewModel @Inject constructor(
    private val satelliteManager: ISatelliteManager,
    private val repository: IDataRepository,
    private val settings: ISettingsManager
) : ViewModel() {

    private val _passes = MutableLiveData<DataState<List<SatPass>>>()
    private val timerData = Triple("Next - Id:Null", "Name: Null", "00:00:00")
    private val _timerText = MutableLiveData<Triple<String, String, String>>()
    private var passesProcessing: Job? = null
    val entriesTotal: LiveData<Int> = repository.getEntriesTotal().asLiveData()
    val passes: LiveData<DataState<List<SatPass>>> = _passes
    val timerText: LiveData<Triple<String, String, String>> = _timerText

    fun getHoursAhead() = settings.getHoursAhead()

    fun getMinElevation() = settings.getMinElevation()

    init {
        viewModelScope.launch {
            satelliteManager.calculatedPasses.collect { passes ->
                passesProcessing?.cancelAndJoin()
                passesProcessing = viewModelScope.launch {
                    while (isActive) {
                        val timeNow = System.currentTimeMillis()
                        val newPasses = satelliteManager.processPasses(passes, timeNow)

                        if (newPasses.isNotEmpty()) {
                            try {
                                val nextPass = newPasses.first { it.aosTime.minus(timeNow) > 0 }
                                val catNum = nextPass.catNum
                                val name = nextPass.name
                                val millisBeforeStart = nextPass.aosTime.minus(timeNow)
                                val timerString = millisBeforeStart.toTimerString()
                                _timerText.postValue(Triple("Next - Id:$catNum", name, timerString))
                            } catch (e: NoSuchElementException) {
                                val lastPass = newPasses.last()
                                val catNum = lastPass.catNum
                                val name = lastPass.name
                                val millisBeforeEnd = lastPass.losTime.minus(timeNow)
                                val timerString = millisBeforeEnd.toTimerString()
                                _timerText.postValue(Triple("Next - Id:$catNum", name, timerString))
                            }
                        } else _timerText.postValue(timerData)

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
        hoursAhead: Int = 1,
        minElevation: Double = settings.getMinElevation(),
        timeRef: Long = System.currentTimeMillis(),
        selection: List<Int>? = null
    ) {
        viewModelScope.launch {
            _passes.postValue(DataState.Loading)
//            _timerText.postValue(timerDefaultText)
            passesProcessing?.cancelAndJoin()
            selection?.let { items -> settings.saveEntriesSelection(items) }
            settings.setHoursAhead(hoursAhead)
            settings.setMinElevation(minElevation)
            val stationPos = settings.loadStationPosition()
            val selectedIds = settings.loadEntriesSelection()
            val satellites = repository.getEntriesWithIds(selectedIds)
            satelliteManager.calculatePasses(
                satellites, stationPos, timeRef, hoursAhead, minElevation
            )
        }
    }
}
