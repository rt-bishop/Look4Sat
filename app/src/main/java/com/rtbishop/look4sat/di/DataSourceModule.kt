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

import android.content.Context
import android.content.SharedPreferences
import android.location.LocationManager
import androidx.room.Room
import com.rtbishop.look4sat.data.*
import com.rtbishop.look4sat.domain.predict4kotlin.QthConverter
import com.rtbishop.look4sat.framework.DefaultLocationSource
import com.rtbishop.look4sat.framework.api.NetworkDataSource
import com.rtbishop.look4sat.framework.api.SatelliteService
import com.rtbishop.look4sat.framework.db.RoomConverters
import com.rtbishop.look4sat.framework.db.RoomDataSource
import com.rtbishop.look4sat.framework.db.SatelliteDao
import com.rtbishop.look4sat.framework.db.SatelliteDb
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataSourceModule {

    @Provides
    fun provideMoshi(): Moshi {
        return Moshi.Builder().build()
    }

    @Provides
    @Singleton
    fun provideLocationSource(
        locationManager: LocationManager,
        preferences: SharedPreferences
    ): LocationSource {
        return DefaultLocationSource(locationManager, preferences)
    }

    @Provides
    @Singleton
    fun provideLocationRepo(
        locationSource: LocationSource,
        qthConverter: QthConverter
    ): LocationRepo {
        return LocationRepo(locationSource, qthConverter)
    }

    @Provides
    @Singleton
    fun provideSatelliteRepo(
        localSource: LocalDataSource,
        remoteSource: RemoteDataSource,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): SatelliteRepo {
        return SatelliteRepo(localSource, remoteSource, ioDispatcher)
    }

    @Provides
    fun provideLocalDataSource(satelliteDao: SatelliteDao): LocalDataSource {
        return RoomDataSource(satelliteDao)
    }

    @Provides
    fun provideSatDataDao(db: SatelliteDb): SatelliteDao {
        return db.satelliteDao()
    }

    @Provides
    fun provideSatelliteDb(@ApplicationContext context: Context, moshi: Moshi): SatelliteDb {
        RoomConverters.initialize(moshi)
        return Room.databaseBuilder(context, SatelliteDb::class.java, "SatelliteDb").build()
    }

    @Provides
    fun provideRemoteDataSource(satelliteService: SatelliteService): RemoteDataSource {
        return NetworkDataSource(satelliteService)
    }

    @Provides
    fun provideSatDataService(): SatelliteService {
        return Retrofit.Builder()
            .baseUrl("https://db.satnogs.org/api/")
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(SatelliteService::class.java)
    }
}
