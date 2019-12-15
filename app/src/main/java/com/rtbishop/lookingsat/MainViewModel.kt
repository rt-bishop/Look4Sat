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

package com.rtbishop.lookingsat

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.amsacode.predict4java.GroundStationPosition
import com.github.amsacode.predict4java.PassPredictor
import com.github.amsacode.predict4java.SatNotFoundException
import com.github.amsacode.predict4java.TLE
import com.google.android.gms.location.FusedLocationProviderClient
import com.rtbishop.lookingsat.repo.Repository
import com.rtbishop.lookingsat.repo.SatPass
import com.rtbishop.lookingsat.repo.SatPassPrefs
import com.rtbishop.lookingsat.repo.Transmitter
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
    private val defValueHours = application.getString(R.string.def_hours_ahead)
    private val defValueMinEl = application.getString(R.string.def_min_el)
    private val defValueUpdateFreq = application.getString(R.string.def_update_freq)
    private val keyLat = application.getString(R.string.key_lat)
    private val keyLon = application.getString(R.string.key_lon)
    private val keyAlt = application.getString(R.string.key_alt)
    private val keyHours = application.getString(R.string.key_hours_ahead)
    private val keyMinEl = application.getString(R.string.key_min_el)
    private val keyUpdateFreq = application.getString(R.string.key_update_freq)
    private val tleMainListFileName = application.getString(R.string.tle_main_list_file_name)
    private val tleSelectionFileName = application.getString(R.string.tle_selection_file_name)
    private val app = application

    @Inject
    lateinit var locationClient: FusedLocationProviderClient
    @Inject
    lateinit var preferences: SharedPreferences
    @Inject
    lateinit var repository: Repository

    init {
        (app as LookingSatApp).appComponent.inject(this)
    }

    private var passPrefs = SatPassPrefs(
        preferences.getString(keyHours, defValueHours)!!.toInt(),
        preferences.getString(keyMinEl, defValueMinEl)!!.toDouble()
    )
    val debugMessage = MutableLiveData("")
    val gsp = MutableLiveData<GroundStationPosition>(
        GroundStationPosition(
            preferences.getString(keyLat, defValueLoc)!!.toDouble(),
            preferences.getString(keyLon, defValueLoc)!!.toDouble(),
            preferences.getString(keyAlt, defValueLoc)!!.toDouble()
        )
    )
    var satPassList = emptyList<SatPass>()
    var tleMainList = loadTwoLineElementFile()
    var selectionList = loadSelectionList()
    val updateFreq
        get() = preferences.getString(keyUpdateFreq, defValueUpdateFreq)!!.toLong()

    fun updateGsp() {
        val lat = preferences.getString(keyLat, defValueLoc)!!.toDouble()
        val lon = preferences.getString(keyLon, defValueLoc)!!.toDouble()
        val alt = preferences.getString(keyAlt, defValueLoc)!!.toDouble()
        gsp.postValue(GroundStationPosition(lat, lon, alt))
    }

    fun updatePassPref() {
        passPrefs = SatPassPrefs(
            preferences.getString(keyHours, defValueHours)!!.toInt(),
            preferences.getString(keyMinEl, defValueMinEl)!!.toDouble()
        )
    }

    fun updateLocation() {
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
            gsp.postValue(GroundStationPosition(lat, lon, alt))
            debugMessage.postValue("Location was updated")
        }
    }

    fun updateAndSaveTleFile() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = repository.fetchTleStream()
                val tleList = TLE.importSat(inputStream).apply { sortBy { it.name } }
                val fileOutStream = app.openFileOutput(tleMainListFileName, Context.MODE_PRIVATE)
                ObjectOutputStream(fileOutStream).apply {
                    writeObject(tleList)
                    flush()
                    close()
                }
                tleMainList = tleList
                debugMessage.postValue("TLE file was updated")
            } catch (exception: IOException) {
                debugMessage.postValue("Couldn't update TLE file")
            }
        }
    }

    fun updateTransmittersDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updateTransmittersDatabase()
                debugMessage.postValue("Transmitters were updated")
            } catch (exception: IOException) {
                debugMessage.postValue("Couldn't update transmitters")
            }
        }
    }

    fun updateAndSaveSelectionList(list: MutableList<Int>) {
        selectionList = list
        try {
            val fileOutputStream = app.openFileOutput(tleSelectionFileName, Context.MODE_PRIVATE)
            ObjectOutputStream(fileOutputStream).apply {
                writeObject(list)
                flush()
                close()
            }
        } catch (exception: IOException) {
            debugMessage.postValue(exception.toString())
        }
    }

    suspend fun getPasses() {
        val passList = mutableListOf<SatPass>()
        withContext(Dispatchers.Default) {
            selectionList.forEach { indexOfSelection ->
                val tle = tleMainList[indexOfSelection]
                try {
                    val predictor = PassPredictor(tle, gsp.value)
                    val passes = predictor.getPasses(Date(), passPrefs.hoursAhead, true)
                    passes.forEach { passList.add(SatPass(tle, predictor, it)) }
                } catch (exception: IllegalArgumentException) {
                    debugMessage.postValue("There was a problem with ${tle.name}")
                } catch (exception: SatNotFoundException) {
                    debugMessage.postValue("${tle.name} shall not pass")
                }
            }
            passList.retainAll { it.pass.maxEl >= passPrefs.maxEl }
            passList.sortBy { it.pass.startTime }
        }
        satPassList = passList
    }

    suspend fun getTransmittersForSat(id: Int): List<Transmitter> {
        return repository.getTransmittersForSat(id)
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadTwoLineElementFile(): List<TLE> {
        try {
            val fileInputStream = app.openFileInput(tleMainListFileName)
            val tleList = ObjectInputStream(fileInputStream).readObject()
            return tleList as List<TLE>
        } catch (exception: FileNotFoundException) {
            debugMessage.postValue("TLE file wasn't found")
        } catch (exception: IOException) {
            debugMessage.postValue(exception.toString())
        }
        return emptyList()
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadSelectionList(): MutableList<Int> {
        try {
            val fileInputStream = app.openFileInput(tleSelectionFileName)
            val selectionList = ObjectInputStream(fileInputStream).readObject()
            return selectionList as MutableList<Int>
        } catch (exception: FileNotFoundException) {
            debugMessage.postValue("Selection file wasn't found")
        } catch (exception: IOException) {
            debugMessage.postValue(exception.toString())
        }
        return mutableListOf()
    }
}