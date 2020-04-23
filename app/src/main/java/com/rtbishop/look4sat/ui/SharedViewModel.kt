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

import androidx.lifecycle.*
import com.github.amsacode.predict4java.GroundStationPosition
import com.rtbishop.look4sat.data.SatEntry
import com.rtbishop.look4sat.data.SatPass
import com.rtbishop.look4sat.repo.Repository
import com.rtbishop.look4sat.utility.PassPredictor
import com.rtbishop.look4sat.utility.PrefsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedViewModel @Inject constructor(
    private val prefsManager: PrefsManager,
    private val repository: Repository
) : ViewModel() {

    private val urlList = listOf("https://celestrak.com/NORAD/elements/active.txt")
    private val _gsp = MutableLiveData(prefsManager.getPosition())
    private val _satPassList = MutableLiveData<MutableList<SatPass>>()
    private val _isListRefreshing = MutableLiveData<Boolean>()
    private val _debugMessage = MutableLiveData<String>()
    private var calculationJob: Job? = null
    var isFirstLaunch = true

    fun getTransmittersForSat(id: Int) = liveData { emit(repository.getTransmittersByCatNum(id)) }
    fun getGSP(): LiveData<GroundStationPosition> = _gsp
    fun getSatPassList(): LiveData<MutableList<SatPass>> = _satPassList
    fun getRefreshing(): LiveData<Boolean> = _isListRefreshing
    fun getDebugMessage(): LiveData<String> = _debugMessage
    fun getRefreshRate() = prefsManager.getRefreshRate()
    fun getHoursAhead() = prefsManager.getHoursAhead()
    fun getMinElevation() = prefsManager.getMinElevation()

    suspend fun getAllEntries(): List<SatEntry> {
        return repository.getAllEntries()
    }

    fun setPassPrefs(hoursAhead: Int, minEl: Double) {
        prefsManager.setHoursAhead(hoursAhead)
        prefsManager.setMinElevation(minEl)
    }

    fun setPositionFromPref() = _gsp.postValue(prefsManager.getPosition())

    fun updatePosition() {
        prefsManager.getLastKnownPosition().let {
            prefsManager.setPosition(it)
            _gsp.postValue(it)
            _debugMessage.postValue("Location was updated")
        }
    }

    fun updateEntries() {
        viewModelScope.launch(Dispatchers.IO) {
            val selected = repository.getSelectedEntries().map { it.catNum }
            repository.updateEntriesFrom(urlList)
            repository.updateEntriesSelection(selected)
            _debugMessage.postValue("TLE file was updated")
        }
    }

    fun updateTransmitters() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTransmitters()
            _debugMessage.postValue("Transceivers database was updated")
        }
    }

    fun updateEntriesSelection(catNumList: MutableList<Int>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateEntriesSelection(catNumList)
            calculatePasses()
        }
    }

    fun calculatePasses() {
        _isListRefreshing.postValue(true)
        calculationJob?.cancel()
        var passList = mutableListOf<SatPass>()
        val dateNow = Date()
        val gsp = getGSP().value ?: GroundStationPosition(0.0, 0.0, 0.0)
        calculationJob = viewModelScope.launch(Dispatchers.Default) {
            repository.getSelectedEntries().forEach {
                passList.addAll(getPassesForEntries(it, dateNow, gsp))
            }
            passList = filterAndSortPasses(passList, dateNow, getHoursAhead())
            _satPassList.postValue(passList)
            _isListRefreshing.postValue(false)
        }
    }

    private fun getPassesForEntries(
        entry: SatEntry,
        dateNow: Date,
        gsp: GroundStationPosition
    ): MutableList<SatPass> {
        val predictor = PassPredictor(entry.tle, gsp)
        val passes = predictor.getPasses(dateNow, getHoursAhead(), true)
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
        passes.removeAll { it.pass.startTime.after(dateFuture) }
        passes.removeAll { it.pass.endTime.before(dateNow) }
        passes.removeAll { it.pass.maxEl < getMinElevation() }
        passes.sortBy { it.pass.startTime }
        return passes
    }
}
