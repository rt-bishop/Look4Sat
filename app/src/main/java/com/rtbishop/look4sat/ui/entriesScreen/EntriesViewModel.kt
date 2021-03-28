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

import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.data.model.Result
import com.rtbishop.look4sat.data.model.SatItem
import com.rtbishop.look4sat.data.model.TleSource
import com.rtbishop.look4sat.data.repository.PrefsRepo
import com.rtbishop.look4sat.data.repository.SatelliteRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.system.measureTimeMillis

@HiltViewModel
class EntriesViewModel @Inject constructor(
    private val prefsRepo: PrefsRepo,
    private val satelliteRepo: SatelliteRepo
) : ViewModel(), EntriesAdapter.EntriesClickListener {

    private var allSatItems = listOf<SatItem>()
    private val satDataState = MutableStateFlow<Result<List<SatItem>>>(Result.InProgress)
    val satData = satDataState.asLiveData(viewModelScope.coroutineContext)

    init {
        loadAndFilterData()
    }

    private fun loadAndFilterData() {
        viewModelScope.launch {
            allSatItems = satelliteRepo.getAllSatItems()
            filterByModes(prefsRepo.loadModesSelection())
        }
    }

    private fun filterByModes(modes: List<String>) {
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
                loadAndFilterData()
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
                    loadAndFilterData()
                } catch (exception: Exception) {
                    satDataState.value = Result.Error(exception)
                }
            }
            Timber.d("Update from WEB took $updateMillis ms")
        }
    }

    fun createModesDialog(context: Context): AlertDialog {
        val modes = arrayOf(
            "AFSK", "AFSK S-Net", "AFSK SALSAT", "AHRPT", "AM", "APT", "BPSK", "BPSK PMT-A3",
            "CERTO", "CW", "DQPSK", "DSTAR", "DUV", "FFSK", "FM", "FMN", "FSK", "FSK AX.100 Mode 5",
            "FSK AX.100 Mode 6", "FSK AX.25 G3RUH", "GFSK", "GFSK Rktr", "GMSK", "HRPT", "LoRa",
            "LRPT", "LSB", "MFSK", "MSK", "MSK AX.100 Mode 5", "MSK AX.100 Mode 6", "OFDM", "OQPSK",
            "PSK", "PSK31", "PSK63", "QPSK", "QPSK31", "QPSK63", "SSTV", "USB", "WSJT"
        )
        val savedModes = BooleanArray(modes.size)
        val selectedModes = prefsRepo.loadModesSelection().toMutableList()
        selectedModes.forEach { savedModes[modes.indexOf(it)] = true }
        val dialogBuilder = MaterialAlertDialogBuilder(context).apply {
            setTitle(context.getString(R.string.modes_title))
            setMultiChoiceItems(modes, savedModes) { _, which, isChecked ->
                when {
                    isChecked -> selectedModes.add(modes[which])
                    selectedModes.contains(modes[which]) -> selectedModes.remove(modes[which])
                }
            }
            setPositiveButton(context.getString(android.R.string.ok)) { _, _ ->
                filterByModes(selectedModes)
            }
            setNeutralButton(context.getString(android.R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
        }
        return dialogBuilder.create()
    }

    override fun updateSelection(catNums: List<Int>, isSelected: Boolean) {
        viewModelScope.launch {
            satelliteRepo.updateEntriesSelection(catNums, isSelected)
        }
    }
}
