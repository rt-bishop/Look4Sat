package com.rtbishop.look4sat.presentation.settingsScreen

import androidx.lifecycle.ViewModel
import com.rtbishop.look4sat.data.ISettingsHandler
import com.rtbishop.look4sat.domain.IDataRepository
import com.rtbishop.look4sat.domain.ILocationHandler
import com.rtbishop.look4sat.domain.model.DataState
import com.rtbishop.look4sat.domain.predict.GeoPos
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: ISettingsHandler,
    private val repository: IDataRepository,
    private val locationHandler: ILocationHandler
) : ViewModel() {

    fun updateDataFromFile(uri: String) {
        repository.updateDataFromFile(uri)
    }

    fun updateDataFromWeb(sources: List<String>) {
        repository.updateDataFromWeb(sources)
    }

    fun clearData() {
        repository.clearData()
    }

    fun getUseUTC(): Boolean = settings.getUseUTC()

    fun setUseUTC(value: Boolean) = settings.setUseUTC(value)

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

    fun getUpdateState() = repository.dataUpdateState

    fun setUpdateHandled() = repository.setDataUpdateHandled()

    val stationPosition: SharedFlow<DataState<GeoPos>> = locationHandler.stationPosition

    fun getStationPosition(): GeoPos = locationHandler.getStationPosition()

    fun setPositionFromGps() = locationHandler.setPositionFromGps()

    fun setPositionFromNet() = locationHandler.setPositionFromNet()

    fun setPositionFromQth(qthString: String) = locationHandler.setPositionFromQth(qthString)

    fun setPositionHandled() = locationHandler.setPositionHandled()
}
