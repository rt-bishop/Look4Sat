package com.rtbishop.look4sat.presentation.sourcesScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.rtbishop.look4sat.domain.DataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SourcesViewModel @Inject constructor(dataRepository: DataRepository) : ViewModel() {

    val sources = liveData {
        emit(dataRepository.getWebSources())
    }
}
