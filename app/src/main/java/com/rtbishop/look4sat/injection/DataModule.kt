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
package com.rtbishop.look4sat.injection

import android.content.Context
import android.content.SharedPreferences
import android.location.LocationManager
import androidx.room.Room
import com.rtbishop.look4sat.data.LocalSource
import com.rtbishop.look4sat.data.Preferences
import com.rtbishop.look4sat.data.RemoteSource
import com.rtbishop.look4sat.data.SatelliteRepo
import com.rtbishop.look4sat.domain.PassReporter
import com.rtbishop.look4sat.domain.Predictor
import com.rtbishop.look4sat.framework.PreferencesSource
import com.rtbishop.look4sat.framework.api.RemoteDataSource
import com.rtbishop.look4sat.framework.api.SatelliteApi
import com.rtbishop.look4sat.framework.db.*
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
object DataModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder().build()
    }

    @Provides
    @Singleton
    fun providePreferenceSource(
        moshi: Moshi,
        locationManager: LocationManager,
        preferences: SharedPreferences
    ): Preferences {
        return PreferencesSource(moshi, locationManager, preferences)
    }

    @Provides
    @Singleton
    fun providePredictor(@DefaultDispatcher dispatcher: CoroutineDispatcher): Predictor {
        return Predictor(dispatcher)
    }

    @Provides
    @Singleton
    fun provideSatelliteRepo(
        preferences: Preferences,
        localSource: LocalSource,
        remoteSource: RemoteSource,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): SatelliteRepo {
        return SatelliteRepo(preferences, localSource, remoteSource, dispatcher)
    }

    @Provides
    fun provideLocalDataSource(satelliteDao: SatelliteDao): LocalSource {
        return LocalDataSource(satelliteDao)
    }

    @Provides
    @Singleton
    fun provideSatelliteDao(db: SatelliteDb): SatelliteDao {
        return db.satelliteDao()
    }

    @Provides
    @Singleton
    fun provideSatelliteDb(@ApplicationContext context: Context, moshi: Moshi): SatelliteDb {
        RoomConverters.initialize(moshi)
        return Room.databaseBuilder(context, SatelliteDb::class.java, "SatelliteDb")
            .addMigrations(MIGRATION_1_2).build()
    }

    @Provides
    @Singleton
    fun provideRemoteDataSource(satelliteApi: SatelliteApi): RemoteSource {
        return RemoteDataSource(satelliteApi)
    }

    @Provides
    @Singleton
    fun provideSatelliteService(): SatelliteApi {
        return Retrofit.Builder()
            .baseUrl("https://db.satnogs.org/api/")
            .addConverterFactory(MoshiConverterFactory.create())
            .build().create(SatelliteApi::class.java)
    }

    @Provides
    @Singleton
    fun provideDataReporter(@IoDispatcher dispatcher: CoroutineDispatcher): PassReporter {
        return PassReporter(dispatcher)
    }
}
