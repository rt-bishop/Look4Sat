/*
 * Look4Sat. Amateur radio & weather satellites passes calculator for Android.
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

    private val app = application
    private val keyHours = application.getString(R.string.pref_hours_ahead_key)
    private val keyMinEl = application.getString(R.string.pref_min_el_key)
    private val keyLat = application.getString(R.string.pref_lat_key)
    private val keyLon = application.getString(R.string.pref_lon_key)
    private val keyAlt = application.getString(R.string.pref_alt_key)
    private val keyDelay = application.getString(R.string.pref_refresh_rate_key)
    private val tleMainListFileName = "tleFile.txt"
    private val tleSelectionFileName = "tleSelection"

    private val _debugMessage = MutableLiveData<String>()
    val debugMessage: LiveData<String> = _debugMessage

    private val _satPassList = MutableLiveData<MutableList<SatPass>>()
    val passSatList: LiveData<MutableList<SatPass>> = _satPassList

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
        get() = preferences.getString(keyDelay, "3000")!!.toLong()

    val hoursAhead: Int
        get() = preferences.getInt(keyHours, 8)

    val minEl: Double
        get() = preferences.getDouble(keyMinEl, 16.0)

    fun setGroundStationPosition() {
        val lat = preferences.getString(keyLat, "0.0")!!.toDouble()
        val lon = preferences.getString(keyLon, "0.0")!!.toDouble()
        val alt = preferences.getString(keyAlt, "0.0")!!.toDouble()
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
            _debugMessage.postValue(app.getString(R.string.update_loc_success))
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

    fun getPasses() {
        val passList = mutableListOf<SatPass>()
        val dateNow = Date()
        val dateFuture = Calendar.getInstance().let {
            it.time = dateNow
            it.add(Calendar.HOUR, hoursAhead)
            it.time
        }
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                tleSelection.forEach { indexOfSelection ->
                    val tle = tleMainList[indexOfSelection]
                    try {
                        val predictor = PassPredictor(tle, gsp.value)
                        val passes = predictor.getPasses(dateNow, hoursAhead, true)
                        passes.forEach {
                            passList.add(SatPass(tle, predictor, it))
                        }
                    } catch (exception: IllegalArgumentException) {
                        _debugMessage.postValue(app.getString(R.string.err_parse_tle))
                    } catch (exception: SatNotFoundException) {
                        _debugMessage.postValue(app.getString(R.string.err_sat_wont_pass))
                    }
                }
                passList.removeAll { it.pass.startTime.after(dateFuture) }
                passList.removeAll { it.pass.endTime.before(dateNow) }
                passList.removeAll { it.pass.maxEl < minEl }
                passList.sortBy { it.pass.startTime }
            }
            _satPassList.postValue(passList)
        }
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
            _debugMessage.postValue(app.getString(R.string.err_no_tle_file))
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
            _debugMessage.postValue(app.getString(R.string.err_no_selection_file))
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