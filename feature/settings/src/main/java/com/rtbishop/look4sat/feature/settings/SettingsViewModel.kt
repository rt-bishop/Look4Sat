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
package com.rtbishop.look4sat.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rtbishop.look4sat.core.domain.repository.IContainerProvider
import com.rtbishop.look4sat.core.domain.repository.IDatabaseRepo
import com.rtbishop.look4sat.core.domain.repository.ISettingsRepo
import com.rtbishop.look4sat.core.domain.usecase.IShowToast
import com.rtbishop.look4sat.core.presentation.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val databaseRepo: IDatabaseRepo,
    private val settingsRepo: ISettingsRepo,
    private val showToast: IShowToast
) : ViewModel() {

    private val defaultPosSettings = PositionSettings(false, settingsRepo.stationPosition.value, 0)
    private val defaultDataSettings = DataSettings(false, 0, 0, 0L)
    private val _uiState = MutableStateFlow(
        SettingsState(
            appVersionName = settingsRepo.appVersionName,
            positionSettings = defaultPosSettings,
            dataSettings = defaultDataSettings,
            otherSettings = settingsRepo.otherSettings.value,
            rcSettings = settingsRepo.rcSettings.value,
            radioControlSettings = settingsRepo.radioControlSettings.value,
            dataSourcesSettings = settingsRepo.dataSourcesSettings.value
        )
    )

    val uiState: StateFlow<SettingsState> = _uiState

    init {
        viewModelScope.launch {
            settingsRepo.stationPosition.collect { geoPos ->
                _uiState.update {
                    it.copy(positionSettings = it.positionSettings.copy(isUpdating = false, stationPos = geoPos))
                }
            }
        }
        viewModelScope.launch {
            settingsRepo.databaseState.collect { state ->
                _uiState.update {
                    it.copy(
                        dataSettings = DataSettings(
                            false,
                            state.numberOfSatellites,
                            state.numberOfRadios,
                            state.updateTimestamp
                        )
                    )
                }
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
        viewModelScope.launch {
            settingsRepo.radioControlSettings.collect { settings ->
                _uiState.update { it.copy(radioControlSettings = settings) }
            }
        }
    }


    fun onAction(action: SettingsAction) {
        when (action) {
            // Position
            SettingsAction.SetGpsPosition -> setGpsPosition()
            is SettingsAction.SetGeoPosition -> setGeoPosition(action.latitude, action.longitude)
            is SettingsAction.SetQthPosition -> setQthPosition(action.locator)
            SettingsAction.DismissPosMessages -> dismissPosMessage()
            // Data
            SettingsAction.UpdateFromWeb -> runDataUpdate { databaseRepo.updateFromRemote() }
            is SettingsAction.UpdateTLEFromFile -> runDataUpdate { databaseRepo.updateTLEFromFile(action.uri) }
            is SettingsAction.UpdateTransceiversFromFile -> runDataUpdate {
                databaseRepo.updateTransceiversFromFile(
                    action.uri
                )
            }
            SettingsAction.ClearAllData -> viewModelScope.launch { databaseRepo.clearAllData() }
            // Toggles
            is SettingsAction.ToggleUtc -> settingsRepo.updateOtherSettings { it.copy(stateOfUtc = action.value) }
            is SettingsAction.ToggleUpdate -> settingsRepo.updateOtherSettings { it.copy(stateOfAutoUpdate = action.value) }
            is SettingsAction.ToggleSweep -> settingsRepo.updateOtherSettings { it.copy(stateOfSweep = action.value) }
            is SettingsAction.ToggleSensor -> settingsRepo.updateOtherSettings { it.copy(stateOfSensors = action.value) }
            is SettingsAction.ToggleLightTheme -> settingsRepo.updateOtherSettings { it.copy(stateOfLightTheme = action.value) }
            is SettingsAction.ToggleNightMode -> settingsRepo.updateOtherSettings { it.copy(stateOfNightMode = action.value) }
            // Remote control & data sources
            is SettingsAction.UpdateRC -> settingsRepo.updateRCSettings(action.settings)
            is SettingsAction.UpdateRadioControl -> settingsRepo.updateRadioControlSettings(action.settings)
            is SettingsAction.UpdateDataSources -> settingsRepo.updateDataSourcesSettings(action.settings)
            // System
            is SettingsAction.ShowToast -> showToast(action.message)
        }
    }

    // region Position helpers — consolidated from 3 near-identical functions

    private fun setGpsPosition() {
        updatePosition(R.string.prefs_loc_gps_error) { settingsRepo.setStationPosition() }
    }

    private fun setGeoPosition(latitude: Double, longitude: Double) {
        updatePosition(R.string.prefs_loc_input_error) { settingsRepo.setStationPosition(latitude, longitude, 0.0) }
    }

    private fun setQthPosition(locator: String) {
        updatePosition(R.string.prefs_loc_qth_error) { settingsRepo.setStationPosition(locator) }
    }

    /**
     * Common position update logic. Calls [action], emits success or error message.
     */
    private inline fun updatePosition(errorResId: Int, action: () -> Boolean) {
        val success = action()
        val resId = if (success) R.string.prefs_loc_success else errorResId
        val isUpdating = success // GPS update is async; geo/qth are immediate
        _uiState.update {
            it.copy(positionSettings = it.positionSettings.copy(isUpdating = isUpdating, messageResId = resId))
        }
    }

    private fun dismissPosMessage() {
        _uiState.update {
            it.copy(positionSettings = it.positionSettings.copy(isUpdating = false, messageResId = 0))
        }
    }

    // endregion

    // region Data update helpers — consolidated from 3 near-identical functions

    /**
     * Common data update logic. Sets isUpdating=true, runs the suspending [block],
     * and resets isUpdating=false on failure.
     */
    private fun runDataUpdate(block: suspend () -> Unit) = viewModelScope.launch {
        try {
            _uiState.update {
                it.copy(dataSettings = it.dataSettings.copy(isUpdating = true))
            }
            block()
        } catch (exception: Exception) {
            _uiState.update {
                it.copy(dataSettings = it.dataSettings.copy(isUpdating = false))
            }
            println(exception)
        }
    }

    // endregion

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            val applicationKey = ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY
            initializer {
                val container = (this[applicationKey] as IContainerProvider).getMainContainer()
                SettingsViewModel(
                    container.databaseRepo,
                    container.settingsRepo,
                    container.provideShowToast()
                )
            }
        }
    }
}
