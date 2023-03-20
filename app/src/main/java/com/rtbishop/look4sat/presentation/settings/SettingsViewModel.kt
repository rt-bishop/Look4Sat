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

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rtbishop.look4sat.MainApplication
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.domain.IDataRepository
import com.rtbishop.look4sat.domain.ISettingsRepository
import com.rtbishop.look4sat.model.DataState
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val dataRepository: IDataRepository, private val settingsRepository: ISettingsRepository
) : ViewModel() {

    private val defaultLocationSettings = LocationSettings(false,
        settingsRepository.stationPosition.value,
        0,
        { setGpsPosition() },
        { latitude, longitude -> setGeoPosition(latitude, longitude) },
        { locator -> setQthPosition(locator) },
        { dismissLocationMessage() })
    val locationSettings = mutableStateOf(defaultLocationSettings)

    private val defaultDataSettings = DataSettings(false,
        settingsRepository.getLastUpdateTime(),
        0,
        0,
        { dataRepository.updateFromWeb() },
        { dataRepository.updateFromFile(it) },
        { dataRepository.clearAllData() })
    val dataSettings = mutableStateOf(defaultDataSettings)

    private val defaultOtherSettings = OtherSettings(settingsRepository.isUtcEnabled(),
        settingsRepository.isUpdateEnabled(),
        settingsRepository.isSweepEnabled(),
        settingsRepository.isSensorEnabled(),
        { setUtc(it) },
        { setUpdate(it) },
        { setSweep(it) },
        { setSensor(it) })
    val otherSettings = mutableStateOf(defaultOtherSettings)

    init {
        viewModelScope.launch {
            settingsRepository.stationPosition.collect { stationPos ->
                locationSettings.value = locationSettings.value.copy(
                    isUpdating = false, stationPos = stationPos
                )
            }
        }
        viewModelScope.launch {
            dataRepository.updateState.collect {
                val isUpdating = it is DataState.Loading
                dataSettings.value = dataSettings.value.copy(
                    isUpdating = isUpdating, lastUpdated = settingsRepository.getLastUpdateTime()
                )
            }
        }
        viewModelScope.launch {
            combine(
                dataRepository.getEntriesTotal(), dataRepository.getRadiosTotal()
            ) { sats, radios ->
                dataSettings.value = dataSettings.value.copy(satsTotal = sats, radiosTotal = radios)
            }.collect {}
        }
    }

    private fun setGpsPosition() {
        if (settingsRepository.setGpsPosition()) {
            val messageResId = R.string.location_success
            locationSettings.value =
                locationSettings.value.copy(isUpdating = true, messageResId = messageResId)
        } else {
            val errorResId = R.string.location_gps_error
            locationSettings.value = locationSettings.value.copy(messageResId = errorResId)
        }
    }

    private fun setGeoPosition(latitude: Double, longitude: Double) {
        if (settingsRepository.setGeoPosition(latitude, longitude)) {
            val messageResId = R.string.location_success
            locationSettings.value = locationSettings.value.copy(messageResId = messageResId)
        } else {
            val errorResId = R.string.location_manual_error
            locationSettings.value = locationSettings.value.copy(messageResId = errorResId)
        }
    }

    private fun setQthPosition(locator: String) {
        if (settingsRepository.setQthPosition(locator)) {
            val messageResId = R.string.location_success
            locationSettings.value = locationSettings.value.copy(messageResId = messageResId)
        } else {
            val errorResId = R.string.location_qth_error
            locationSettings.value = locationSettings.value.copy(messageResId = errorResId)
        }
    }

    private fun dismissLocationMessage() {
        locationSettings.value = locationSettings.value.copy(messageResId = 0)
    }

    private fun setUtc(value: Boolean) {
        settingsRepository.setUtcState(value)
        otherSettings.value = otherSettings.value.copy(getUtc = value)
    }

    private fun setUpdate(value: Boolean) {
        settingsRepository.setUpdateState(value)
        otherSettings.value = otherSettings.value.copy(getUpdate = value)
    }

    private fun setSweep(value: Boolean) {
        settingsRepository.setSweepState(value)
        otherSettings.value = otherSettings.value.copy(getSweep = value)
    }

    private fun setSensor(value: Boolean) {
        settingsRepository.setSensorState(value)
        otherSettings.value = otherSettings.value.copy(getSensor = value)
    }

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
                SettingsViewModel(container.dataRepository, container.settingsRepository)
            }
        }
    }
}
