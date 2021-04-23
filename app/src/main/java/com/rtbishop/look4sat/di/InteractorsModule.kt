package com.rtbishop.look4sat.di

import com.rtbishop.look4sat.domain.SatelliteRepo
import com.rtbishop.look4sat.interactors.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object InteractorsModule {

    @Provides
    fun provideGetSatItemsUseCase(satelliteRepo: SatelliteRepo): GetSatItems {
        return GetSatItems(satelliteRepo)
    }

    @Provides
    fun provideGetSelectedSatellitesUseCase(satelliteRepo: SatelliteRepo): GetSelectedSatellites {
        return GetSelectedSatellites(satelliteRepo)
    }

    @Provides
    fun provideGetTransmittersForSatUseCase(satelliteRepo: SatelliteRepo): GetTransmittersForSat {
        return GetTransmittersForSat(satelliteRepo)
    }

    @Provides
    fun provideImportDataFromFileUseCase(satelliteRepo: SatelliteRepo): ImportDataFromFile {
        return ImportDataFromFile(satelliteRepo)
    }

    @Provides
    fun provideImportDataFromWebUseCase(satelliteRepo: SatelliteRepo): ImportDataFromWeb {
        return ImportDataFromWeb(satelliteRepo)
    }

    @Provides
    fun provideUpdateEntriesSelectionUseCase(satelliteRepo: SatelliteRepo): UpdateEntriesSelection {
        return UpdateEntriesSelection(satelliteRepo)
    }
}
