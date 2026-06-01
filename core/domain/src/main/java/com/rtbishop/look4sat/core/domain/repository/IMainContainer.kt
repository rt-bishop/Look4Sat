/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2026 Arty Bishop and contributors.
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
package com.rtbishop.look4sat.core.domain.repository

import com.rtbishop.look4sat.core.domain.usecase.IAddToCalendar
import com.rtbishop.look4sat.core.domain.usecase.IAudioCapture
import com.rtbishop.look4sat.core.domain.usecase.ISaveImage
import com.rtbishop.look4sat.core.domain.usecase.IShowToast
import kotlinx.coroutines.CoroutineScope

interface IMainContainer {
    val appScope: CoroutineScope
    val settingsRepo: ISettingsRepo
    val selectionRepo: ISelectionRepo
    val satelliteRepo: ISatelliteRepo
    val databaseRepo: IDatabaseRepo
    val radioTrackingService: IRadioTrackingService
    fun provideAddToCalendar(): IAddToCalendar
    fun provideShowToast(): IShowToast
    fun provideBluetoothReporter(): IReporter
    fun provideNetworkReporter(): IReporter
    fun provideSensorsRepo(): ISensorsRepo
    fun provideTxRadioController(): IRadioController
    fun provideRxRadioController(): IRadioController
    fun provideAudioCapture(): IAudioCapture
    fun provideSaveImage(): ISaveImage
}

interface IContainerProvider {
    fun getMainContainer(): IMainContainer
}
