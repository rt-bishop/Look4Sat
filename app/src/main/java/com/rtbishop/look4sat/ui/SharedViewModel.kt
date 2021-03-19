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
package com.rtbishop.look4sat.ui

import androidx.lifecycle.*
import com.github.amsacode.predict4java.Satellite
import com.rtbishop.look4sat.data.model.Result
import com.rtbishop.look4sat.data.model.SatPass
import com.rtbishop.look4sat.data.repository.PrefsRepo
import com.rtbishop.look4sat.data.repository.SatelliteRepo
import com.rtbishop.look4sat.utility.getPredictor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@FlowPreview
@HiltViewModel
class SharedViewModel @Inject constructor(
    private val prefsRepo: PrefsRepo,
    private val satelliteRepo: SatelliteRepo,
) : ViewModel() {

    private val _passes = MutableStateFlow<Result<MutableList<SatPass>>>(Result.InProgress)
    val passes: LiveData<Result<MutableList<SatPass>>> = _passes.asLiveData()

    init {
        if (prefsRepo.isFirstLaunch()) {
            prefsRepo.setFirstLaunchDone()
        }
        calculatePasses()
    }

    fun getAppTimer() = liveData {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000)
        }
    }

    fun getTransmittersForSat(satId: Int) = satelliteRepo.getSatTransmitters(satId).asLiveData()

    fun calculatePasses(dateNow: Date = Date(System.currentTimeMillis())) {
        _passes.value = Result.InProgress
        viewModelScope.launch(Dispatchers.Default) {
            val passes = mutableListOf<SatPass>()
            satelliteRepo.getSelectedSatellites().forEach { satellite ->
                passes.addAll(getPasses(satellite, dateNow))
            }
            val filteredPasses = sortList(passes, dateNow)
            _passes.value = Result.Success(filteredPasses)
        }
    }

    private fun getPasses(satellite: Satellite, dateNow: Date): MutableList<SatPass> {
        val predictor = satellite.getPredictor(prefsRepo.getStationPosition())
        val passes = predictor.getPasses(dateNow, prefsRepo.getHoursAhead(), true)
        val passList = passes.map { SatPass(satellite.tle, predictor, it) }
        return passList as MutableList<SatPass>
    }

    private fun sortList(passes: MutableList<SatPass>, dateNow: Date): MutableList<SatPass> {
        val hoursAhead = prefsRepo.getHoursAhead()
        val dateFuture = Calendar.getInstance().apply {
            this.time = dateNow
            this.add(Calendar.HOUR, hoursAhead)
        }.time
        passes.removeAll { it.pass.startTime.after(dateFuture) }
        passes.removeAll { it.pass.endTime.before(dateNow) }
        passes.removeAll { it.pass.maxEl < prefsRepo.getMinElevation() }
        passes.sortBy { it.pass.startTime }
        return passes
    }
}
