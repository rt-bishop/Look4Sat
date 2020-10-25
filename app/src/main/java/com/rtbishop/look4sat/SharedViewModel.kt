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
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rtbishop.look4sat.data.*
import com.rtbishop.look4sat.repo.EntriesRepo
import com.rtbishop.look4sat.repo.SourcesRepo
import com.rtbishop.look4sat.repo.TransmittersRepo
import com.rtbishop.look4sat.utility.PassPredictor
import com.rtbishop.look4sat.utility.PrefsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class SharedViewModel @ViewModelInject constructor(
    private val prefsManager: PrefsManager,
    private val sourcesRepo: SourcesRepo,
    private val entriesRepo: EntriesRepo,
    private val transmittersRepo: TransmittersRepo
) : ViewModel() {

    private val _appTimer = MutableLiveData(System.currentTimeMillis())
    private val _passes = MutableLiveData<Result<MutableList<SatPass>>>()
    private val _appEvent = MutableLiveData<Event<Int>>()
    private var selectedEntries = emptyList<SatEntry>()
    private var shouldTriggerCalculation = true

    init {
        if (prefsManager.isFirstLaunch()) updateDefaultSourcesAndEntries()
        startApplicationTimer()
    }

    fun getAppEvent(): LiveData<Event<Int>> = _appEvent
    fun getAppTimer(): LiveData<Long> = _appTimer
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

    fun calculatePasses(dateNow: Date = Date(System.currentTimeMillis())) {
        shouldTriggerCalculation = false
        _passes.value = Result.InProgress
        viewModelScope.launch(Dispatchers.Default) {
            val passes = mutableListOf<SatPass>()
            selectedEntries.forEach { passes.addAll(getPasses(it, dateNow)) }
            val filteredPasses = sortList(passes, dateNow)
            _passes.postValue(Result.Success(filteredPasses))
        }
    }

    fun setEntries(entries: List<SatEntry>) {
        selectedEntries = entries.filter { it.isSelected }
    }

    fun updateEntriesFromFile(uri: Uri) {
        postAppEvent(Event(0))
        viewModelScope.launch {
            try {
                entriesRepo.updateEntriesFromFile(uri)
            } catch (exception: Exception) {
                postAppEvent(Event(1))
            }
        }
    }

    fun updateEntriesFromSources(sources: List<TleSource>) {
        postAppEvent(Event(0))
        viewModelScope.launch {
            try {
                sourcesRepo.updateSources(sources)
                entriesRepo.updateEntriesFromSources(sources)
                transmittersRepo.updateTransmitters()
            } catch (exception: Exception) {
                postAppEvent(Event(1))
            }
        }
    }

    fun updateEntriesSelection(entries: List<SatEntry>) {
        viewModelScope.launch {
            selectedEntries = entries.filter { it.isSelected }
            calculatePasses()
            entriesRepo.updateEntriesSelection(selectedEntries.map { it.catNum })
        }
    }

    private fun postAppEvent(event: Event<Int>) {
        _appEvent.value = event
    }

    private fun startApplicationTimer(tickMillis: Long = 1000L) {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(tickMillis)
                _appTimer.postValue(System.currentTimeMillis())
            }
        }
    }

    private fun getPasses(entry: SatEntry, dateNow: Date): MutableList<SatPass> {
        val gsp = prefsManager.getStationPosition()
        val predictor = PassPredictor(entry.tle, gsp)
        val hoursAhead = prefsManager.getPassPrefs().hoursAhead
        val passes = predictor.getPasses(dateNow, hoursAhead, true)
        val passList = passes.map { SatPass(entry.tle, predictor, it) }
        return passList as MutableList<SatPass>
    }

    private fun sortList(passes: MutableList<SatPass>, dateNow: Date): MutableList<SatPass> {
        val hoursAhead = prefsManager.getPassPrefs().hoursAhead
        val dateFuture = Calendar.getInstance().apply {
            this.time = dateNow
            this.add(Calendar.HOUR, hoursAhead)
        }.time
        passes.removeAll { it.pass.startTime.after(dateFuture) }
        passes.removeAll { it.pass.endTime.before(dateNow) }
        passes.removeAll { it.pass.maxEl < prefsManager.getPassPrefs().minEl }
        passes.sortBy { it.pass.startTime }
        return passes
    }

    private fun updateDefaultSourcesAndEntries() {
        val defaultTleSources = listOf(
            TleSource("https://celestrak.com/NORAD/elements/active.txt"),
            TleSource("https://amsat.org/tle/current/nasabare.txt")
        )
        updateEntriesFromSources(defaultTleSources)
    }
}
