package com.rtbishop.look4sat.presentation.settingsScreen

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rtbishop.look4sat.domain.IDataRepository
import com.rtbishop.look4sat.domain.ILocationHandler
import com.rtbishop.look4sat.domain.model.DataState
import com.rtbishop.look4sat.domain.predict.GeoPos
import com.rtbishop.look4sat.framework.SettingsHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val resolver: ContentResolver,
    private val settingsHandler: SettingsHandler,
    private val dataRepository: IDataRepository,
    private val locationHandler: ILocationHandler
) : ViewModel(), ILocationHandler {

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        println("Handled $exception in CoroutineExceptionHandler")
    }

    fun updateDataFromFile(uri: Uri) {
        viewModelScope.launch(exceptionHandler) {
            @Suppress("BlockingMethodInNonBlockingContext")
            resolver.openInputStream(uri)?.use { fileUri ->
                dataRepository.updateDataFromFile(fileUri)
            }
        }
    }

    fun updateDataFromWeb(sources: List<String>) {
        viewModelScope.launch(exceptionHandler) {
            dataRepository.updateDataFromWeb(sources)
        }
    }

    override val stationPosition: SharedFlow<DataState<GeoPos>> = locationHandler.stationPosition

    override fun getStationPosition(): GeoPos = locationHandler.getStationPosition()

    override fun setStationPosition(latitude: Double, longitude: Double) {
        locationHandler.setStationPosition(latitude, longitude)
    }

    override fun setPositionFromGps() = locationHandler.setPositionFromGps()

    override fun setPositionFromNet() = locationHandler.setPositionFromNet()

    override fun setPositionFromQth(qthString: String) {
        locationHandler.setPositionFromQth(qthString)
    }

    override fun setPositionHandled() = locationHandler.setPositionHandled()
}
