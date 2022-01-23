package com.rtbishop.look4sat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rtbishop.look4sat.domain.DataRepository
import com.rtbishop.look4sat.domain.predict.Predictor
import com.rtbishop.look4sat.framework.SettingsProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val predictor: Predictor,
    private val preferences: SettingsProvider,
    private val dataRepository: DataRepository
) : ViewModel() {

    fun calculatePasses(
        hoursAhead: Int = preferences.getHoursAhead(),
        minElevation: Double = preferences.getMinElevation(),
        timeRef: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val satellites = dataRepository.getSelectedSatellites()
            val stationPos = preferences.loadStationPosition()
            predictor.forceCalculation(satellites, stationPos, timeRef, hoursAhead, minElevation)
        }
    }
}
