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

import android.content.SharedPreferences
import android.location.Criteria
import android.location.LocationManager
import androidx.core.content.edit
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import com.rtbishop.look4sat.domain.ISettingsRepository
import com.rtbishop.look4sat.model.GeoPos
import com.rtbishop.look4sat.utility.QthConverter
import com.rtbishop.look4sat.utility.round
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.Executors

class SettingsRepository(
    private val manager: LocationManager, private val preferences: SharedPreferences
) : ISettingsRepository {

    private val keyModes = "satModes"
    private val keyCompass = "compass"
    private val keyRadarSweep = "radarSweep"
    private val keyTimeUTC = "timeUTC"
    private val keyLastUpdateTime = "lastUpdateTime"
    private val keyAutoUpdateEnabled = "autoUpdateEnabled"
    private val keyHoursAhead = "hoursAhead"
    private val keyMinElevation = "minElevation"
    private val keyRotator = "isRotatorEnabled"
    private val keyRotatorAddress = "rotatorAddress"
    private val keyRotatorPort = "rotatorPort"
    private val keyBTEnabled = "isBTEnabled"
    private val keyBTDeviceName = "BTDeviceName"
    private val keyBTDeviceAddr = "BTDeviceAddr"
    private val keyBTFormat = "BTFormat"
    private val keyLatitude = "stationLat"
    private val keyLongitude = "stationLon"
    private val keyAltitude = "stationAlt"
    private val keyLocator = "stationQTH"
    private val keyLocTimestamp = "locTimestamp"
    private val keySelection = "selection"

    //region # Station position settings

    private val _stationPosition = MutableStateFlow(loadStationPosition())
    private val defaultProvider = LocationManager.PASSIVE_PROVIDER
    private val executor = Executors.newSingleThreadExecutor()
    private val timeoutSignal = CancellationSignal().apply {
        setOnCancelListener { _stationPosition.value = loadStationPosition() }
    }
    override val stationPosition: StateFlow<GeoPos> = _stationPosition

    override fun setGpsPosition(): Boolean {
        if (!LocationManagerCompat.isLocationEnabled(manager)) return false
        try {
            val criteria = Criteria().apply { isCostAllowed = true }
            val provider = manager.getBestProvider(criteria, true) ?: defaultProvider
            println("Requesting location for $provider provider")
            LocationManagerCompat.getCurrentLocation(manager, provider, timeoutSignal, executor) {
                it?.let { setGeoPosition(it.latitude, it.longitude, it.altitude) }
            }
        } catch (exception: SecurityException) {
            println("No permissions were given")
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

    private fun loadStationPosition(): GeoPos {
        val latitude = (preferences.getString(keyLatitude, null) ?: "0.0").toDouble()
        val longitude = (preferences.getString(keyLongitude, null) ?: "0.0").toDouble()
        val altitude = (preferences.getString(keyAltitude, null) ?: "0.0").toDouble()
        val qthLocator = preferences.getString(keyLocator, null) ?: "null"
        val timestamp = preferences.getLong(keyLocTimestamp, 0L)
        return GeoPos(latitude, longitude, altitude, qthLocator, timestamp)
    }

    private fun saveStationPosition(stationPos: GeoPos) = preferences.edit {
        putString(keyLatitude, stationPos.latitude.toString())
        putString(keyLongitude, stationPos.longitude.toString())
        putString(keyAltitude, stationPos.altitude.toString())
        putString(keyLocator, stationPos.qthLocator)
        putLong(keyLocTimestamp, stationPos.timestamp)
        _stationPosition.value = stationPos
    }

    private fun saveGeoPos(latitude: Double, longitude: Double, altitude: Double, locator: String) {
        val newLat = latitude.round(4)
        val newLon = longitude.round(4)
        val newAlt = altitude.round(1)
        val timestamp = System.currentTimeMillis()
        println("Received new Position($newLat, $newLon, $newAlt) & Locator $locator")
        saveStationPosition(GeoPos(newLat, newLon, newAlt, locator, timestamp))
    }

    //endregion

    override fun saveEntriesSelection(catnums: List<Int>) {
        val stringList = catnums.map { catnum -> catnum.toString() }
        preferences.edit { putStringSet(keySelection, stringList.toSet()) }
    }

    override fun loadEntriesSelection(): List<Int> {
        val catnums =
            preferences.getStringSet(keySelection, emptySet())?.map { catnum -> catnum.toInt() }
        return catnums?.sorted() ?: emptyList()
    }

    override fun saveSatType(type: String, catnums: List<Int>) {
        val stringList = catnums.map { catnum -> catnum.toString() }
        preferences.edit { putStringSet("type$type", stringList.toSet()) }
    }

    override fun loadSatType(type: String): List<Int> {
        val catnums =
            preferences.getStringSet("type$type", emptySet())?.map { catnum -> catnum.toInt() }
        return catnums ?: emptyList()
    }

    override fun getHoursAhead(): Int {
        return preferences.getInt(keyHoursAhead, 24)
    }

    override fun setHoursAhead(hoursAhead: Int) {
        preferences.edit { putInt(keyHoursAhead, hoursAhead) }
    }

    override fun getMinElevation(): Double {
        return preferences.getDouble(keyMinElevation, 16.0)
    }

    override fun setMinElevation(minElevation: Double) {
        preferences.edit { putDouble(keyMinElevation, minElevation) }
    }

    override fun isUtcEnabled(): Boolean {
        return preferences.getBoolean(keyTimeUTC, false)
    }

    override fun setUtcState(value: Boolean) {
        preferences.edit { putBoolean(keyTimeUTC, value) }
    }

    override fun getLastUpdateTime(): Long {
        return preferences.getLong(keyLastUpdateTime, 0L)
    }

    override fun setLastUpdateTime(updateTime: Long) {
        preferences.edit { putLong(keyLastUpdateTime, updateTime) }
    }

    override fun isUpdateEnabled(): Boolean {
        return preferences.getBoolean(keyAutoUpdateEnabled, true)
    }

    override fun setUpdateState(value: Boolean) {
        preferences.edit { putBoolean(keyAutoUpdateEnabled, value) }
    }

    override fun isSensorEnabled(): Boolean {
        return preferences.getBoolean(keyCompass, true)
    }

    override fun setSensorState(value: Boolean) {
        preferences.edit { putBoolean(keyCompass, value) }
    }

    override fun isSweepEnabled(): Boolean {
        return preferences.getBoolean(keyRadarSweep, true)
    }

    override fun setSweepState(value: Boolean) {
        preferences.edit { putBoolean(keyRadarSweep, value) }
    }

    override fun saveModesSelection(modes: List<String>) {
        preferences.edit { putStringSet(keyModes, modes.toSet()) }
    }

    override fun loadModesSelection(): List<String> {
        return preferences.getStringSet(keyModes, null)?.toList()?.sorted() ?: emptyList()
    }

    override fun getRotatorEnabled(): Boolean {
        return preferences.getBoolean(keyRotator, false)
    }

    override fun setRotatorEnabled(value: Boolean) {
        preferences.edit { putBoolean(keyRotator, value) }
    }

    override fun getRotatorServer(): String {
        return preferences.getString(keyRotatorAddress, null) ?: "127.0.0.1"
    }

    override fun setRotatorServer(value: String) {
        preferences.edit { putString(keyRotatorAddress, value) }
    }

    override fun getRotatorPort(): String {
        return preferences.getString(keyRotatorPort, null) ?: "4533"
    }

    override fun setRotatorPort(value: String) {
        preferences.edit { putString(keyRotatorPort, value) }
    }

    override fun getBTEnabled(): Boolean {
        return preferences.getBoolean(keyBTEnabled, false)
    }

    override fun setBTEnabled(value: Boolean) {
        preferences.edit { putBoolean(keyBTEnabled, value) }
    }

    override fun getBTDeviceAddr(): String {
        return preferences.getString(keyBTDeviceAddr, null) ?: "00:0C:BF:13:80:5D"
    }

    override fun setBTDeviceAddr(value: String) {
        preferences.edit { putString(keyBTDeviceAddr, value) }
    }

    override fun getBTDeviceName(): String {
        return preferences.getString(keyBTDeviceName, null) ?: "Default"
    }

    override fun setBTDeviceName(value: String) {
        preferences.edit { putString(keyBTDeviceName, value) }
    }

    override fun getBTFormat(): String {
        return preferences.getString(keyBTFormat, null) ?: "W\$AZ \$EL"
    }

    override fun setBTFormat(value: String) {
        preferences.edit { putString(keyBTFormat, value) }
    }

    private fun SharedPreferences.getDouble(key: String, default: Double): Double {
        return Double.fromBits(getLong(key, default.toRawBits()))
    }

    private fun SharedPreferences.Editor.putDouble(key: String, double: Double) {
        putLong(key, double.toRawBits())
    }
}
