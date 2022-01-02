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
package com.rtbishop.look4sat.presentation.settingsScreen

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rtbishop.look4sat.domain.DataRepository
import com.rtbishop.look4sat.framework.PreferencesSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: PreferencesSource,
    private val resolver: ContentResolver,
    private val dataRepository: DataRepository
) : ViewModel() {

    private val coroutineHandler = CoroutineExceptionHandler { _, throwable ->
//        Timber.d(throwable)
//        _satData.value = DataState.Error(throwable)
    }

    fun updateDataFromFile(uri: Uri) {
        viewModelScope.launch(coroutineHandler) {
//            _satData.value = DataState.Loading
            runCatching {
                resolver.openInputStream(uri)?.use { stream ->
                    dataRepository.updateDataFromFile(stream)
                }
            }.onFailure {
//                _satData.value = DataState.Error(it)
            }
        }
    }

    fun updateDataFromWeb(sources: List<String>) {
        viewModelScope.launch(coroutineHandler) {
//            _satData.value = DataState.Loading
            dataRepository.updateDataFromWeb(sources)
        }
    }
}
