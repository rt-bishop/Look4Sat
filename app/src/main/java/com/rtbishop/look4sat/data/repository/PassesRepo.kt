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
package com.rtbishop.look4sat.data.repository

import com.github.amsacode.predict4java.Satellite
import com.rtbishop.look4sat.data.model.Result
import com.rtbishop.look4sat.data.model.SatPass
import com.rtbishop.look4sat.di.DefaultDispatcher
import com.rtbishop.look4sat.utility.PrefsManager
import com.rtbishop.look4sat.utility.getPredictor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PassesRepo @Inject constructor(
    private val prefsManager: PrefsManager,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {
    private val _passes = MutableSharedFlow<Result<List<SatPass>>>(replay = 1)
    private var selectedSatellites = emptyList<Satellite>()
    val passes: SharedFlow<Result<List<SatPass>>> = _passes

    suspend fun triggerCalculation(satellites: List<Satellite>, refDate: Date = Date()) {
        if (satellites.isEmpty()) _passes.emit(Result.Error(Exception()))
        val oldCatNums = selectedSatellites.map { it.tle.catnum }
        val newCatNums = satellites.map { it.tle.catnum }
        if (oldCatNums != newCatNums) forceCalculation(satellites, refDate)
    }

    suspend fun forceCalculation(satellites: List<Satellite>, refDate: Date = Date()) {
        _passes.emit(Result.InProgress)
        withContext(defaultDispatcher) {
            val allPasses = mutableListOf<SatPass>()
            selectedSatellites = satellites
            satellites.forEach { satellite ->
                allPasses.addAll(getPasses(satellite, refDate))
            }
            _passes.emit(Result.Success(filterPasses(allPasses, refDate)))
        }
    }

    private fun getPasses(satellite: Satellite, refDate: Date): List<SatPass> {
        val predictor = satellite.getPredictor(prefsManager.getStationPosition())
        return predictor.getPasses(refDate, prefsManager.getHoursAhead(), true)
    }

    private fun filterPasses(passes: List<SatPass>, refDate: Date): List<SatPass> {
        val timeFuture = Date(refDate.time + (prefsManager.getHoursAhead() * 3600 * 1000))
        return passes.filter { it.endDate.after(refDate) }
            .filter { it.startDate.before(timeFuture) }
            .filter { it.maxElevation > prefsManager.getMinElevation() }
            .sortedBy { it.startDate }
    }
}
