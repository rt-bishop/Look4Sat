/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2026 Arty Bishop and contributors.
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
import android.location.Location
import android.location.LocationManager
import androidx.core.content.edit
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import com.rtbishop.look4sat.domain.model.DatabaseState
import com.rtbishop.look4sat.domain.model.OtherSettings
import com.rtbishop.look4sat.domain.model.PassesSettings
import com.rtbishop.look4sat.domain.model.RCSettings
import com.rtbishop.look4sat.domain.model.DataSourcesSettings
import com.rtbishop.look4sat.domain.predict.GeoPos
import com.rtbishop.look4sat.domain.repository.ISettingsRepo
import com.rtbishop.look4sat.domain.utility.positionToQth
import com.rtbishop.look4sat.domain.utility.qthToPosition
import com.rtbishop.look4sat.domain.utility.round
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class SettingsRepo(
    private val locationManager: LocationManager,
    private val preferences: SharedPreferences
) : ISettingsRepo, LocationListenerCompat {

    private val keyBluetoothRotatorAddress = "bluetoothAddress"
    private val keyBluetoothRotatorName = "bluetoothName"
    private val keyBluetoothRotatorFormat = "bluetoothFormat"
    private val keyBluetoothRotatorState = "bluetoothState"
    private val keyBluetoothFrequencyState = "bluetoothFrequencyState"
    private val keyBluetoothFrequencyAddress = "bluetoothFrequencyAddress"
    private val keyBluetoothFrequencyFormat = "bluetoothFrequencyFormat"
    private val keyFilterHoursAhead = "filterHoursAhead"
    private val keyFilterMinElevation = "filterMinElevation"
    private val keyNumberOfRadios = "numberOfRadios"
    private val keyNumberOfSatellites = "numberOfSatellites"
    private val keyRotatorAddress = "rotatorAddress"
    private val keyRotatorPort = "rotatorPort"
    private val keyRotatorState = "rotatorState"
    private val keyRotatorFormat = "rotatorFormat"
    private val keyFrequencyState = "frequencyState"
    private val keyFrequencyAddress = "frequencyAddress"
    private val keyFrequencyPort = "frequencyPort"
    private val keyFrequencyFormat = "frequencyFormat"
    private val keySelectedIds = "selectedIds"
    private val keySelectedTypes = "selectedTypes"
    private val keySelectedModes = "selectedModes"
    private val keyStateOfAutoUpdate = "stateOfAutoUpdate"
    private val keyStateOfSensors = "stateOfSensors"
    private val keyStateOfSweep = "stateOfSweep"
    private val keyStateOfUtc = "stateOfUtc"
    private val keyStateOfLightTheme = "stateOfLightTheme"
    private val keyStationAltitude = "stationAltitude"
    private val keyStationLatitude = "stationLatitude"
    private val keyStationLongitude = "stationLongitude"
    private val keyStationQth = "stationQth"
    private val keyStationTimestamp = "stationTimestamp"
    private val keyUpdateTimestamp = "updateTimestamp"
    private val keyShouldSeeWarning = "shouldSeeWarning"
    private val keyShouldSeeWelcome = "shouldSeeWelcome"
    private val keyUseCustomTle = "useCustomTle"
    private val keyUseCustomTransceivers = "useCustomTransceivers"
    private val keyTleUrl = "tleUrl"
    private val keyTransceiversUrl = "transceiversUrl"
    private val separatorComma = ","

    //region # Satellites selection settings
    private val _satelliteSelection = MutableStateFlow(getSelectedIds())
    private val _typesSelection = MutableStateFlow(getSelectedTypes())
    override val selectedIds: StateFlow<List<Int>> = _satelliteSelection
    override val selectedTypes: StateFlow<List<String>> = _typesSelection

    override fun setSelectedIds(ids: List<Int>) {
        val selectionString = ids.joinToString(separatorComma)
        preferences.edit { putString(keySelectedIds, selectionString) }
        _satelliteSelection.value = ids
    }

    override fun setSelectedTypes(types: List<String>) {
        val typesString = types.joinToString(separatorComma)
        preferences.edit { putString(keySelectedTypes, typesString) }
        _typesSelection.value = types
    }

    private fun getSelectedIds(): List<Int> {
        val selectionString = preferences.getString(keySelectedIds, null)
        if (selectionString.isNullOrEmpty()) return emptyList()
        return selectionString.split(separatorComma).map { it.toInt() }
    }

    private fun getSelectedTypes(): List<String> {
        val typesString = preferences.getString(keySelectedTypes, null)
        if (typesString.isNullOrEmpty()) return listOf("Amateur")
        return typesString.split(separatorComma)
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
    private val providerDef = LocationManager.PASSIVE_PROVIDER
    private val providerGps = LocationManager.GPS_PROVIDER
    private val providerNet = LocationManager.NETWORK_PROVIDER
    override val stationPosition: StateFlow<GeoPos> = _stationPosition

    override fun onLocationChanged(location: Location) {
        setStationPosition(location.latitude, location.longitude, location.altitude)
    }

    override fun setStationPosition(latitude: Double, longitude: Double, altitude: Double): Boolean {
        val newLongitude = if (longitude > 180.0) longitude - 180 else longitude
        val locator = positionToQth(latitude, newLongitude) ?: return false
        setStationPosition(latitude, newLongitude, altitude, locator)
        return true
    }

    override fun setStationPosition(): Boolean {
        if (!LocationManagerCompat.isLocationEnabled(locationManager)) return false
        try {
            val hasGps = LocationManagerCompat.hasProvider(locationManager, providerGps)
            val hasNet = LocationManagerCompat.hasProvider(locationManager, providerNet)
            val provider = if (hasGps) providerGps else if (hasNet) providerNet else providerDef
            val location = locationManager.getLastKnownLocation(providerDef)
            if (location == null || System.currentTimeMillis() - location.time > 600_000L) {
                println("Requesting location for $provider provider")
                locationManager.requestLocationUpdates(provider, 0L, 0f, this)
            } else {
                setStationPosition(location.latitude, location.longitude, location.altitude)
            }
        } catch (exception: SecurityException) {
            println("No permissions were given - $exception")
        }
        return true
    }

    override fun setStationPosition(locator: String): Boolean {
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

    override fun getSatelliteTypesIds(types: List<String>): List<Int> {
        val idsSet = mutableSetOf<Int>()
        types.forEach { type ->
            val typeString = preferences.getString("type$type", null)
            val typeIds = if (typeString.isNullOrBlank()) {
                emptyList()
            } else {
                typeString.split(separatorComma).map { it.toInt() }
            }
            idsSet.addAll(typeIds)
        }
        return idsSet.toList()
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

    //region # RC settings
    private val _rcSettings = MutableStateFlow(getRCSettings())
    override val rcSettings: StateFlow<RCSettings> = _rcSettings

    override fun setBluetoothRotatorAddress(value: String) {
        preferences.edit { putString(keyBluetoothRotatorAddress, value) }
        _rcSettings.update { it.copy(bluetoothRotatorAddress = value) }
    }

    override fun setBluetoothRotatorFormat(value: String) {
        preferences.edit { putString(keyBluetoothRotatorFormat, value) }
        _rcSettings.update { it.copy(bluetoothRotatorFormat = value) }
    }

    override fun setBluetoothRotatorName(value: String) {
        preferences.edit { putString(keyBluetoothRotatorName, value) }
        _rcSettings.update { it.copy(bluetoothRotatorName = value) }
    }

    override fun setBluetoothRotatorState(value: Boolean) {
        preferences.edit { putBoolean(keyBluetoothRotatorState, value) }
        _rcSettings.update { it.copy(bluetoothRotatorState = value) }
    }

    override fun setBluetoothFrequencyState(value: Boolean) {
        preferences.edit { putBoolean(keyBluetoothFrequencyState, value) }
        _rcSettings.update { it.copy(bluetoothFrequencyState = value) }
    }

    override fun setBluetoothFrequencyAddress(value: String) {
        preferences.edit { putString(keyBluetoothFrequencyAddress, value) }
        _rcSettings.update { it.copy(bluetoothFrequencyAddress = value) }
    }

    override fun setBluetoothFrequencyFormat(value: String) {
        preferences.edit { putString(keyBluetoothFrequencyFormat, value) }
        _rcSettings.update { it.copy(bluetoothFrequencyFormat = value) }
    }

    override fun setRotatorAddress(value: String) {
        preferences.edit { putString(keyRotatorAddress, value) }
        _rcSettings.update { it.copy(rotatorAddress = value) }
    }

    override fun setRotatorPort(value: String) {
        preferences.edit { putString(keyRotatorPort, value) }
        _rcSettings.update { it.copy(rotatorPort = value) }
    }

    override fun setRotatorState(value: Boolean) {
        preferences.edit { putBoolean(keyRotatorState, value) }
        _rcSettings.update { it.copy(rotatorState = value) }
    }

    override fun setRotatorFormat(value: String) {
        preferences.edit { putString(keyRotatorFormat, value) }
        _rcSettings.update { it.copy(rotatorFormat = value) }
    }

    override fun setFrequencyState(value: Boolean) {
        preferences.edit { putBoolean(keyFrequencyState, value) }
        _rcSettings.update { it.copy(frequencyState = value) }
    }

    override fun setFrequencyAddress(value: String) {
        preferences.edit { putString(keyFrequencyAddress, value) }
        _rcSettings.update { it.copy(frequencyAddress = value) }
    }

    override fun setFrequencyPort(value: String) {
        preferences.edit { putString(keyFrequencyPort, value) }
        _rcSettings.update { it.copy(frequencyPort = value) }
    }

    override fun setFrequencyFormat(value: String) {
        preferences.edit { putString(keyFrequencyFormat, value) }
        _rcSettings.update { it.copy(frequencyFormat = value) }
    }

    private fun getRCSettings(): RCSettings = RCSettings(
        rotatorState = preferences.getBoolean(keyRotatorState, false),
        rotatorAddress = preferences.getString(keyRotatorAddress, null) ?: "127.0.0.1",
        rotatorPort = preferences.getString(keyRotatorPort, null) ?: "4533",
        rotatorFormat = preferences.getString(keyRotatorFormat, null) ?: $$"P $AZ $EL",
        frequencyState = preferences.getBoolean(keyFrequencyState, false),
        frequencyAddress = preferences.getString(keyFrequencyAddress, null) ?: "127.0.0.1",
        frequencyPort = preferences.getString(keyFrequencyPort, null) ?: "4532",
        frequencyFormat = preferences.getString(keyFrequencyFormat, null) ?: $$"set_freq $FREQ",
        bluetoothRotatorState = preferences.getBoolean(keyBluetoothRotatorState, false),
        bluetoothRotatorFormat = preferences.getString(keyBluetoothRotatorFormat, null) ?: $$"W$AZ $EL",
        bluetoothRotatorName = preferences.getString(keyBluetoothRotatorName, null) ?: "Default",
        bluetoothRotatorAddress = preferences.getString(keyBluetoothRotatorAddress, null) ?: "00:0C:BF:13:80:5D",
        bluetoothFrequencyState = preferences.getBoolean(keyBluetoothFrequencyState, false),
        bluetoothFrequencyAddress = preferences.getString(keyBluetoothFrequencyAddress, null) ?: "00:0C:BF:13:80:5D",
        bluetoothFrequencyFormat = preferences.getString(keyBluetoothFrequencyFormat, null) ?: $$"FA$FREQ"
    )
    //endregion

    //region # Other settings
    private val _otherSettings = MutableStateFlow(getOtherSettings())
    override val otherSettings: StateFlow<OtherSettings> = _otherSettings

    override fun setStateOfAutoUpdate(value: Boolean) {
        preferences.edit { putBoolean(keyStateOfAutoUpdate, value) }
        _otherSettings.update { it.copy(stateOfAutoUpdate = value) }
    }

    override fun setStateOfSensors(value: Boolean) {
        preferences.edit { putBoolean(keyStateOfSensors, value) }
        _otherSettings.update { it.copy(stateOfSensors = value) }
    }

    override fun setStateOfSweep(value: Boolean) {
        preferences.edit { putBoolean(keyStateOfSweep, value) }
        _otherSettings.update { it.copy(stateOfSweep = value) }
    }

    override fun setStateOfUtc(value: Boolean) {
        preferences.edit { putBoolean(keyStateOfUtc, value) }
        _otherSettings.update { it.copy(stateOfUtc = value) }
    }

    override fun setStateOfLightTheme(value: Boolean){
        preferences.edit { putBoolean(keyStateOfLightTheme, value) }
        _otherSettings.update { it.copy(stateOfLightTheme = value) }
    }

    override fun setWarningDismissed() {
        preferences.edit { putBoolean(keyShouldSeeWarning, false) }
        _otherSettings.update { it.copy(shouldSeeWarning = false) }
    }

    override fun setWelcomeDismissed() {
        preferences.edit { putBoolean(keyShouldSeeWelcome, false) }
        _otherSettings.update { it.copy(shouldSeeWelcome = false) }
    }

    private fun getOtherSettings(): OtherSettings = OtherSettings(
        stateOfAutoUpdate = preferences.getBoolean(keyStateOfAutoUpdate, true),
        stateOfSensors = preferences.getBoolean(keyStateOfSensors, true),
        stateOfSweep = preferences.getBoolean(keyStateOfSweep, true),
        stateOfUtc = preferences.getBoolean(keyStateOfUtc, false),
        stateOfLightTheme = preferences.getBoolean(keyStateOfLightTheme, false),
        shouldSeeWarning = preferences.getBoolean(keyShouldSeeWarning, true),
        shouldSeeWelcome = preferences.getBoolean(keyShouldSeeWelcome, true)
    )
    //endregion

    //region # Data sources settings
    private val _dataSourcesSettings = MutableStateFlow(getDataSourcesSettings())
    override val dataSourcesSettings: StateFlow<DataSourcesSettings> = _dataSourcesSettings

    override fun setUseCustomTle(value: Boolean) {
        preferences.edit { putBoolean(keyUseCustomTle, value) }
        _dataSourcesSettings.update { it.copy(useCustomTLE = value) }
    }

    override fun setUseCustomTransceivers(value: Boolean) {
        preferences.edit { putBoolean(keyUseCustomTransceivers, value) }
        _dataSourcesSettings.update { it.copy(useCustomTransceivers = value) }
    }

    override fun setTleUrl(value: String) {
        preferences.edit { putString(keyTleUrl, value) }
        _dataSourcesSettings.update { it.copy(tleUrl = value) }
    }

    override fun setTransceiversUrl(value: String) {
        preferences.edit { putString(keyTransceiversUrl, value) }
        _dataSourcesSettings.update { it.copy(transceiversUrl = value) }
    }

    private fun getDataSourcesSettings(): DataSourcesSettings = DataSourcesSettings(
        useCustomTLE = preferences.getBoolean(keyUseCustomTle, false),
        useCustomTransceivers = preferences.getBoolean(keyUseCustomTransceivers, false),
        tleUrl = preferences.getString(keyTleUrl, "https://example.com/tle.txt") ?: "",
        transceiversUrl = preferences.getString(keyTransceiversUrl, "https://example.com/radio.json") ?: ""
    )
    //endregion
}
