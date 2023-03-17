/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2022 Arty Bishop (bishop.arty@gmail.com)
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

import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import com.rtbishop.look4sat.domain.ILocationSource
import com.rtbishop.look4sat.domain.ISettingsSource
import com.rtbishop.look4sat.model.StationPos
import com.rtbishop.look4sat.utility.QthConverter
import com.rtbishop.look4sat.utility.round
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

class LocationSource(
    private val manager: LocationManager,
    private val settings: ISettingsSource,
) : ILocationSource, LocationListenerCompat {

    private val defaultProvider = LocationManager.PASSIVE_PROVIDER
    private val _stationPosition = MutableStateFlow(settings.loadStationPosition())
    override val stationPosition: StateFlow<StationPos> = _stationPosition

    override fun setGpsPosition(): Boolean {
        if (!LocationManagerCompat.isLocationEnabled(manager)) return false
        val criteria = Criteria().apply { isCostAllowed = true }
        val bestProvider = manager.getBestProvider(criteria, true) ?: defaultProvider
        forceLocationUpdate(bestProvider)
        return true
    }

    override fun setGeoPosition(latitude: Double, longitude: Double): Boolean {
        manager.removeUpdates(this)
        val newLongitude = if (longitude > 180.0) longitude - 180 else longitude
        val locator = QthConverter.positionToQth(latitude, newLongitude) ?: return false
        saveStationPosition(latitude, newLongitude, locator)
        return true
    }

    override fun setQthPosition(locator: String): Boolean {
        val position = QthConverter.qthToPosition(locator) ?: return false
        saveStationPosition(position.lat, position.lon, locator)
        return true
    }

    override fun onLocationChanged(location: Location) {
        setGeoPosition(location.latitude, location.longitude)
    }

    private fun forceLocationUpdate(provider: String) {
        try {
            val location = manager.getLastKnownLocation(defaultProvider)
            if (location == null || System.currentTimeMillis() - location.time > 600000) {
                Timber.d("Requesting $provider location update")
                manager.requestLocationUpdates(provider, 0L, 0f, this)
            } else {
                setGeoPosition(location.latitude, location.longitude)
            }
        } catch (exception: SecurityException) {
            Timber.d("No permissions were given")
        }
    }

    private fun saveStationPosition(latitude: Double, longitude: Double, locator: String) {
        val newLat = latitude.round(4)
        val newLon = longitude.round(4)
        Timber.d("Received new Position($newLat, $newLon) & Locator $locator")
        _stationPosition.value = StationPos(newLat, newLon, locator, System.currentTimeMillis())
        settings.saveStationPosition(stationPosition.value)
    }
}
