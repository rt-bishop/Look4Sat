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
package com.rtbishop.look4sat.data.repository

import android.content.SharedPreferences
import android.location.LocationManager
import androidx.core.content.edit
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import com.rtbishop.look4sat.domain.model.DatabaseState
import com.rtbishop.look4sat.domain.model.OtherSettings
import com.rtbishop.look4sat.domain.model.PassesSettings
import com.rtbishop.look4sat.domain.predict.GeoPos
import com.rtbishop.look4sat.domain.repository.ISettingsRepo
import com.rtbishop.look4sat.domain.utility.positionToQth
import com.rtbishop.look4sat.domain.utility.qthToPosition
import com.rtbishop.look4sat.domain.utility.round
import java.util.concurrent.Executors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsRepo(private val manager: LocationManager, private val preferences: SharedPreferences) : ISettingsRepo {

    private val keyBluetoothAddress = "bluetoothAddress"
    private val keyBluetoothName = "bluetoothName"
    private val keyBluetoothFormat = "bluetoothFormat"
    private val keyBluetoothState = "bluetoothState"
    private val keyFilterHoursAhead = "filterHoursAhead"
    private val keyFilterMinElevation = "filterMinElevation"
    private val keyNumberOfRadios = "numberOfRadios"
    private val keyNumberOfSatellites = "numberOfSatellites"
    private val keyRotatorAddress = "rotatorAddress"
    private val keyRotatorPort = "rotatorPort"
    private val keyRotatorState = "rotatorState"
    private val keySelectedIds = "selectedIds"
    private val keySelectedModes = "selectedModes"
    private val keyStateOfAutoUpdate = "stateOfAutoUpdate"
    private val keyStateOfSensors = "stateOfSensors"
    private val keyStateOfSweep = "stateOfSweep"
    private val keyStateOfUtc = "stateOfUtc"
    private val keyStationAltitude = "stationAltitude"
    private val keyStationLatitude = "stationLatitude"
    private val keyStationLongitude = "stationLongitude"
    private val keyStationQth = "stationQth"
    private val keyStationTimestamp = "stationTimestamp"
    private val keyUpdateTimestamp = "updateTimestamp"
    private val separatorComma = ","

    //region # Satellites selection settings
    private val _satelliteSelection = MutableStateFlow(getSelectedIds())
    override val selectedIds: StateFlow<List<Int>> = _satelliteSelection

    override fun setSelectedIds(ids: List<Int>) {
        val selectionString = ids.joinToString(separatorComma)
        preferences.edit { putString(keySelectedIds, selectionString) }
        _satelliteSelection.value = ids
    }

    private fun getSelectedIds(): List<Int> {
        val selectionString = preferences.getString(keySelectedIds, null)
        val selectionList = selectionString?.split(separatorComma)?.map { it.toInt() }
        return selectionList ?: emptyList()
    }
    //endregion

    //region # Passes filter settings
    private val _passesSettings = MutableStateFlow(getPassesSettings())
    override val passesSettings: StateFlow<PassesSettings> = _passesSettings

    override fun setPassesSettings(settings: PassesSettings) = preferences.edit {
        putInt(keyFilterHoursAhead, settings.hoursAhead)
        putLong(keyFilterMinElevation, settings.minElevation.toRawBits())
        putString(keySelectedModes, settings.selectedModes.joinToString(separatorComma))
        _passesSettings.value = settings
    }

    private fun getPassesSettings(): PassesSettings {
        val hoursAhead = preferences.getInt(keyFilterHoursAhead, 24)
        val minElevation = Double.fromBits(preferences.getLong(keyFilterMinElevation, 16.0.toRawBits()))
        val selectedModesString = preferences.getString(keySelectedModes, null)
        val selectedModes = selectedModesString?.split(separatorComma)?.sorted() ?: emptyList()
        return PassesSettings(hoursAhead, minElevation, selectedModes)
    }
    //endregion

    //region # Station position settings
    private val _stationPosition = MutableStateFlow(getStationPosition())
    private val executor = Executors.newSingleThreadExecutor()
    private val providerDef = LocationManager.PASSIVE_PROVIDER
    private val providerGps = LocationManager.GPS_PROVIDER
    private val providerNet = LocationManager.NETWORK_PROVIDER
    private val timeoutSignal = CancellationSignal().apply {
        setOnCancelListener { _stationPosition.value = getStationPosition() }
    }
    override val stationPosition: StateFlow<GeoPos> = _stationPosition

    override fun setStationPositionGeo(latitude: Double, longitude: Double, altitude: Double): Boolean {
        val newLongitude = if (longitude > 180.0) longitude - 180 else longitude
        val locator = positionToQth(latitude, newLongitude) ?: return false
        setStationPosition(latitude, newLongitude, altitude, locator)
        return true
    }

    override fun setStationPositionGps(): Boolean {
        if (!LocationManagerCompat.isLocationEnabled(manager)) return false
        try {
            val hasProviderGps = LocationManagerCompat.hasProvider(manager, providerGps)
            val hasProviderNet = LocationManagerCompat.hasProvider(manager, providerNet)
            val provider = if (hasProviderGps) providerGps else if (hasProviderNet) providerNet else providerDef
            println("Requesting location for $provider provider")
            LocationManagerCompat.getCurrentLocation(manager, provider, timeoutSignal, executor) {
                it?.let { setStationPositionGeo(it.latitude, it.longitude, it.altitude) }
            }
        } catch (exception: SecurityException) {
            println("No permissions were given - $exception")
        }
        return true
    }

    override fun setStationPositionQth(locator: String): Boolean {
        val position = qthToPosition(locator) ?: return false
        setStationPosition(position.latitude, position.longitude, 0.0, locator)
        return true
    }

    private fun getStationPosition(): GeoPos {
        val latitude = (preferences.getString(keyStationLatitude, null) ?: "0.0").toDouble()
        val longitude = (preferences.getString(keyStationLongitude, null) ?: "0.0").toDouble()
        val altitude = (preferences.getString(keyStationAltitude, null) ?: "0.0").toDouble()
        val qthLocator = preferences.getString(keyStationQth, null) ?: "JJ00aa"
        val timestamp = preferences.getLong(keyStationTimestamp, 0L)
        return GeoPos(latitude, longitude, altitude, qthLocator, timestamp)
    }

    private fun setStationPosition(latitude: Double, longitude: Double, altitude: Double, locator: String) {
        val newLat = latitude.round(4)
        val newLon = longitude.round(4)
        val newAlt = altitude.round(1)
        val timestamp = System.currentTimeMillis()
        println("Received new Position($newLat, $newLon, $newAlt) & Locator $locator")
        setStationPosition(GeoPos(newLat, newLon, newAlt, locator, timestamp))
    }

    private fun setStationPosition(stationPos: GeoPos) = preferences.edit {
        putString(keyStationLatitude, stationPos.latitude.toString())
        putString(keyStationLongitude, stationPos.longitude.toString())
        putString(keyStationAltitude, stationPos.altitude.toString())
        putString(keyStationQth, stationPos.qthLocator)
        putLong(keyStationTimestamp, stationPos.timestamp)
        _stationPosition.value = stationPos
    }
    //endregion

    //region # Database update settings
    private val _databaseState = MutableStateFlow(getDatabaseState())
    override val databaseState: StateFlow<DatabaseState> = _databaseState

    override fun getSatelliteTypeIds(type: String): List<Int> {
        val typesString = preferences.getString("type$type", null)
        if (typesString.isNullOrBlank()) return emptyList()
        return typesString.split(separatorComma).map { it.toInt() }
    }

    override fun setSatelliteTypeIds(type: String, ids: List<Int>) {
        if (type == "All") return
        val typesString = ids.joinToString(separatorComma)
        preferences.edit { putString("type$type", typesString) }
    }

    override fun updateDatabaseState(state: DatabaseState) = preferences.edit {
        putInt(keyNumberOfSatellites, state.numberOfSatellites)
        putInt(keyNumberOfRadios, state.numberOfRadios)
        putLong(keyUpdateTimestamp, state.updateTimestamp)
        _databaseState.value = state
    }

    private fun getDatabaseState(): DatabaseState {
        val numberOfRadios = preferences.getInt(keyNumberOfRadios, 0)
        val numberOfSatellites = preferences.getInt(keyNumberOfSatellites, 0)
        val updateTimestamp = preferences.getLong(keyUpdateTimestamp, 0L)
        return DatabaseState(numberOfRadios, numberOfSatellites, updateTimestamp)
    }
    //endregion

    //region # Other settings
    private val _otherSettings = MutableStateFlow(getOtherSettings())
    override val otherSettings: StateFlow<OtherSettings> = _otherSettings

    override fun setStateOfAutoUpdate(value: Boolean) {
        preferences.edit { putBoolean(keyStateOfAutoUpdate, value) }
        _otherSettings.value = otherSettings.value.copy(stateOfAutoUpdate = value)
    }

    override fun setStateOfSensors(value: Boolean) {
        preferences.edit { putBoolean(keyStateOfSensors, value) }
        _otherSettings.value = otherSettings.value.copy(stateOfSensors = value)
    }

    override fun setStateOfSweep(value: Boolean) {
        preferences.edit { putBoolean(keyStateOfSweep, value) }
        _otherSettings.value = otherSettings.value.copy(stateOfSweep = value)
    }

    override fun setStateOfUtc(value: Boolean) {
        preferences.edit { putBoolean(keyStateOfUtc, value) }
        _otherSettings.value = otherSettings.value.copy(stateOfUtc = value)
    }

    private fun getOtherSettings(): OtherSettings {
        val stateOfAutoUpdate = preferences.getBoolean(keyStateOfAutoUpdate, true)
        val stateOfSensors = preferences.getBoolean(keyStateOfSensors, true)
        val stateOfSweep = preferences.getBoolean(keyStateOfSweep, true)
        val stateOfUtc = preferences.getBoolean(keyStateOfUtc, false)
        return OtherSettings(stateOfAutoUpdate, stateOfSensors, stateOfSweep, stateOfUtc)
    }
    //endregion

    //region # Undefined settings
    override fun getBluetoothAddress(): String = preferences.getString(keyBluetoothAddress, null) ?: "00:0C:BF:13:80:5D"
    override fun setBluetoothAddress(value: String) = preferences.edit { putString(keyBluetoothAddress, value) }

    override fun getBluetoothFormat(): String = preferences.getString(keyBluetoothFormat, null) ?: "W\$AZ \$EL"
    override fun setBluetoothFormat(value: String) = preferences.edit { putString(keyBluetoothFormat, value) }

    override fun getBluetoothName(): String = preferences.getString(keyBluetoothName, null) ?: "Default"
    override fun setBluetoothName(value: String) = preferences.edit { putString(keyBluetoothName, value) }

    override fun getBluetoothState(): Boolean = preferences.getBoolean(keyBluetoothState, false)
    override fun setBluetoothState(value: Boolean) = preferences.edit { putBoolean(keyBluetoothState, value) }

    override fun getRotatorAddress(): String = preferences.getString(keyRotatorAddress, null) ?: "127.0.0.1"
    override fun setRotatorAddress(value: String) = preferences.edit { putString(keyRotatorAddress, value) }

    override fun getRotatorPort(): String = preferences.getString(keyRotatorPort, null) ?: "4533"
    override fun setRotatorPort(value: String) = preferences.edit { putString(keyRotatorPort, value) }

    override fun getRotatorState(): Boolean = preferences.getBoolean(keyRotatorState, false)
    override fun setRotatorState(value: Boolean) = preferences.edit { putBoolean(keyRotatorState, value) }
    //endregion
}
