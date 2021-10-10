package com.rtbishop.look4sat.presentation.sourcesScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.rtbishop.look4sat.domain.SatelliteRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SourcesViewModel @Inject constructor(satelliteRepo: SatelliteRepo) : ViewModel() {

    val sources = liveData {
        emit(satelliteRepo.getSavedSources())
    }
}
