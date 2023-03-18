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
import androidx.core.content.edit
import com.rtbishop.look4sat.domain.ISettingsSource
import com.rtbishop.look4sat.model.GeoPos
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onCompletion

class SettingsSource(private val prefs: SharedPreferences) : ISettingsSource {

    companion object {
        const val keyModes = "satModes"
        const val keyCompass = "compass"
        const val keyRadarSweep = "radarSweep"
        const val keyTimeUTC = "timeUTC"
        const val keyLastUpdateTime = "lastUpdateTime"
        const val keyAutoUpdateEnabled = "autoUpdateEnabled"
        const val keyHoursAhead = "hoursAhead"
        const val keyMinElevation = "minElevation"
        const val keyRotator = "isRotatorEnabled"
        const val keyRotatorAddress = "rotatorAddress"
        const val keyRotatorPort = "rotatorPort"
        const val keyBTEnabled = "isBTEnabled"
        const val keyBTDeviceName = "BTDeviceName"
        const val keyBTDeviceAddr = "BTDeviceAddr"
        const val keyBTFormat = "BTFormat"
        const val keyLatitude = "stationLat"
        const val keyLongitude = "stationLon"
        const val keyAltitude = "stationAlt"
        const val keyLocator = "stationQTH"
        const val keyLocTimestamp = "locTimestamp"
        const val keySelection = "selection"
    }

    override fun loadStationPosition(): GeoPos {
        val latitude = (prefs.getString(keyLatitude, null) ?: "0.0").toDouble()
        val longitude = (prefs.getString(keyLongitude, null) ?: "0.0").toDouble()
        val altitude = (prefs.getString(keyAltitude, null) ?: "0.0").toDouble()
        val qthLocator = prefs.getString(keyLocator, null) ?: "null"
        val timestamp = prefs.getLong(keyLocTimestamp, 0L)
        return GeoPos(latitude, longitude, altitude, qthLocator, timestamp)
    }

    override fun saveStationPosition(stationPos: GeoPos) = prefs.edit {
        putString(keyLatitude, stationPos.latitude.toString())
        putString(keyLongitude, stationPos.longitude.toString())
        putString(keyAltitude, stationPos.altitude.toString())
        putString(keyLocator, stationPos.qthLocator)
        putLong(keyLocTimestamp, stationPos.timestamp)
    }

    override fun saveEntriesSelection(catnums: List<Int>) {
        val stringList = catnums.map { catnum -> catnum.toString() }
        prefs.edit { putStringSet(keySelection, stringList.toSet()) }
    }

    override fun loadEntriesSelection(): List<Int> {
        val catnums = prefs.getStringSet(keySelection, emptySet())?.map { catnum -> catnum.toInt() }
        return catnums?.sorted() ?: emptyList()
    }

    override fun saveSatType(type: String, catnums: List<Int>) {
        val stringList = catnums.map { catnum -> catnum.toString() }
        prefs.edit { putStringSet("type$type", stringList.toSet()) }
    }

    override fun loadSatType(type: String): List<Int> {
        val catnums = prefs.getStringSet("type$type", emptySet())?.map { catnum -> catnum.toInt() }
        return catnums ?: emptyList()
    }

    override fun getHoursAhead(): Int {
        return prefs.getInt(keyHoursAhead, 24)
    }

    override fun setHoursAhead(hoursAhead: Int) {
        prefs.edit { putInt(keyHoursAhead, hoursAhead) }
    }

    override fun getMinElevation(): Double {
        return prefs.getDouble(keyMinElevation, 16.0)
    }

    override fun setMinElevation(minElevation: Double) {
        prefs.edit { putDouble(keyMinElevation, minElevation) }
    }

    override fun isUtcEnabled(): Boolean {
        return prefs.getBoolean(keyTimeUTC, false)
    }

    override fun setUtcState(value: Boolean) {
        prefs.edit { putBoolean(keyTimeUTC, value) }
    }

    override fun getLastUpdateTime(): Long {
        return prefs.getLong(keyLastUpdateTime, 0L)
    }

