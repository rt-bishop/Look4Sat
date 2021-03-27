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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
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

    private val allSatItems = mutableListOf<SatItem>()
    private val satDataState = MutableStateFlow<Result<List<SatItem>>>(Result.InProgress)
    val satData = satDataState.asLiveData(viewModelScope.coroutineContext)

    init {
        viewModelScope.launch {
            satelliteRepo.getAllSatItems().collect { items ->
                allSatItems.clear()
                allSatItems.addAll(items)
                filterByModes(getModesSelection())
            }
        }
    }

    fun getModesSelection(): List<String> {
        return prefsRepo.loadModesSelection()
    }

    fun filterByModes(modes: List<String>) {
        if (modes.isEmpty()) {
            satDataState.value = Result.Success(allSatItems)
        } else {
            val itemsWithModes = allSatItems.filter { item ->
                item.modes.any { mode -> mode in modes }
            }
            satDataState.value = Result.Success(itemsWithModes)
        }
        prefsRepo.saveModesSelection(modes)
    }

    fun importSatDataFromFile(uri: Uri) {
        viewModelScope.launch {
            satDataState.value = Result.InProgress
            try {
                satelliteRepo.importSatDataFromFile(uri)
            } catch (exception: Exception) {
                satDataState.value = Result.Error(exception)
            }
        }
    }

    fun importSatDataFromSources(sources: List<TleSource> = prefsRepo.loadDefaultSources()) {
        viewModelScope.launch {
            satDataState.value = Result.InProgress
            val updateMillis = measureTimeMillis {
                try {
                    prefsRepo.saveTleSources(sources)
                    satelliteRepo.importSatDataFromWeb(sources)
                } catch (exception: Exception) {
                    satDataState.value = Result.Error(exception)
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
