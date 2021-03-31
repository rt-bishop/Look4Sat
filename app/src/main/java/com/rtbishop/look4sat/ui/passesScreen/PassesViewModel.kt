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
package com.rtbishop.look4sat.ui.passesScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.rtbishop.look4sat.data.model.Result
import com.rtbishop.look4sat.data.model.SatPass
import com.rtbishop.look4sat.data.repository.PassesRepo
import com.rtbishop.look4sat.di.DefaultDispatcher
import com.rtbishop.look4sat.utility.PrefsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PassesViewModel @Inject constructor(
    private val passesRepo: PassesRepo,
    private val prefsManager: PrefsManager,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _passes = MutableStateFlow<Result<List<SatPass>>>(Result.InProgress)
    private var passesCache = emptyList<SatPass>()
    private var passesCalculation: Job? = null
    val passes = _passes.asLiveData(viewModelScope.coroutineContext)

    fun getAppTimer() = liveData {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000)
        }
    }

    fun shouldUseUTC(): Boolean {
        return prefsManager.shouldUseUTC()
    }

    init {
        viewModelScope.launch {
            passesRepo.passes.collect { newPasses ->
                passesCalculation?.cancelAndJoin()
                passesCalculation = launch { handlePasses(newPasses) }
            }
        }
    }

    fun forceCalculation() {
        viewModelScope.launch {
            _passes.value = Result.InProgress
            passesRepo.calculatePasses()
        }
    }

    private suspend fun handlePasses(newPasses: List<SatPass>) = withContext(defaultDispatcher) {
        if (newPasses.isEmpty()) {
            _passes.value = Result.Success(newPasses)
        } else {
            while (isActive) {
                val timeNow = System.currentTimeMillis()
                passesCache = newPasses.toMutableList().apply {
                    val iterator = listIterator()
                    while (iterator.hasNext()) {
                        val satPass = iterator.next()
                        if (!satPass.isDeepSpace) {
                            if (satPass.progress <= 100) {
                                val timeStart = satPass.startDate.time
                                if (timeNow > timeStart) {
                                    val deltaNow = timeNow.minus(timeStart).toFloat()
                                    val deltaTotal = satPass.endDate.time.minus(timeStart).toFloat()
                                    satPass.progress = ((deltaNow / deltaTotal) * 100).toInt()
                                }
                            } else {
                                iterator.remove()
                            }
                        }
                    }
                }
                _passes.value = Result.Success(passesCache)
                Timber.d("$passesCache")
                delay(1000)
            }
        }
    }
}