    override fun setLastUpdateTime(updateTime: Long) {
        prefs.edit { putLong(keyLastUpdateTime, updateTime) }
    }

    override fun isUpdateEnabled(): Boolean {
        return prefs.getBoolean(keyAutoUpdateEnabled, true)
    }

    override fun setUpdateState(value: Boolean) {
        prefs.edit { putBoolean(keyAutoUpdateEnabled, value) }
    }

    override fun isSensorEnabled(): Boolean {
        return prefs.getBoolean(keyCompass, true)
    }

    override fun setSensorState(value: Boolean) {
        prefs.edit { putBoolean(keyCompass, value) }
    }

    override fun isSweepEnabled(): Boolean {
        return prefs.getBoolean(keyRadarSweep, true)
    }

    override fun setSweepState(value: Boolean) {
        prefs.edit { putBoolean(keyRadarSweep, value) }
    }

    override fun saveModesSelection(modes: List<String>) {
        prefs.edit { putStringSet(keyModes, modes.toSet()) }
    }

    override fun loadModesSelection(): List<String> {
        return prefs.getStringSet(keyModes, null)?.toList()?.sorted() ?: emptyList()
    }

    override fun getRotatorEnabled(): Boolean {
        return prefs.getBoolean(keyRotator, false)
    }

    override fun setRotatorEnabled(value: Boolean) {
        prefs.edit { putBoolean(keyRotator, value) }
    }

    override fun getRotatorServer(): String {
        return prefs.getString(keyRotatorAddress, null) ?: "127.0.0.1"
    }

    override fun setRotatorServer(value: String) {
        prefs.edit { putString(keyRotatorAddress, value) }
    }

    override fun getRotatorPort(): String {
        return prefs.getString(keyRotatorPort, null) ?: "4533"
    }

    override fun setRotatorPort(value: String) {
        prefs.edit { putString(keyRotatorPort, value) }
    }

    override fun getBTEnabled(): Boolean {
        return prefs.getBoolean(keyBTEnabled, false)
    }

    override fun setBTEnabled(value: Boolean) {
        prefs.edit { putBoolean(keyBTEnabled, value) }
    }

    override fun getBTDeviceAddr(): String {
        return prefs.getString(keyBTDeviceAddr, null) ?: "00:0C:BF:13:80:5D"
    }

    override fun setBTDeviceAddr(value: String) {
        prefs.edit { putString(keyBTDeviceAddr, value) }
    }

    override fun getBTDeviceName(): String {
        return prefs.getString(keyBTDeviceName, null) ?: "Default"
    }

    override fun setBTDeviceName(value: String) {
        prefs.edit { putString(keyBTDeviceName, value) }
    }

    override fun getBTFormat(): String {
        return prefs.getString(keyBTFormat, null) ?: "W\$AZ \$EL"
    }

    override fun setBTFormat(value: String) {
        prefs.edit { putString(keyBTFormat, value) }
    }

    private fun SharedPreferences.getDouble(key: String, default: Double): Double {
        return Double.fromBits(getLong(key, default.toRawBits()))
    }

    private fun SharedPreferences.Editor.putDouble(key: String, double: Double) {
        putLong(key, double.toRawBits())
    }

    inline fun <reified T> SharedPreferences.observeKey(key: String, default: T): Flow<T> {
        val flow = MutableStateFlow(getItem(key, default))
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, k ->
            if (key == k) flow.value = getItem(key, default)
        }
        registerOnSharedPreferenceChangeListener(listener)
        return flow.onCompletion { unregisterOnSharedPreferenceChangeListener(listener) }
    }

    inline fun <reified T> SharedPreferences.getItem(key: String, default: T): T {
        return when (default) {
            is Boolean -> getBoolean(key, default) as T
            is Int -> getInt(key, default) as T
            is Long -> getLong(key, default) as T
            is Float -> getFloat(key, default) as T
            is String -> getString(key, default) as T
            else -> throw IllegalArgumentException("Could not handle ${T::class.java.name}")
        }
    }
}
