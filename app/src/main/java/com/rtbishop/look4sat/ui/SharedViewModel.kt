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

package com.rtbishop.look4sat.ui

import android.net.Uri
import androidx.lifecycle.*
import com.github.amsacode.predict4java.GroundStationPosition
import com.rtbishop.look4sat.data.*
import com.rtbishop.look4sat.repo.Repository
import com.rtbishop.look4sat.utility.PassPredictor
import com.rtbishop.look4sat.utility.PrefsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedViewModel @Inject constructor(
    private val prefsManager: PrefsManager,
    private val repository: Repository
) : ViewModel() {

    private val _passes = MutableLiveData<Result<MutableList<SatPass>>>()

    init {
        if (prefsManager.isFirstLaunch()) {
            setDefaultTleSources()
        }
    }

    fun getSources() = repository.getSources()
    fun getEntries() = repository.getEntries()
    fun getPasses(): LiveData<Result<MutableList<SatPass>>> = _passes
    fun getTransmittersForSat(id: Int) = liveData { emit(repository.getTransmittersForSatId(id)) }

    fun getPassPrefs(): PassPrefs {
        return prefsManager.getPassPrefs()
    }

    fun setPassPrefs(hoursAhead: Int, minEl: Double) {
        prefsManager.setPassPrefs(hoursAhead, minEl)
    }

    fun getStationPosition(): GroundStationPosition {
        return prefsManager.getStationPosition()
    }

    fun setStationPositionFromGPS() {
        prefsManager.setStationPositionFromGPS()
    }

    fun shouldUseUTC(): Boolean {
        return prefsManager.shouldUseUTC()
    }

    fun shouldUseCompass(): Boolean {
        return prefsManager.shouldUseCompass()
    }

    fun updateEntriesFromFile(uri: Uri) {
        viewModelScope.launch {
            repository.updateEntriesFromFile(uri)
        }
    }

    fun updateEntriesFromSources(sources: List<TleSource>) {
        viewModelScope.launch {
            repository.updateSources(sources)
            repository.updateEntriesFromSources(sources)
            repository.updateTransmitters()
        }
    }

    fun updateEntriesSelection(satIds: MutableList<Int>) {
        viewModelScope.launch {
            repository.updateEntriesSelection(satIds)
        }
    }

    fun calculatePassesForEntries(entries: List<SatEntry>) {
        _passes.value = Result.InProgress
        val dateNow = Date(System.currentTimeMillis())
        val passes = mutableListOf<SatPass>()
        viewModelScope.launch(Dispatchers.Default) {
            entries.forEach {
                passes.addAll(getPassesForEntries(it, dateNow, getStationPosition()))
            }
            val filteredPasses = filterAndSortPasses(passes, dateNow, getPassPrefs().hoursAhead)
            _passes.postValue(Result.Success(filteredPasses))
        }
    }

    private fun setDefaultTleSources() {
        val defaultTleSources = listOf(
            TleSource("https://celestrak.com/NORAD/elements/active.txt"),
            TleSource("https://amsat.org/tle/current/nasabare.txt")
        )
        viewModelScope.launch { repository.updateSources(defaultTleSources) }
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
