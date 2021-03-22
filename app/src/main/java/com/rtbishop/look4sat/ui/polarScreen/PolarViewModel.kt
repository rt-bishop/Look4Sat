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
package com.rtbishop.look4sat.ui.polarScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import com.rtbishop.look4sat.data.model.Result
import com.rtbishop.look4sat.data.model.SatPass
import com.rtbishop.look4sat.data.repository.PassesRepo
import com.rtbishop.look4sat.data.repository.PrefsRepo
import com.rtbishop.look4sat.data.repository.SatelliteRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

@HiltViewModel
class PolarViewModel @Inject constructor(
    private val prefsRepo: PrefsRepo,
    private val passesRepo: PassesRepo,
    private val satelliteRepo: SatelliteRepo
) : ViewModel() {

    fun getAppTimer() = liveData {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000)
        }
    }

    fun getPass(passId: Int) = liveData {
        passesRepo.passes.collect { passes ->
            if (passes is Result.Success) {
                emit(passes.data[passId])
            }
        }
    }

    fun getPasses() = passesRepo.passes.asLiveData()

    fun shouldUseCompass(): Boolean {
        return prefsRepo.shouldUseCompass()
    }

    fun getMagDeclination(): Float {
        return prefsRepo.getMagDeclination()
    }

    fun getTransmittersForSat(satId: Int) = satelliteRepo.getSatTransmitters(satId).asLiveData()
}