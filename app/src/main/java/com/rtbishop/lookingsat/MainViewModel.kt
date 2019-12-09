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

@Suppress("UNCHECKED_CAST")
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val defValueLoc = application.getString(R.string.def_gsp_loc)
    private val defValueHours = application.getString(R.string.def_hours_ahead)
    private val defValueMinEl = application.getString(R.string.def_min_el)
    private val keyLat = application.getString(R.string.key_lat)
    private val keyLon = application.getString(R.string.key_lon)
    private val keyAlt = application.getString(R.string.key_alt)
    private val keyHours = application.getString(R.string.key_hours_ahead)
    private val keyMinEl = application.getString(R.string.key_min_el)
    private val tleFileName = application.getString(R.string.tle_file_name)
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
    var tleSelectedMap = mutableMapOf<TLE, Boolean>()
    var passPrefs = SatPassPrefs(
        preferences.getString(keyHours, defValueHours)!!.toInt(),
        preferences.getString(keyMinEl, defValueMinEl)!!.toDouble()
    )

    private fun loadTwoLineElementFile(): List<TLE> {
        try {
            val fileInput = app.openFileInput(tleFileName)
            val objectInput = ObjectInputStream(fileInput)
            val tleList = objectInput.readObject()
            return tleList as List<TLE>
        } catch (e: FileNotFoundException) {
            debugMessage.postValue("TLE file wasn't found")
        } catch (e: IOException) {
            debugMessage.postValue(e.toString())
        }
        return emptyList()
    }

    suspend fun getPasses() {
        val passList = mutableListOf<SatPass>()
        withContext(Dispatchers.Default) {
            tleSelectedMap.forEach { (tle, value) ->
                if (value) {
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
            }
            passList.retainAll { it.pass.maxEl >= passPrefs.maxEl }
            passList.sortBy { it.pass.startTime }
        }
        satPassList = passList
    }

    fun updateSelectedSatMap(mutableMap: MutableMap<TLE, Boolean>) {
        tleSelectedMap = mutableMap
    }

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
                val fileOutStream = app.openFileOutput(tleFileName, Context.MODE_PRIVATE)
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

    suspend fun getTransmittersForSat(id: Int): List<Transmitter> {
        return repository.getTransmittersForSat(id)
    }
}