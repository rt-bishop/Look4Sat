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

    private val _debugMessage = MutableLiveData("")
    val debugMessage: LiveData<String> = _debugMessage

    private val _gsp = MutableLiveData<GroundStationPosition>(
        GroundStationPosition(
            preferences.getDouble(keyLat, 0.0),
            preferences.getDouble(keyLon, 0.0),
            preferences.getDouble(keyHeight, 0.0)
        )
    )
    val gsp: LiveData<GroundStationPosition> = _gsp

    var tleMainList = loadTwoLineElementFile().sortedWith(compareBy { it.name })
    var tleSelectedMap = mutableMapOf<TLE, Boolean>()

    init {
        (application as LookingSatApp).appComponent.inject(this)
    }

    private fun loadTwoLineElementFile(): List<TLE> {
        return try {
            TLE.importSat(getApplication<Application>().openFileInput(tleFile))
        } catch (exception: FileNotFoundException) {
            _debugMessage.postValue("TLE file wasn't found")
            emptyList()
        }
    }

    suspend fun getPassesForSelectedSatellites(): List<SatPass> {
        val satPassList = mutableListOf<SatPass>()
        var sortedList = listOf<SatPass>()
        withContext(Dispatchers.Default) {
            for ((tle, value) in tleSelectedMap) {
                if (value) {
                    try {
                        Log.d(tag, "Trying ${tle.name}")
                        val passPredictor = PassPredictor(tle, gsp.value)
                        val passes = passPredictor.getPasses(Date(), 6, true)
                        for (pass in passes) {
                            Log.d(tag, "Trying ${pass.maxEl}")
                            satPassList.add(SatPass(tle.name, tle.catnum, pass))
                        }
                    } catch (exception: IllegalArgumentException) {
                        val tleProblem = "There was a problem with TLE"
                        Log.d(tag, tleProblem)
                        _debugMessage.postValue(tleProblem)
                    } catch (exception: SatNotFoundException) {
                        Log.d(tag, "Certain satellites shall not pass")
                    }
                }
            }
            sortedList = satPassList.sortedWith(compareBy { it.pass.startTime })
        }
        return sortedList
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
                tleMainList = TLE.importSat(stream)
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