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
import android.location.LocationManager
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import com.rtbishop.look4sat.domain.ILocationSource
import com.rtbishop.look4sat.domain.ISettingsSource
import com.rtbishop.look4sat.model.GeoPos
import com.rtbishop.look4sat.utility.QthConverter
import com.rtbishop.look4sat.utility.round
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import java.util.concurrent.Executors

class LocationSource(
    private val manager: LocationManager,
    private val settings: ISettingsSource,
) : ILocationSource {

    private val _stationPosition = MutableStateFlow(settings.loadStationPosition())
    private val defaultProvider = LocationManager.PASSIVE_PROVIDER
    private val executor = Executors.newSingleThreadExecutor()
    private val timeoutSignal = CancellationSignal().apply {
        setOnCancelListener { _stationPosition.value = _stationPosition.value }
    }
    override val stationPosition: StateFlow<GeoPos> = _stationPosition

    override fun setGpsPosition(): Boolean {
        if (!LocationManagerCompat.isLocationEnabled(manager)) return false
        try {
            val criteria = Criteria().apply { isCostAllowed = true }
            val provider = manager.getBestProvider(criteria, true) ?: defaultProvider
            Timber.d("Requesting location for $provider provider")
            LocationManagerCompat.getCurrentLocation(manager, provider, timeoutSignal, executor) {
                it?.let { setGeoPosition(it.latitude, it.longitude, it.altitude) }
            }
        } catch (exception: SecurityException) {
            Timber.d("No permissions were given")
        }
        return true
    }

    override fun setGeoPosition(latitude: Double, longitude: Double, altitude: Double): Boolean {
        val newLongitude = if (longitude > 180.0) longitude - 180 else longitude
        val locator = QthConverter.positionToQth(latitude, newLongitude) ?: return false
        saveGeoPos(latitude, newLongitude, altitude, locator)
        return true
    }

    override fun setQthPosition(locator: String): Boolean {
        val position = QthConverter.qthToPosition(locator) ?: return false
        saveGeoPos(position.latitude, position.longitude, 0.0, locator)
        return true
    }

    private fun saveGeoPos(latitude: Double, longitude: Double, altitude: Double, locator: String) {
        val newLat = latitude.round(4)
        val newLon = longitude.round(4)
        val newAlt = altitude.round(1)
        val timestamp = System.currentTimeMillis()
        Timber.d("Received new Position($newLat, $newLon, $newAlt) & Locator $locator")
        _stationPosition.value = GeoPos(newLat, newLon, newAlt, locator, timestamp)
        settings.saveStationPosition(stationPosition.value)
    }
}
