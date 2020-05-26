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

import android.location.LocationManager
import android.net.Uri
import androidx.lifecycle.*
import com.github.amsacode.predict4java.GroundStationPosition
import com.rtbishop.look4sat.data.Result
import com.rtbishop.look4sat.data.SatEntry
import com.rtbishop.look4sat.data.SatPass
import com.rtbishop.look4sat.data.TleSource
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

    private val _satPassList = MutableLiveData<Result<MutableList<SatPass>>>()
    private val _updateStatus = MutableLiveData<Result<Int>>()
    private val _gsp = MutableLiveData<Result<GroundStationPosition>>().apply {
        value = Result.Success(prefsManager.getPosition())
    }
    private var calculationJob: Job? = null
    var isFirstLaunch = true

    fun getTransmittersForSat(id: Int) = liveData { emit(repository.getTransmittersByCatNum(id)) }
    fun getPassList(): LiveData<Result<MutableList<SatPass>>> = _satPassList
    fun getUpdateStatus(): LiveData<Result<Int>> = _updateStatus
    fun getGSP(): LiveData<Result<GroundStationPosition>> = _gsp

    fun getCompass() = prefsManager.getCompass()
    fun getRefreshRate() = prefsManager.getRefreshRate()
    fun getHoursAhead() = prefsManager.getHoursAhead()
    fun getMinElevation() = prefsManager.getMinElevation()
    fun getTleSources() = prefsManager.getTleSources()
    fun setPositionFromPref() = _gsp.postValue(Result.Success(prefsManager.getPosition()))
    suspend fun getAllEntries() = repository.getAllEntries()

    fun setPassPrefs(hoursAhead: Int, minEl: Double) {
        prefsManager.setHoursAhead(hoursAhead)
        prefsManager.setMinElevation(minEl)
    }

    fun updatePosition() {
        val provider = LocationManager.PASSIVE_PROVIDER
        try {
            val location = prefsManager.locationManager.getLastKnownLocation(provider)
            location.let {
                if (it == null) {
                    _gsp.postValue(Result.Error(IllegalArgumentException()))
                } else {
                    val gsp = GroundStationPosition(it.latitude, it.longitude, it.altitude)
                    prefsManager.setPosition(gsp)
                    _gsp.postValue(Result.Success(gsp))
                }
            }
        } catch (e: SecurityException) {
            _gsp.postValue(Result.Error(e))
        }
    }

    fun updateEntriesFromFile(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val selected = repository.getSelectedEntries().map { it.catNum }
                repository.updateEntriesFromFile(uri)
                repository.updateEntriesSelection(selected)
                _updateStatus.postValue(Result.Success(0))
            } catch (e: Exception) {
                _updateStatus.postValue(Result.Error(e))
            }
        }
    }

    fun updateEntriesFromWeb(list: List<TleSource>) {
        val sourcesSet = list.map { it.url }.toSet()
        prefsManager.setTleSources(sourcesSet)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val selected = repository.getSelectedEntries().map { it.catNum }
                repository.updateEntriesFromUrl(list)
                repository.updateEntriesSelection(selected)
                _updateStatus.postValue(Result.Success(0))
            } catch (e: Exception) {
                _updateStatus.postValue(Result.Error(e))
            }
        }
    }

    fun updateEntriesSelection(catNumList: MutableList<Int>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateEntriesSelection(catNumList)
            calculatePasses()
        }
    }

    fun updateTransmitters() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updateTransmitters()
                _updateStatus.postValue(Result.Success(1))
            } catch (e: Exception) {
                _updateStatus.postValue(Result.Error(e))
            }
        }
    }

    fun calculatePasses() {
        _satPassList.postValue(Result.InProgress)
        calculationJob?.cancel()
        var passList = mutableListOf<SatPass>()
        val dateNow = Date()
        val gsp = prefsManager.getPosition()
        calculationJob = viewModelScope.launch(Dispatchers.Default) {
            repository.getSelectedEntries().forEach {
                passList.addAll(getPassesForEntries(it, dateNow, gsp))
            }
            passList = filterAndSortPasses(passList, dateNow, getHoursAhead())
            _satPassList.postValue(Result.Success(passList))
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
