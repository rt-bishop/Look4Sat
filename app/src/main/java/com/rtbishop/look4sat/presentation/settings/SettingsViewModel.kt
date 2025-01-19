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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rtbishop.look4sat.MainApplication
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.domain.repository.IDatabaseRepo
import com.rtbishop.look4sat.domain.repository.ISettingsRepo
import com.rtbishop.look4sat.domain.usecase.IOpenWebUrl
import com.rtbishop.look4sat.domain.usecase.IShowToast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val databaseRepo: IDatabaseRepo,
    private val settingsRepo: ISettingsRepo,
    private val openWebUrl: IOpenWebUrl,
    private val showToast: IShowToast,
    appVersionName: String
) : ViewModel() {

    private val githubUrl = "https://github.com/rt-bishop/Look4Sat/"
    private val donateUrl = "https://ko-fi.com/rt_bishop"
    private val fdroidUrl = "https://f-droid.org/en/packages/com.rtbishop.look4sat/"
    private val defaultPosSettings = PositionSettings(false, settingsRepo.stationPosition.value, 0)
    private val defaultDataSettings = DataSettings(false, 0, 0, 0L)
    private val defaultRCSettings = RCSettings(
        rotatorState = settingsRepo.getRotatorState(),
        rotatorAddress = settingsRepo.getRotatorAddress(),
        rotatorPort = settingsRepo.getRotatorPort(),
        bluetoothState = settingsRepo.getBluetoothState(),
        bluetoothFormat = settingsRepo.getBluetoothFormat(),
        bluetoothName = settingsRepo.getBluetoothName(),
        bluetoothAddress = settingsRepo.getBluetoothAddress()
    )
    private val _uiState = MutableStateFlow(
        SettingsState(
            appVersionName = appVersionName,
            positionSettings = defaultPosSettings,
            dataSettings = defaultDataSettings,
            otherSettings = settingsRepo.otherSettings.value,
            rcSettings = defaultRCSettings,
            sendAction = ::handleAction,
            sendRCAction = ::handleAction,
            sendSystemAction = ::handleAction
        )
    )
    val uiState: StateFlow<SettingsState> = _uiState

    init {
        viewModelScope.launch {
            settingsRepo.stationPosition.collect { geoPos ->
                val newPosSettings = _uiState.value.positionSettings.copy(
                    isUpdating = false, stationPos = geoPos
                )
                _uiState.update { it.copy(positionSettings = newPosSettings) }
            }
        }
        viewModelScope.launch {
            settingsRepo.databaseState.collect { state ->
                val newDataSettings = _uiState.value.dataSettings.copy(
                    isUpdating = false,
                    entriesTotal = state.numberOfSatellites,
                    radiosTotal = state.numberOfRadios,
                    timestamp = state.updateTimestamp
                )
                _uiState.update { it.copy(dataSettings = newDataSettings) }
            }
        }
        viewModelScope.launch {
            settingsRepo.otherSettings.collect { settings ->
                _uiState.update { it.copy(otherSettings = settings) }
            }
        }
    }

    private fun handleAction(action: SettingsAction) {
        when (action) {
            SettingsAction.SetGpsPosition -> setGpsPosition()
            is SettingsAction.SetGeoPosition -> setGeoPosition(action.latitude, action.longitude)
            is SettingsAction.SetQthPosition -> setQthPosition(action.locator)
            SettingsAction.DismissPosMessages -> dismissPosMessage()
            SettingsAction.UpdateFromWeb -> updateFromWeb()
            is SettingsAction.UpdateFromFile -> updateFromFile(action.uri)
            SettingsAction.ClearAllData -> clearAllData()
            is SettingsAction.ToggleUtc -> settingsRepo.setStateOfUtc(action.value)
            is SettingsAction.ToggleUpdate -> settingsRepo.setStateOfAutoUpdate(action.value)
            is SettingsAction.ToggleSweep -> settingsRepo.setStateOfSweep(action.value)
            is SettingsAction.ToggleSensor -> settingsRepo.setStateOfSensors(action.value)
            is SettingsAction.ToggleLightTheme -> settingsRepo.setStateOfLightTheme(action.value)
        }
    }

    private fun handleAction(action: RCAction) {
        when (action) {
            is RCAction.SetRotatorState -> settingsRepo.setRotatorState(action.value)
            is RCAction.SetRotatorAddress -> settingsRepo.setRotatorAddress(action.value)
            is RCAction.SetRotatorPort -> settingsRepo.setRotatorPort(action.value)
            is RCAction.SetBluetoothState -> settingsRepo.setBluetoothState(action.value)
            is RCAction.SetBluetoothAddress -> settingsRepo.setBluetoothAddress(action.value)
            is RCAction.SetBluetoothFormat -> settingsRepo.setBluetoothFormat(action.value)
            is RCAction.SetBluetoothName -> settingsRepo.setBluetoothName(action.value)
        }
    }

    private fun handleAction(action: SystemAction) {
        when (action) {
            SystemAction.OpenGitHub -> openWebUrl(githubUrl)
            SystemAction.OpenDonate -> openWebUrl(donateUrl)
            SystemAction.OpenFDroid -> openWebUrl(fdroidUrl)
            is SystemAction.ShowToast -> showToast(action.message)
        }
    }

    private fun setGpsPosition() {
        if (settingsRepo.setStationPositionGps()) {
            val messageResId = R.string.location_success
            val newPosSettings = _uiState.value.positionSettings.copy(
                isUpdating = true, messageResId = messageResId
            )
            _uiState.update { it.copy(positionSettings = newPosSettings) }
        } else {
            val errorResId = R.string.location_gps_error
            val newPosSettings = _uiState.value.positionSettings.copy(
                isUpdating = false, messageResId = errorResId
            )
            _uiState.update { it.copy(positionSettings = newPosSettings) }
        }
    }

    private fun setGeoPosition(latitude: Double, longitude: Double) {
        if (settingsRepo.setStationPositionGeo(latitude, longitude, 0.0)) {
            val messageResId = R.string.location_success
            val newPosSettings = _uiState.value.positionSettings.copy(
                isUpdating = false, messageResId = messageResId
            )
            _uiState.update { it.copy(positionSettings = newPosSettings) }
        } else {
            val errorResId = R.string.location_manual_error
            val newPosSettings = _uiState.value.positionSettings.copy(
                isUpdating = false, messageResId = errorResId
            )
            _uiState.update { it.copy(positionSettings = newPosSettings) }
        }
    }

    private fun setQthPosition(locator: String) {
        if (settingsRepo.setStationPositionQth(locator)) {
            val messageResId = R.string.location_success
            val newPosSettings = _uiState.value.positionSettings.copy(
                isUpdating = false, messageResId = messageResId
            )
            _uiState.update { it.copy(positionSettings = newPosSettings) }
        } else {
            val errorResId = R.string.location_qth_error
            val newPosSettings = _uiState.value.positionSettings.copy(
                isUpdating = false, messageResId = errorResId
            )
            _uiState.update { it.copy(positionSettings = newPosSettings) }
        }
    }

    private fun dismissPosMessage() {
        val newPosSettings = _uiState.value.positionSettings.copy(
            isUpdating = false, messageResId = 0
        )
        _uiState.update { it.copy(positionSettings = newPosSettings) }
    }

    private fun updateFromWeb() = viewModelScope.launch {
        try {
            val newDataSettings = _uiState.value.dataSettings.copy(isUpdating = true)
            _uiState.update { it.copy(dataSettings = newDataSettings) }
            databaseRepo.updateFromRemote()
        } catch (exception: Exception) {
            val newDataSettings = _uiState.value.dataSettings.copy(isUpdating = false)
            _uiState.update { it.copy(dataSettings = newDataSettings) }
            println(exception)
        }
    }

    private fun updateFromFile(uri: String) = viewModelScope.launch {
        try {
            val newDataSettings = _uiState.value.dataSettings.copy(isUpdating = true)
            _uiState.update { it.copy(dataSettings = newDataSettings) }
            databaseRepo.updateFromFile(uri)
        } catch (exception: Exception) {
            val newDataSettings = _uiState.value.dataSettings.copy(isUpdating = false)
            _uiState.update { it.copy(dataSettings = newDataSettings) }
            println(exception)
        }
    }

    private fun clearAllData() = viewModelScope.launch { databaseRepo.clearAllData() }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            val applicationKey = ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY
            initializer {
                val container = (this[applicationKey] as MainApplication).container
                SettingsViewModel(
                    container.databaseRepo,
                    container.settingsRepo,
                    container.provideOpenWebUrl(),
                    container.provideShowToast(),
                    container.provideAppVersionName()
                )
            }
        }
    }
}
