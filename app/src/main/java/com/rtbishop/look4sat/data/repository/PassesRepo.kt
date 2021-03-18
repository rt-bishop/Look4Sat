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
import com.rtbishop.look4sat.utility.getPredictor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PassesRepo @Inject constructor(
    private val prefsRepo: PrefsRepo,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {
    private val _passes = MutableStateFlow<Result<MutableList<SatPass>>>(Result.InProgress)
    val passes: StateFlow<Result<MutableList<SatPass>>> = _passes

    private var selectedSatellites = emptyList<Satellite>()

    suspend fun calculatePasses(satellites: List<Satellite>) {
        if (satellites !== selectedSatellites) {
            withContext(defaultDispatcher) {
                _passes.value = Result.InProgress
                selectedSatellites = satellites
                val refDate = Date(System.currentTimeMillis())
                val allPasses = mutableListOf<SatPass>()
                selectedSatellites.forEach { satellite ->
                    allPasses.addAll(getPasses(satellite, refDate))
                }
                val filteredPasses = filterPasses(allPasses, refDate)
                _passes.value = Result.Success(filteredPasses)
            }
        }
    }

    private fun getPasses(satellite: Satellite, refDate: Date): MutableList<SatPass> {
        val predictor = satellite.getPredictor(prefsRepo.getStationPosition())
        val passes = predictor.getPasses(refDate, prefsRepo.getHoursAhead(), true)
        val passList = passes.map { SatPass(satellite.tle, predictor, it) }
        return passList as MutableList<SatPass>
    }

    private fun filterPasses(passes: MutableList<SatPass>, refDate: Date): MutableList<SatPass> {
        val hoursAhead = prefsRepo.getHoursAhead()
        val dateFuture = Calendar.getInstance().apply {
            this.time = refDate
            this.add(Calendar.HOUR, hoursAhead)
        }.time
        passes.removeAll { it.pass.startTime.after(dateFuture) }
        passes.removeAll { it.pass.endTime.before(refDate) }
        passes.removeAll { it.pass.maxEl < prefsRepo.getMinElevation() }
        passes.sortBy { it.pass.startTime }
        return passes
    }
}