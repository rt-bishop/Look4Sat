package com.rtbishop.look4sat.core.domain.repository

import com.rtbishop.look4sat.core.domain.usecase.IAddToCalendar
import com.rtbishop.look4sat.core.domain.usecase.IShowToast
import kotlinx.coroutines.CoroutineScope

interface IMainContainer {
    val appScope: CoroutineScope
    val settingsRepo: ISettingsRepo
    val selectionRepo: ISelectionRepo
    val satelliteRepo: ISatelliteRepo
    val databaseRepo: IDatabaseRepo
    fun provideAddToCalendar(): IAddToCalendar
    fun provideShowToast(): IShowToast
    fun provideBluetoothReporter(): IReporter
    fun provideNetworkReporter(): IReporter
    fun provideSensorsRepo(): ISensorsRepo
    fun provideTxRadioController(): IRadioController
    fun provideRxRadioController(): IRadioController
    val radioTrackingService: IRadioTrackingService
}

interface IContainerProvider {
    fun getMainContainer(): IMainContainer
}
