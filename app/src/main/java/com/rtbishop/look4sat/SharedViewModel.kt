/*
 * Look4Sat. Amateur radio and weather satellite tracker and passes predictor for Android.
 * Copyright (C) 2019, 2020 Arty Bishop (bishop.arty@gmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.rtbishop.look4sat

import android.net.Uri
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.github.amsacode.predict4java.GroundStationPosition
import com.rtbishop.look4sat.data.Result
import com.rtbishop.look4sat.data.SatEntry
import com.rtbishop.look4sat.data.SatPass
import com.rtbishop.look4sat.data.TleSource
import com.rtbishop.look4sat.repo.EntriesRepo
import com.rtbishop.look4sat.repo.SourcesRepo
import com.rtbishop.look4sat.repo.TransmittersRepo
import com.rtbishop.look4sat.utility.PassPredictor
import com.rtbishop.look4sat.utility.PrefsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Singleton

@Singleton
class SharedViewModel @ViewModelInject constructor(
    private val prefsManager: PrefsManager,
    private val sourcesRepo: SourcesRepo,
    private val entriesRepo: EntriesRepo,
    private val transmittersRepo: TransmittersRepo,
    @Assisted private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _passes = MutableLiveData<Result<MutableList<SatPass>>>()
    private var selectedEntries = emptyList<SatEntry>()
    private var shouldTriggerCalculation = true

    init {
        if (prefsManager.isFirstLaunch()) {
            setDefaultTleSources()
        }
    }

    fun getSources() = sourcesRepo.getSources()
    fun getEntries() = entriesRepo.getEntries()
    fun getPasses(): LiveData<Result<MutableList<SatPass>>> = _passes
    fun getTransmittersForSat(satId: Int) = transmittersRepo.getTransmittersForSat(satId)

    fun triggerCalculation() {
        if (shouldTriggerCalculation) {
            shouldTriggerCalculation = false
            calculatePasses()
        }
    }

    fun setSelectedEntries(entries: List<SatEntry>) {
        selectedEntries = entries.filter { it.isSelected }
    }

    fun updateEntriesFromFile(uri: Uri) {
        viewModelScope.launch {
            entriesRepo.updateEntriesFromFile(uri)
        }
    }

    fun updateEntriesFromSources(sources: List<TleSource>) {
        viewModelScope.launch {
            sourcesRepo.updateSources(sources)
            entriesRepo.updateEntriesFromSources(sources)
            transmittersRepo.updateTransmitters()
        }
    }

    fun updateEntriesSelection(entries: List<SatEntry>) {
        viewModelScope.launch {
            selectedEntries = entries.filter { it.isSelected }
            calculatePasses()
            entriesRepo.updateEntriesSelection(selectedEntries.map { it.catNum })
        }
    }

    fun calculatePasses(dateNow: Date = Date(System.currentTimeMillis())) {
        shouldTriggerCalculation = false
        _passes.value = Result.InProgress
        viewModelScope.launch(Dispatchers.Default) {
            val passes = mutableListOf<SatPass>()
            selectedEntries.forEach {
                passes.addAll(getPassesForEntries(it, dateNow, prefsManager.getStationPosition()))
            }
            val filteredPasses =
                filterAndSortPasses(passes, dateNow, prefsManager.getPassPrefs().hoursAhead)
            _passes.postValue(Result.Success(filteredPasses))
        }
    }

    private fun setDefaultTleSources() {
        viewModelScope.launch {
            val defaultTleSources = listOf(
                TleSource("https://celestrak.com/NORAD/elements/active.txt"),
                TleSource("https://amsat.org/tle/current/nasabare.txt")
            )
            sourcesRepo.updateSources(defaultTleSources)
        }
    }

    private fun getPassesForEntries(
        entry: SatEntry,
        dateNow: Date,
        gsp: GroundStationPosition
    ): MutableList<SatPass> {
        val predictor = PassPredictor(entry.tle, gsp)
        val hoursAhead = prefsManager.getPassPrefs().hoursAhead
        val passes = predictor.getPasses(dateNow, hoursAhead, true)
        val passList = passes.map { SatPass(entry.tle, predictor, it) }
        return passList as MutableList<SatPass>
    }

    private fun filterAndSortPasses(
        passes: MutableList<SatPass>,
        dateNow: Date,
        hoursAhead: Int
    ): MutableList<SatPass> {
        val dateFuture = Calendar.getInstance().let {
            it.time = dateNow
            it.add(Calendar.HOUR, hoursAhead)
            it.time
        }
        val minEl = prefsManager.getPassPrefs().minEl
        passes.removeAll { it.pass.startTime.after(dateFuture) }
        passes.removeAll { it.pass.endTime.before(dateNow) }
        passes.removeAll { it.pass.maxEl < minEl }
        passes.sortBy { it.pass.startTime }
        return passes
    }
}
