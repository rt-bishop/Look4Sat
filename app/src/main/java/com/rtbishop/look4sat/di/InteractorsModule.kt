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
package com.rtbishop.look4sat.di

import com.rtbishop.look4sat.data.SatelliteRepo
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
    fun provideImportDataFromStreamUseCase(satelliteRepo: SatelliteRepo): ImportDataFromStream {
        return ImportDataFromStream(satelliteRepo)
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
