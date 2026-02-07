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
package com.rtbishop.look4sat.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rtbishop.look4sat.MainApplication
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.domain.model.DataSourcesSettings
import com.rtbishop.look4sat.domain.predict.OrbitalPass
import com.rtbishop.look4sat.domain.repository.IDatabaseRepo
import com.rtbishop.look4sat.domain.repository.ISatelliteRepo
import com.rtbishop.look4sat.domain.repository.ISettingsRepo
import com.rtbishop.look4sat.domain.usecase.IShowToast
import com.rtbishop.look4sat.domain.utility.toTimerString
import com.rtbishop.look4sat.presentation.common.getDefaultPass
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val databaseRepo: IDatabaseRepo,
    private val satelliteRepo: ISatelliteRepo,
    private val settingsRepo: ISettingsRepo,
    private val showToast: IShowToast,
    appVersionName: String
) : ViewModel() {

    private val defaultPosSettings = PositionSettings(false, settingsRepo.stationPosition.value, 0)
    private val defaultDataSettings = DataSettings(false, 0, 0, 0L)
    private val defaultDataSourcesSettings = DataSourcesSettings(
        useCustomTLE = false,
        useCustomTransceivers = false,
        tleUrl = settingsRepo.dataSourcesSettings.value.tleUrl,
        transceiversUrl = settingsRepo.dataSourcesSettings.value.transceiversUrl
    )
    private val _uiState = MutableStateFlow(
        SettingsState(
            nextTime = "00:00:00",
            isNextTimeAos = true,
            nextPass = getDefaultPass(),
            appVersionName = appVersionName,
            positionSettings = defaultPosSettings,
            dataSettings = defaultDataSettings,
            otherSettings = settingsRepo.otherSettings.value,
            rcSettings = settingsRepo.rcSettings.value,
            dataSourcesSettings = defaultDataSourcesSettings,
            sendAction = ::handleAction,
            sendRCAction = ::handleAction,
            sendSystemAction = ::handleAction,
            sendDataSourcesAction = ::handleAction
        )
    )
    private var processing: Job? = null

    val uiState: StateFlow<SettingsState> = _uiState

    init {
        viewModelScope.launch {
            satelliteRepo.passes.collectLatest { passes ->
                processing?.cancelAndJoin()
                processing = viewModelScope.launch {
                    while (isActive) {
                        val timeNow = System.currentTimeMillis()
                        val newPasses = satelliteRepo.processPasses(passes, timeNow)
                        setPassInfo(newPasses, timeNow)
                        delay(1000)
                    }
                }
            }
        }
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
            settingsRepo.rcSettings.collect { settings ->
                _uiState.update { it.copy(rcSettings = settings) }
            }
        }
        viewModelScope.launch {
            settingsRepo.otherSettings.collect { settings ->
                _uiState.update { it.copy(otherSettings = settings) }
            }
        }
        viewModelScope.launch {
            settingsRepo.dataSourcesSettings.collect { settings ->
                _uiState.update { it.copy(dataSourcesSettings = settings) }
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
            is SettingsAction.UpdateTLEFromFile -> updateTLEFromFile(action.uri)
            is SettingsAction.UpdateTransceiversFromFile -> updateTransceiversFromFile(action.uri)
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
            is SystemAction.ShowToast -> showToast(action.message)
        }
    }

    private fun handleAction(action: DataSourcesAction) {
        when (action) {
            is DataSourcesAction.SetUseCustomTle ->  settingsRepo.setUseCustomTle(action.value)
            is DataSourcesAction.SetUseCustomTransceivers ->  settingsRepo.setUseCustomTransceivers(action.value)
            is DataSourcesAction.SetTleUrl -> settingsRepo.setTleUrl(action.value)
            is DataSourcesAction.SetTransceiversUrl -> settingsRepo.setTransceiversUrl(action.value)
        }
    }

    private fun setGpsPosition() {
        if (settingsRepo.setStationPosition()) {
            val messageResId = R.string.prefs_loc_success
            val newPosSettings = _uiState.value.positionSettings.copy(
                isUpdating = true, messageResId = messageResId
            )
            _uiState.update { it.copy(positionSettings = newPosSettings) }
        } else {
            val errorResId = R.string.prefs_loc_gps_error
            val newPosSettings = _uiState.value.positionSettings.copy(
                isUpdating = false, messageResId = errorResId
            )
            _uiState.update { it.copy(positionSettings = newPosSettings) }
        }
    }

    private fun setGeoPosition(latitude: Double, longitude: Double) {
        if (settingsRepo.setStationPosition(latitude, longitude, 0.0)) {
            val messageResId = R.string.prefs_loc_success
            val newPosSettings = _uiState.value.positionSettings.copy(
                isUpdating = false, messageResId = messageResId
            )
            _uiState.update { it.copy(positionSettings = newPosSettings) }
        } else {
            val errorResId = R.string.prefs_loc_input_error
            val newPosSettings = _uiState.value.positionSettings.copy(
                isUpdating = false, messageResId = errorResId
            )
            _uiState.update { it.copy(positionSettings = newPosSettings) }
        }
    }

    private fun setQthPosition(locator: String) {
        if (settingsRepo.setStationPosition(locator)) {
            val messageResId = R.string.prefs_loc_success
            val newPosSettings = _uiState.value.positionSettings.copy(
                isUpdating = false, messageResId = messageResId
            )
            _uiState.update { it.copy(positionSettings = newPosSettings) }
        } else {
            val errorResId = R.string.prefs_loc_qth_error
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

    private fun updateTLEFromFile(uri: String) = viewModelScope.launch {
        try {
            val newDataSettings = _uiState.value.dataSettings.copy(isUpdating = true)
            _uiState.update { it.copy(dataSettings = newDataSettings) }
            databaseRepo.updateTLEFromFile(uri)
        } catch (exception: Exception) {
            val newDataSettings = _uiState.value.dataSettings.copy(isUpdating = false)
            _uiState.update { it.copy(dataSettings = newDataSettings) }
            println(exception)
        }
    }

    private fun updateTransceiversFromFile(uri: String) = viewModelScope.launch {
        try {
            val newDataSettings = _uiState.value.dataSettings.copy(isUpdating = true)
            _uiState.update { it.copy(dataSettings = newDataSettings) }
            databaseRepo.updateTransceiversFromFile(uri)
        } catch (exception: Exception) {
            val newDataSettings = _uiState.value.dataSettings.copy(isUpdating = false)
            _uiState.update { it.copy(dataSettings = newDataSettings) }
            println(exception)
        }
    }

    private fun setPassInfo(passes: List<OrbitalPass>, timeNow: Long) {
        if (passes.isEmpty()) return
        try {
            val nextPass = passes.first { it.aosTime.minus(timeNow) > 0 }
            val time = nextPass.aosTime.minus(timeNow).toTimerString()
            _uiState.update { it.copy(nextPass = nextPass, nextTime = time, isNextTimeAos = true) }
        } catch (_: NoSuchElementException) {
            val lastPass = passes.last()
            val time = lastPass.losTime.minus(timeNow).toTimerString()
            _uiState.update { it.copy(nextPass = lastPass, nextTime = time, isNextTimeAos = false) }
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
                    container.satelliteRepo,
                    container.settingsRepo,
                    container.provideShowToast(),
                    container.provideAppVersionName()
                )
            }
        }
    }
}
