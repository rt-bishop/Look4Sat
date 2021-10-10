package com.rtbishop.look4sat.framework

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.rtbishop.look4sat.domain.LocationProvider
import com.rtbishop.look4sat.domain.QthConverter
import com.rtbishop.look4sat.domain.predict.GeoPos
import com.rtbishop.look4sat.utility.round
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: SharedPreferences
) : LocationListener, LocationProvider {

    private val locManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val _updatedLocation = MutableSharedFlow<GeoPos?>()
    override val updatedLocation: SharedFlow<GeoPos?> = _updatedLocation

    fun getMagDeclination(stationPos: GeoPos, time: Long = System.currentTimeMillis()): Float {
        val lat = stationPos.latitude.toFloat()
        val lon = stationPos.longitude.toFloat()
        return GeomagneticField(lat, lon, 0f, time).declination
    }

    fun updatePosition(latitude: Double, longitude: Double) {
        if (QthConverter.isValidPosition(latitude, longitude)) {
            _updatedLocation.tryEmit(GeoPos(latitude, longitude))
        } else _updatedLocation.tryEmit(null)
    }

    fun updatePositionFromQth(qthString: String) {
        val position = QthConverter.qthToPosition(qthString)
        if (position != null) {
            _updatedLocation.tryEmit(GeoPos(position.latitude, position.longitude))
        } else _updatedLocation.tryEmit(null)
    }

    fun updatePositionFromGps() {
        val provider = LocationManager.GPS_PROVIDER
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        val result = ContextCompat.checkSelfPermission(context, permission)
        if (locManager.isProviderEnabled(provider) && result == PackageManager.PERMISSION_GRANTED) {
            locManager.requestLocationUpdates(provider, 0L, 0f, this)
        } else _updatedLocation.tryEmit(null)
    }

    fun updatePositionFromNetwork() {
        val provider = LocationManager.NETWORK_PROVIDER
        val permission = Manifest.permission.ACCESS_COARSE_LOCATION
        val result = ContextCompat.checkSelfPermission(context, permission)
        if (locManager.isProviderEnabled(provider) && result == PackageManager.PERMISSION_GRANTED) {
            locManager.requestLocationUpdates(provider, 0L, 0f, this)
        } else _updatedLocation.tryEmit(null)
    }

    override fun onLocationChanged(location: Location) {
        locManager.removeUpdates(this)
        val latitude = location.latitude.round(4)
        val longitude = location.longitude.round(4)
        _updatedLocation.tryEmit(GeoPos(latitude, longitude))
    }
}
