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
package com.rtbishop.look4sat.domain

import com.rtbishop.look4sat.model.GeoPos
import kotlinx.coroutines.flow.StateFlow

interface ISettingsRepository {

    val stationPosition: StateFlow<GeoPos>

    val satelliteSelection: StateFlow<List<Int>>

    fun setGpsPosition(): Boolean

    fun setGeoPosition(latitude: Double, longitude: Double, altitude: Double = 0.0): Boolean

    fun setQthPosition(locator: String): Boolean

    fun getHoursAhead(): Int

    fun setHoursAhead(hoursAhead: Int)

    fun getMinElevation(): Double

    fun setMinElevation(minElevation: Double)

    fun isUtcEnabled(): Boolean

    fun setUtcState(value: Boolean)

    fun getLastUpdateTime(): Long

    fun setLastUpdateTime(updateTime: Long)

    fun isUpdateEnabled(): Boolean

    fun setUpdateState(value: Boolean)

    fun isSensorEnabled(): Boolean

    fun setSensorState(value: Boolean)

    fun isSweepEnabled(): Boolean

    fun setSweepState(value: Boolean)

    fun saveModesSelection(modes: List<String>)

    fun loadModesSelection(): List<String>

    fun saveEntriesSelection(catnums: List<Int>)

    fun saveSatType(type: String, catnums: List<Int>)

    fun loadSatType(type: String): List<Int>

    fun getRotatorEnabled(): Boolean

    fun setRotatorEnabled(value: Boolean)

    fun getRotatorServer(): String

    fun setRotatorServer(value: String)

    fun getRotatorPort(): String

    fun setRotatorPort(value: String)

    fun getBTEnabled(): Boolean

    fun setBTEnabled(value: Boolean)

    fun getBTDeviceAddr(): String

    fun setBTDeviceAddr(value: String)

    fun getBTDeviceName(): String

    fun setBTDeviceName(value: String)

    fun getBTFormat(): String

    fun setBTFormat(value: String)
}
