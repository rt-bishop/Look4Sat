package com.rtbishop.lookingsat.vm

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.widget.Toast
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

    private val repository: Repository = Injector.provideRepository(application)
    private val preferences = PreferenceManager.getDefaultSharedPreferences(application)
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
    private val _gsp = MutableLiveData<GroundStationPosition>()
    val gsp: LiveData<GroundStationPosition> = _gsp

    fun updateLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            val lat = location?.latitude ?: 51.5074
            val lon = location?.longitude ?: 0.1278
            val height = location?.altitude ?: 48.0

            preferences.edit {
                putDouble("LATITUDE", lat)
                putDouble("LONGITUDE", lon)
                putDouble("HEIGHT", height)
                apply()
            }
            _gsp.postValue(GroundStationPosition(lat, lon, height))
        }
    }

    fun updateTwoLineElementFile() {
        val fileName = "tles.txt"
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getApplication<Application>().openFileOutput(fileName, Context.MODE_PRIVATE).use {
                    it.write(repository.fetchTleStream().readBytes())
                }
                Toast.makeText(getApplication(), "TLE file was updated", Toast.LENGTH_SHORT).show()
            } catch (exception: IOException) {
                Toast.makeText(getApplication(), "Could not update TLE", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun updateTransmittersDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updateTransmittersDatabase()
                Toast.makeText(getApplication(), "", Toast.LENGTH_SHORT).show()
            } catch (exception: IOException) {
                Toast.makeText(getApplication(), "", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

fun SharedPreferences.Editor.putDouble(key: String, double: Double): SharedPreferences.Editor =
    putLong(key, java.lang.Double.doubleToRawLongBits(double))

fun SharedPreferences.getDouble(key: String, default: Double) =
    java.lang.Double.longBitsToDouble(getLong(key, java.lang.Double.doubleToRawLongBits(default)))