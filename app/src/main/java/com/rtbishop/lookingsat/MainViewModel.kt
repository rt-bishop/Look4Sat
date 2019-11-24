package com.rtbishop.lookingsat

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.github.amsacode.predict4java.GroundStationPosition
import com.github.amsacode.predict4java.PassPredictor
import com.github.amsacode.predict4java.SatNotFoundException
import com.github.amsacode.predict4java.TLE
import com.google.android.gms.location.LocationServices
import com.rtbishop.lookingsat.repo.Repository
import com.rtbishop.lookingsat.repo.SatPass
import com.rtbishop.lookingsat.repo.SatPassPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*
import javax.inject.Inject

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val keyLat = "LATITUDE"
    private val keyLon = "LONGITUDE"
    private val keyHeight = "HEIGHT"
    private val tleFile = "tles.txt"
    private val tag = "myTag"

    @Inject
    lateinit var repository: Repository

    private val preferences = PreferenceManager.getDefaultSharedPreferences(application)
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    init {
        (application as LookingSatApp).appComponent.inject(this)
    }

    private val _debugMessage = MutableLiveData("")
    val debugMessage: LiveData<String> = _debugMessage

    private val _tleMainList = MutableLiveData<List<TLE>>(
        try {
            TLE.importSat(getApplication<Application>().openFileInput(tleFile))
                .sortedWith(compareBy { it.name })
        } catch (exception: FileNotFoundException) {
            _debugMessage.postValue("TLE file wasn't found")
            emptyList<TLE>()
        }

    )
    val tleMainList: LiveData<List<TLE>> = _tleMainList

    private val _tleSelectedMap = MutableLiveData<MutableMap<TLE, Boolean>>(mutableMapOf())
    val tleSelectedMap: LiveData<MutableMap<TLE, Boolean>> = _tleSelectedMap

    private val _satPassPrefs = MutableLiveData<SatPassPrefs>(
        SatPassPrefs(
            preferences.getInt("hoursAhead", 8),
            preferences.getDouble("maxEl", 20.0)
        )
    )
    val satPassPrefs: LiveData<SatPassPrefs> = _satPassPrefs

    private val _gsp = MutableLiveData<GroundStationPosition>(
        GroundStationPosition(
            preferences.getDouble(keyLat, 0.0),
            preferences.getDouble(keyLon, 0.0),
            preferences.getDouble(keyHeight, 0.0)
        )
    )
    val gsp: LiveData<GroundStationPosition> = _gsp

    suspend fun getPasses(satMap: Map<TLE, Boolean>, hours: Int, maxEl: Double): List<SatPass> {
        val satPassList = mutableListOf<SatPass>()
        withContext(Dispatchers.Default) {
            satMap.forEach { (tle, value) ->
                if (value) {
                    try {
                        val passPredictor = PassPredictor(tle, gsp.value)
                        val passes = passPredictor.getPasses(Date(), hours, false)
                        passes.forEach { satPassList.add(SatPass(tle.name, tle.catnum, it)) }
                    } catch (exception: IllegalArgumentException) {
                        Log.d(tag, "There was a problem with TLE")
                    } catch (exception: SatNotFoundException) {
                        Log.d(tag, "Certain satellites shall not pass")
                    }
                }
            }
            satPassList.retainAll { it.pass.maxEl >= maxEl }
            satPassList.sortBy { it.pass.startTime }
        }
        return satPassList
    }

    fun updateSelectedSatMap(mutableMap: MutableMap<TLE, Boolean>) {
        _tleSelectedMap.postValue(mutableMap)
    }

    fun updatePassPrefs(hoursAhead: Int, maxEl: Double) {
        _satPassPrefs.postValue(SatPassPrefs(hoursAhead, maxEl))
        preferences.edit {
            putInt("hoursAhead", hoursAhead)
            putDouble("maxEl", maxEl)
            apply()
        }
    }

    fun updateLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            val lat = location?.latitude ?: 0.0
            val lon = location?.longitude ?: 0.0
            val height = location?.altitude ?: 0.0

            preferences.edit {
                putDouble(keyLat, lat)
                putDouble(keyLon, lon)
                putDouble(keyHeight, height)
                apply()
            }
            _gsp.postValue(GroundStationPosition(lat, lon, height))
            _debugMessage.postValue("Location was updated")
        }
    }

    fun updateTwoLineElementFile() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val stream = repository.fetchTleStream()
                getApplication<Application>().openFileOutput(tleFile, Context.MODE_PRIVATE).use {
                    it.write(stream.readBytes())
                }
                _tleMainList.postValue(TLE.importSat(stream))
                _debugMessage.postValue("TLE file was updated")
            } catch (exception: IOException) {
                _debugMessage.postValue("Couldn't update TLE file")
            }
        }
    }

    fun updateTransmittersDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updateTransmittersDatabase()
                _debugMessage.postValue("Transmitters were updated")
            } catch (exception: IOException) {
                _debugMessage.postValue("Couldn't update transmitters")
            }
        }
    }
}

fun SharedPreferences.Editor.putDouble(key: String, double: Double): SharedPreferences.Editor =
    putLong(key, java.lang.Double.doubleToRawLongBits(double))

fun SharedPreferences.getDouble(key: String, default: Double) =
    java.lang.Double.longBitsToDouble(getLong(key, java.lang.Double.doubleToRawLongBits(default)))