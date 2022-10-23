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
package com.rtbishop.look4sat.presentation.settingsScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.rtbishop.look4sat.domain.IDataRepository
import com.rtbishop.look4sat.domain.ILocationManager
import com.rtbishop.look4sat.domain.ISettingsManager
import com.rtbishop.look4sat.domain.model.DataState
import com.rtbishop.look4sat.domain.predict.GeoPos
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val locationManager: ILocationManager,
    private val repository: IDataRepository,
    private val settings: ISettingsManager
) : ViewModel() {

    val entriesTotal = repository.getEntriesTotal().asLiveData()
    val radiosTotal = repository.getRadiosTotal().asLiveData()

    fun updateFromFile(uri: String) = repository.updateFromFile(uri)

    fun updateFromWeb() = repository.updateFromWeb()

    fun clearAllData() = repository.clearAllData()

    fun getUseUTC(): Boolean = settings.getUseUTC()

    fun setUseUTC(value: Boolean) = settings.setUseUTC(value)

    fun getLastUpdateTime(): Long = settings.getLastUpdateTime()

    fun getAutoUpdateEnabled(): Boolean = settings.getAutoUpdateEnabled()

    fun setAutoUpdateEnabled(value: Boolean) = settings.setAutoUpdateEnabled(value)

    fun getUseCompass(): Boolean = settings.getUseCompass()

    fun setUseCompass(value: Boolean) = settings.setUseCompass(value)

    fun getShowSweep(): Boolean = settings.getShowSweep()

    fun setShowSweep(value: Boolean) = settings.setShowSweep(value)

    fun getRotatorEnabled(): Boolean = settings.getRotatorEnabled()

    fun setRotatorEnabled(value: Boolean) = settings.setRotatorEnabled(value)

    fun getRotatorServer(): String = settings.getRotatorServer()

    fun setRotatorServer(value: String) = settings.setRotatorServer(value)

    fun getRotatorPort(): String = settings.getRotatorPort()

    fun setRotatorPort(value: String) = settings.setRotatorPort(value)

    fun getBTEnabled(): Boolean = settings.getBTEnabled()

    fun setBTEnabled(value: Boolean) = settings.setBTEnabled(value)

    fun getBTFormat(): String = settings.getBTFormat()

    fun setBTFormat(value: String) = settings.setBTFormat(value)

//    fun getBTDeviceName(): String = settings.getBTDeviceName()

//    fun setBTDeviceName(value: String) = settings.setBTDeviceName(value)

    fun getBTDeviceAddr(): String = settings.getBTDeviceAddr()

    fun setBTDeviceAddr(value: String) = settings.setBTDeviceAddr(value)

    fun getUpdateState() = repository.updateState

    fun setUpdateHandled() = repository.setUpdateStateHandled()

    val stationPosition: SharedFlow<DataState<GeoPos>> = locationManager.stationPosition

    fun getStationPosition(): GeoPos = locationManager.getStationPosition()

    fun setStationPosition(lat: Double, lon: Double) = locationManager.setStationPosition(lat, lon)

    fun setPositionFromGps() = locationManager.setPositionFromGps()

    fun setPositionFromNet() = locationManager.setPositionFromNet()

    fun setPositionFromQth(qthString: String) = locationManager.setPositionFromQth(qthString)

    fun setPositionHandled() = locationManager.setPositionHandled()
}
