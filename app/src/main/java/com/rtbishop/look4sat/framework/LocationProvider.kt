/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2021 Arty Bishop (bishop.arty@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.rtbishop.look4sat.framework

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.domain.LocationHandler
import com.rtbishop.look4sat.domain.QthConverter
import com.rtbishop.look4sat.domain.model.DataState
import com.rtbishop.look4sat.domain.predict.GeoPos
import com.rtbishop.look4sat.presentation.round
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsProvider: SettingsProvider,
) : LocationListener, LocationHandler {

    private val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val providerDef = LocationManager.PASSIVE_PROVIDER
    private val providerNet = LocationManager.NETWORK_PROVIDER
    private val providerGps = LocationManager.GPS_PROVIDER
    private val locationCoarse = Manifest.permission.ACCESS_COARSE_LOCATION
    private val locationFine = Manifest.permission.ACCESS_FINE_LOCATION
    private val _stationPosition = MutableSharedFlow<DataState<GeoPos>>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private var currentPosition = settingsProvider.loadStationPosition()
    override val stationPosition: SharedFlow<DataState<GeoPos>> = _stationPosition

    override fun getStationPosition(): GeoPos = currentPosition

    override fun setStationPosition(latitude: Double, longitude: Double) {
        if (QthConverter.isValidPosition(latitude, longitude)) {
            val newLat = latitude.round(4)
            val newLon = longitude.round(4)
            currentPosition = GeoPos(newLat, newLon)
            settingsProvider.saveStationPosition(newLat, newLon)
            _stationPosition.tryEmit(DataState.Success(currentPosition))
        } else _stationPosition.tryEmit(DataState.Error(context.getString(R.string.pref_pos_gps_null)))
    }

    override fun setPositionFromGps() {
        val result = ContextCompat.checkSelfPermission(context, locationFine)
        if (manager.isProviderEnabled(providerGps) && result == PackageManager.PERMISSION_GRANTED) {
            val location = manager.getLastKnownLocation(providerDef)
            if (location != null) {
                setStationPosition(location.latitude, location.longitude)
            } else {
                _stationPosition.tryEmit(DataState.Loading)
                manager.requestLocationUpdates(providerGps, 0L, 0f, this)
            }
        } else _stationPosition.tryEmit(DataState.Error(context.getString(R.string.pref_pos_gps_null)))
    }

    override fun setPositionFromNet() {
        val result = ContextCompat.checkSelfPermission(context, locationCoarse)
        if (manager.isProviderEnabled(providerNet) && result == PackageManager.PERMISSION_GRANTED) {
            val location = manager.getLastKnownLocation(providerDef)
            if (location != null) {
                setStationPosition(location.latitude, location.longitude)
            } else {
                _stationPosition.tryEmit(DataState.Loading)
                manager.requestLocationUpdates(providerNet, 0L, 0f, this)
            }
        } else _stationPosition.tryEmit(DataState.Error(context.getString(R.string.pref_pos_gps_null)))
    }

    override fun setPositionFromQth(qthString: String) {
        val position = QthConverter.qthToPosition(qthString)
        if (position != null) {
            setStationPosition(position.latitude, position.longitude)
        } else _stationPosition.tryEmit(DataState.Error(context.getString(R.string.pref_pos_qth_error)))
    }

    override fun onLocationChanged(location: Location) {
        manager.removeUpdates(this)
        setStationPosition(location.latitude, location.longitude)
    }
}
