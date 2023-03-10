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
import com.rtbishop.look4sat.domain.IDataRepository
import com.rtbishop.look4sat.domain.ILocationManager
import com.rtbishop.look4sat.domain.ISettingsManager
import com.rtbishop.look4sat.domain.model.DataState
import com.rtbishop.look4sat.domain.model.OtherSettings
import com.rtbishop.look4sat.domain.predict.GeoPos
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val locationManager: ILocationManager,
    private val repository: IDataRepository,
    private val settings: ISettingsManager
) : ViewModel() {

    private val _otherSettings = MutableStateFlow(
        OtherSettings(
            settings.isUtcEnabled(),
            settings.isUpdateEnabled(),
            settings.isSweepEnabled(),
            settings.isSensorEnabled()
        )
    )
    val otherSettings: StateFlow<OtherSettings> = _otherSettings

    fun setUtcState(value: Boolean) {
        settings.setUtcState(value)
        _otherSettings.value = otherSettings.value.copy(isUtcEnabled = value)
    }

    fun setUpdateState(value: Boolean) {
        settings.setUpdateState(value)
        _otherSettings.value = otherSettings.value.copy(isUpdateEnabled = value)
    }

    fun setSweepState(value: Boolean) {
        settings.setSweepState(value)
        _otherSettings.value = otherSettings.value.copy(isSweepEnabled = value)
    }

    fun setSensorState(value: Boolean) {
        settings.setSensorState(value)
        _otherSettings.value = otherSettings.value.copy(isSensorEnabled = value)
    }

    val entriesTotal = repository.getEntriesTotal()
    val radiosTotal = repository.getRadiosTotal()

    fun updateFromFile(uri: String) = repository.updateFromFile(uri)

    fun updateFromWeb() = repository.updateFromWeb()

    fun clearAllData() = repository.clearAllData()

    fun getUseUTC(): Boolean = settings.isUtcEnabled()

    fun getLastUpdateTime(): Long = settings.getLastUpdateTime()

    fun getAutoUpdateEnabled(): Boolean = settings.isUpdateEnabled()

    fun getUseCompass(): Boolean = settings.isSensorEnabled()

    fun getShowSweep(): Boolean = settings.isSweepEnabled()

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

    fun getDataUpdateState() = repository.updateState

    fun setUpdateHandled() = repository.setUpdateStateHandled()

    val stationPosition: SharedFlow<DataState<GeoPos>> = locationManager.stationPosition

    fun getStationPosition(): GeoPos = locationManager.getStationPosition()

    fun getStationLocator(): String = settings.loadStationLocator()

    fun setStationPosition(lat: Double, lon: Double) = locationManager.setStationPosition(lat, lon)

    fun setPositionFromGps() = locationManager.setPositionFromGps()

    fun setPositionFromNet() = locationManager.setPositionFromNet()

    fun setPositionFromQth(qthString: String) = locationManager.setPositionFromQth(qthString)

    fun setPositionHandled() = locationManager.setPositionHandled()
}
