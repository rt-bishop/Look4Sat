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
package com.rtbishop.look4sat.presentation.settings

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rtbishop.look4sat.MainApplication
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.domain.model.OtherSettings
import com.rtbishop.look4sat.domain.repository.IDatabaseRepo
import com.rtbishop.look4sat.domain.repository.ISettingsRepo
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val databaseRepo: IDatabaseRepo, private val settingsRepo: ISettingsRepo
) : ViewModel() {

    private val defaultPosSettings = PositionSettings(false, settingsRepo.stationPosition.value, 0)
    private val _positionSettings = mutableStateOf(defaultPosSettings)
    val positionSettings: State<PositionSettings> = _positionSettings

    private val defaultDataSettings = DataSettings(false, 0, 0, 0L)
    private val _dataSettings = mutableStateOf(defaultDataSettings)
    val dataSettings: State<DataSettings> = _dataSettings

    private val _otherSettings = mutableStateOf(settingsRepo.otherSettings.value)
    val otherSettings: State<OtherSettings> = _otherSettings

    init {
        viewModelScope.launch {
            settingsRepo.stationPosition.collect { geoPos ->
                _positionSettings.value =
                    _positionSettings.value.copy(isUpdating = false, stationPos = geoPos)
            }
        }
        viewModelScope.launch {
            settingsRepo.databaseState.collect { state ->
                _dataSettings.value = _dataSettings.value.copy(
                    isUpdating = false,
                    entriesTotal = state.numberOfSatellites,
                    radiosTotal = state.numberOfRadios,
                    timestamp = state.updateTimestamp
                )
            }
        }
        viewModelScope.launch {
            settingsRepo.otherSettings.collect { _otherSettings.value = it }
        }
    }

    fun setGpsPosition() {
        if (settingsRepo.setStationPositionGps()) {
            val messageResId = R.string.location_success
            _positionSettings.value =
                _positionSettings.value.copy(isUpdating = true, messageResId = messageResId)
        } else {
            val errorResId = R.string.location_gps_error
            _positionSettings.value = _positionSettings.value.copy(messageResId = errorResId)
        }
    }

    fun setGeoPosition(latitude: Double, longitude: Double) {
        if (settingsRepo.setStationPositionGeo(latitude, longitude, 0.0)) {
            val messageResId = R.string.location_success
            _positionSettings.value = _positionSettings.value.copy(messageResId = messageResId)
        } else {
            val errorResId = R.string.location_manual_error
            _positionSettings.value = _positionSettings.value.copy(messageResId = errorResId)
        }
    }

    fun setQthPosition(locator: String) {
        if (settingsRepo.setStationPositionQth(locator)) {
            val messageResId = R.string.location_success
            _positionSettings.value = _positionSettings.value.copy(messageResId = messageResId)
        } else {
            val errorResId = R.string.location_qth_error
            _positionSettings.value = _positionSettings.value.copy(messageResId = errorResId)
        }
    }

    fun dismissPosMessage() {
        _positionSettings.value = _positionSettings.value.copy(messageResId = 0)
    }

    fun updateFromWeb() = viewModelScope.launch {
        try {
            _dataSettings.value = _dataSettings.value.copy(isUpdating = true)
            databaseRepo.updateFromRemote()
        } catch (exception: Exception) {
            _dataSettings.value = _dataSettings.value.copy(isUpdating = false)
            println(exception)
        }
    }

    fun updateFromFile(uri: String) = viewModelScope.launch {
        try {
            _dataSettings.value = _dataSettings.value.copy(isUpdating = true)
            databaseRepo.updateFromFile(uri)
        } catch (exception: Exception) {
            _dataSettings.value = _dataSettings.value.copy(isUpdating = false)
            println(exception)
        }
    }

    fun clearAllData() = viewModelScope.launch { databaseRepo.clearAllData() }

    fun toggleUtc(value: Boolean) = settingsRepo.setStateOfUtc(value)
    fun toggleUpdate(value: Boolean) = settingsRepo.setStateOfAutoUpdate(value)
    fun toggleSweep(value: Boolean) = settingsRepo.setStateOfSweep(value)
    fun toggleSensor(value: Boolean) = settingsRepo.setStateOfSensors(value)
    fun toggleLightTheme(value: Boolean) = settingsRepo.setStateOfLightTheme(value)
//    fun getRotatorEnabled(): Boolean = settings.getRotatorEnabled()
//    fun setRotatorEnabled(value: Boolean) = settings.setRotatorEnabled(value)
//    fun getRotatorServer(): String = settings.getRotatorServer()
//    fun setRotatorServer(value: String) = settings.setRotatorServer(value)
//    fun getRotatorPort(): String = settings.getRotatorPort()
//    fun setRotatorPort(value: String) = settings.setRotatorPort(value)
//    fun getBTEnabled(): Boolean = settings.getBTEnabled()
//    fun setBTEnabled(value: Boolean) = settings.setBTEnabled(value)
//    fun getBTFormat(): String = settings.getBTFormat()
//    fun setBTFormat(value: String) = settings.setBTFormat(value)
//    fun getBTDeviceName(): String = settings.getBTDeviceName()
//    fun setBTDeviceName(value: String) = settings.setBTDeviceName(value)
//    fun getBTDeviceAddr(): String = settings.getBTDeviceAddr()
//    fun setBTDeviceAddr(value: String) = settings.setBTDeviceAddr(value)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            val applicationKey = ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY
            initializer {
                val container = (this[applicationKey] as MainApplication).container
                SettingsViewModel(container.databaseRepo, container.settingsRepo)
            }
        }
    }
}
