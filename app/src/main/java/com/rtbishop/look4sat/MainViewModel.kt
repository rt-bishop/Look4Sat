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

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.amsacode.predict4java.GroundStationPosition
import com.github.amsacode.predict4java.SatNotFoundException
import com.github.amsacode.predict4java.TLE
import com.rtbishop.look4sat.predict4kotlin.PassPredictor
import com.rtbishop.look4sat.repo.Repository
import com.rtbishop.look4sat.repo.SatPass
import com.rtbishop.look4sat.repo.Transmitter
import com.rtbishop.look4sat.utility.DataManager
import com.rtbishop.look4sat.utility.PrefsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*
import javax.inject.Inject

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application
    private val urlList = listOf("https://celestrak.com/NORAD/elements/active.txt")
    private val _satPassList = MutableLiveData<MutableList<SatPass>>()
    private val _debugMessage = MutableLiveData<String>()
    private val _gsp = MutableLiveData<GroundStationPosition>()
    private val _isListRefreshing = MutableLiveData<Boolean>()
    private var calculationJob: Job? = null

    @Inject
    lateinit var repository: Repository
    @Inject
    lateinit var dataManager: DataManager
    @Inject
    lateinit var prefsManager: PrefsManager

    init {
        (app as Look4SatApp).appComponent.inject(this)
        setGroundStationPosition()
    }

    var tleMainList = dataManager.loadTleList()
    var tleSelection = dataManager.loadSelectionList()

    fun getGSP(): LiveData<GroundStationPosition> = _gsp
    fun getDebugMessage(): LiveData<String> = _debugMessage
    fun getSatPassList(): LiveData<MutableList<SatPass>> = _satPassList
    fun getRefreshing(): LiveData<Boolean> = _isListRefreshing
    fun getRefreshRate() = prefsManager.getRefreshRate()
    fun getHoursAhead() = prefsManager.getHoursAhead()
    fun getMinElevation() = prefsManager.getMinElevation()
    fun setGroundStationPosition() = _gsp.postValue(prefsManager.getGroundStationPosition())

    fun updateLocation() {
        dataManager.getLastKnownLocation()?.let {
            prefsManager.setGroundStationPosition(it)
            _gsp.postValue(it)
        }
    }

    fun setPassPrefs(hoursAhead: Int, minEl: Double) {
        prefsManager.setHoursAhead(hoursAhead)
        prefsManager.setMinElevation(minEl)
    }

    fun updateAndSaveTleFile() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = repository.getStreamForUrl(urlList)
                val tleList = TLE.importSat(inputStream).apply { sortBy { it.name } }
                dataManager.saveTleList(tleList)
                tleMainList = tleList
                updateAndSaveSelectionList(mutableListOf())
                _debugMessage.postValue(app.getString(R.string.update_tle_success))
            } catch (exception: IOException) {
                _debugMessage.postValue(app.getString(R.string.update_failure))
            }
        }
    }

    fun updateTransmittersDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updateTransmittersDatabase()
                _debugMessage.postValue(app.getString(R.string.update_trans_success))
            } catch (exception: IOException) {
                _debugMessage.postValue(app.getString(R.string.update_failure))
            }
        }
    }

    fun updateAndSaveSelectionList(list: MutableList<Int>) {
        tleSelection = list
        dataManager.saveSelectionList(list)
    }

    fun getPasses() {
        _isListRefreshing.value = true
        calculationJob?.cancel()
        var passList = mutableListOf<SatPass>()
        calculationJob = viewModelScope.launch {
            if (tleMainList.isNotEmpty() && tleSelection.isNotEmpty()) {
                withContext(Dispatchers.Default) {
                    val dateNow = Date()
                    tleSelection.forEach { indexOfSelection ->
                        val tle = tleMainList[indexOfSelection]
                        try {
                            val gsp = getGSP().value ?: GroundStationPosition(0.0, 0.0, 0.0)
                            val predictor = PassPredictor(tle, gsp)
                            val passes = predictor.getPasses(dateNow, getHoursAhead(), true)
                            passes.forEach {
                                passList.add(SatPass(tle, predictor, it))
                            }
                        } catch (exception: IllegalArgumentException) {
                            _debugMessage.postValue(app.getString(R.string.err_parse_tle))
                        } catch (exception: SatNotFoundException) {
                            _debugMessage.postValue(app.getString(R.string.err_sat_wont_pass))
                        }
                    }
                    passList = filterAndSortList(passList, getHoursAhead())
                }
                _satPassList.postValue(passList)
            } else {
                _satPassList.postValue(mutableListOf())
                _debugMessage.postValue(app.getString(R.string.err_no_sat_selected))
            }
            _isListRefreshing.value = false
        }
    }

    private fun filterAndSortList(
        list: MutableList<SatPass>,
        hoursAhead: Int
    ): MutableList<SatPass> {
        val dateNow = Date()
        val dateFuture = Calendar.getInstance().let {
            it.time = dateNow
            it.add(Calendar.HOUR, hoursAhead)
            it.time
        }
        list.removeAll { it.pass.startTime.after(dateFuture) }
        list.removeAll { it.pass.endTime.before(dateNow) }
        list.removeAll { it.pass.maxEl < getMinElevation() }
        list.sortBy { it.pass.startTime }
        return list
    }

    suspend fun getTransmittersForSat(id: Int): List<Transmitter> {
        return repository.getTransmittersForSat(id)
    }
}
