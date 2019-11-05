package com.rtbishop.lookingsat.vm

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.github.amsacode.predict4java.GroundStationPosition
import com.google.android.gms.location.LocationServices
import com.rtbishop.lookingsat.Injector
import com.rtbishop.lookingsat.repo.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val keyLat = "LATITUDE"
    private val keyLon = "LONGITUDE"
    private val keyHeight = "HEIGHT"

    private val repository: Repository = Injector.provideRepository(application)
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
        val fileName = "tles.txt"
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getApplication<Application>().openFileOutput(fileName, Context.MODE_PRIVATE).use {
                    it.write(repository.fetchTleStream().readBytes())
                }
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