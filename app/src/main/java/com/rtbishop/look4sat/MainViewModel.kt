/*
 * LookingSat. Amateur radio & weather satellite tracker and passes calculator.
 * Copyright (C) 2019 Arty Bishop (bishop.arty@gmail.com)
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
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.amsacode.predict4java.GroundStationPosition
import com.github.amsacode.predict4java.PassPredictor
import com.github.amsacode.predict4java.SatNotFoundException
import com.github.amsacode.predict4java.TLE
import com.google.android.gms.location.FusedLocationProviderClient
import com.rtbishop.look4sat.repo.Repository
import com.rtbishop.look4sat.repo.SatPass
import com.rtbishop.look4sat.repo.Transmitter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.*
import javax.inject.Inject

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val defValueLoc = application.getString(R.string.def_gsp_loc)
    private val defValueHours = application.getString(R.string.def_hours_ahead).toInt()
    private val defValueMinEl = application.getString(R.string.def_min_el).toDouble()
    private val defValueUpdateFreq = application.getString(R.string.def_update_freq)
    private val keyHours = application.getString(R.string.key_hours_ahead)
    private val keyMinEl = application.getString(R.string.key_min_el)
    private val keyLat = application.getString(R.string.key_lat)
    private val keyLon = application.getString(R.string.key_lon)
    private val keyAlt = application.getString(R.string.key_alt)
    private val keyDelay = application.getString(R.string.key_delay)
    private val tleMainListFileName = application.getString(R.string.tle_main_list_file_name)
    private val tleSelectionFileName = application.getString(R.string.tle_selection_file_name)
    private val app = application

    private val _debugMessage = MutableLiveData<String>()
    val debugMessage: LiveData<String> = _debugMessage

    private val _satPassList = MutableLiveData<List<SatPass>>()
    val passSatList: LiveData<List<SatPass>> = _satPassList

    private val _gsp = MutableLiveData<GroundStationPosition>()
    val gsp: LiveData<GroundStationPosition> = _gsp

    private val urlList = listOf(
        "https://celestrak.com/NORAD/elements/amateur.txt",
        "https://celestrak.com/NORAD/elements/weather.txt"
    )

    @Inject
    lateinit var locationClient: FusedLocationProviderClient
    @Inject
    lateinit var preferences: SharedPreferences
    @Inject
    lateinit var repository: Repository

    lateinit var tleMainList: List<TLE>
    lateinit var tleSelection: MutableList<Int>

    init {
        (app as Look4SatApp).appComponent.inject(this)
        loadDataFromDisk()
    }

    val delay: Long
        get() = preferences.getString(keyDelay, defValueUpdateFreq)!!.toLong()

    val hoursAhead: Int
        get() = preferences.getInt(keyHours, defValueHours)

    val minEl: Double
        get() = preferences.getDouble(keyMinEl, defValueMinEl)

    fun setGroundStationPosition() {
        val lat = preferences.getString(keyLat, defValueLoc)!!.toDouble()
        val lon = preferences.getString(keyLon, defValueLoc)!!.toDouble()
        val alt = preferences.getString(keyAlt, defValueLoc)!!.toDouble()
        _gsp.postValue(GroundStationPosition(lat, lon, alt))
    }

    fun getCurrentLocation() {
        locationClient.lastLocation.addOnSuccessListener { location: Location? ->
            val lat = location?.latitude ?: 0.0
            val lon = location?.longitude ?: 0.0
            val alt = location?.altitude ?: 0.0

            preferences.edit {
                putString(keyLat, lat.toString())
                putString(keyLon, lon.toString())
                putString(keyAlt, alt.toString())
                apply()
            }
            _gsp.postValue(GroundStationPosition(lat, lon, alt))
            _debugMessage.postValue(app.getString(R.string.updateLocSuccess))
        }
    }

    fun setPassPrefs(hoursAhead: Int, minEl: Double) {
        preferences.edit {
            putInt(keyHours, hoursAhead)
            putDouble(keyMinEl, minEl)
            apply()
        }
    }

    fun updateAndSaveTleFile() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = repository.getStreamForUrl(urlList)
                val tleList = TLE.importSat(inputStream).apply { sortBy { it.name } }
                val fileOutStream = app.openFileOutput(tleMainListFileName, Context.MODE_PRIVATE)
                ObjectOutputStream(fileOutStream).apply {
                    writeObject(tleList)
                    flush()
                    close()
                }
                tleMainList = tleList
                tleSelection = mutableListOf()
                _debugMessage.postValue(app.getString(R.string.updateTleSuccess))
            } catch (exception: IOException) {
                _debugMessage.postValue(app.getString(R.string.updateTleFailure))
            }
        }
    }

    fun updateTransmittersDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updateTransmittersDatabase()
                _debugMessage.postValue(app.getString(R.string.updateTransSuccess))
            } catch (exception: IOException) {
                _debugMessage.postValue(app.getString(R.string.updateTransFailure))
            }
        }
    }

    fun updateAndSaveSelectionList(list: MutableList<Int>) {
        tleSelection = list
        try {
            val fileOutputStream = app.openFileOutput(tleSelectionFileName, Context.MODE_PRIVATE)
            ObjectOutputStream(fileOutputStream).apply {
                writeObject(list)
                flush()
                close()
            }
        } catch (exception: IOException) {
            _debugMessage.postValue(exception.toString())
        }
    }

    suspend fun getPasses() {
        val passList = mutableListOf<SatPass>()
        withContext(Dispatchers.Default) {
            tleSelection.forEach { indexOfSelection ->
                val tle = tleMainList[indexOfSelection]
                try {
                    val predictor = PassPredictor(tle, gsp.value)
                    val passes = predictor.getPasses(Date(), hoursAhead, true)
                    passes.forEach { passList.add(SatPass(tle, predictor, it)) }
                } catch (exception: IllegalArgumentException) {
                    _debugMessage.postValue("There was a problem with ${tle.name}")
                } catch (exception: SatNotFoundException) {
                    _debugMessage.postValue("${tle.name} shall not pass")
                }
            }
            passList.retainAll { it.pass.maxEl >= minEl }
            passList.sortBy { it.pass.startTime }
        }
        _satPassList.postValue(passList)
    }

    suspend fun getTransmittersForSat(id: Int): List<Transmitter> {
        return repository.getTransmittersForSat(id)
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadDataFromDisk() {
        tleMainList = try {
            val tleStream = app.openFileInput(tleMainListFileName)
            val tleList = ObjectInputStream(tleStream).readObject()
            tleList as List<TLE>
        } catch (exception: FileNotFoundException) {
            _debugMessage.postValue("TLE file wasn't found")
            emptyList()
        } catch (exception: IOException) {
            _debugMessage.postValue(exception.toString())
            emptyList()
        }
        tleSelection = try {
            val selectionStream = app.openFileInput(tleSelectionFileName)
            val selectionList = ObjectInputStream(selectionStream).readObject()
            selectionList as MutableList<Int>
        } catch (exception: FileNotFoundException) {
            _debugMessage.postValue("Selection file wasn't found")
            mutableListOf()
        } catch (exception: IOException) {
            _debugMessage.postValue(exception.toString())
            mutableListOf()
        }
        setGroundStationPosition()
    }
}

fun SharedPreferences.Editor.putDouble(key: String, double: Double) {
    putLong(key, double.toRawBits())
}

fun SharedPreferences.getDouble(key: String, default: Double): Double {
    return Double.fromBits(getLong(key, default.toRawBits()))
}