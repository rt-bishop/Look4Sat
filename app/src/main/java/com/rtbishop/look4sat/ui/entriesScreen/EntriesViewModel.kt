/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2021 Arty Bishop (bishop.arty@gmail.com)
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
package com.rtbishop.look4sat.ui.entriesScreen

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.rtbishop.look4sat.data.model.Result
import com.rtbishop.look4sat.data.model.SatItem
import com.rtbishop.look4sat.data.model.TleSource
import com.rtbishop.look4sat.data.repository.PrefsRepo
import com.rtbishop.look4sat.data.repository.SatelliteRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.system.measureTimeMillis

@FlowPreview
@HiltViewModel
class EntriesViewModel @Inject constructor(
    private val prefsRepo: PrefsRepo,
    private val satelliteRepo: SatelliteRepo
) : ViewModel() {

    private val satDataState = MutableSharedFlow<Result<List<SatItem>>>(replay = 0)
    private val satDataFlow = satelliteRepo.getAllSatItems().map { Result.Success(it) }
    val satData =
        flowOf(satDataState, satDataFlow).flattenMerge().asLiveData(viewModelScope.coroutineContext)

    fun importSatDataFromFile(uri: Uri) {
        viewModelScope.launch {
            satDataState.emit(Result.InProgress)
            try {
                satelliteRepo.importSatDataFromFile(uri)
            } catch (exception: Exception) {
                satDataState.emit(Result.Error(exception))
            }
        }
    }

    fun importSatDataFromSources(sources: List<TleSource> = prefsRepo.loadDefaultSources()) {
        viewModelScope.launch {
            satDataState.emit(Result.InProgress)
            val updateMillis = measureTimeMillis {
                try {
                    prefsRepo.saveTleSources(sources)
                    satelliteRepo.importSatDataFromWeb(sources)
                } catch (exception: Exception) {
                    satDataState.emit(Result.Error(exception))
                }
            }
            Timber.d("Update from WEB took $updateMillis ms")
        }
    }

    fun updateEntriesSelection(items: List<Int>, isSelected: Boolean) {
        viewModelScope.launch {
            satelliteRepo.updateEntriesSelection(items, isSelected)
        }
    }
}
