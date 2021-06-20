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
import com.rtbishop.look4sat.data.*
import com.rtbishop.look4sat.domain.predict4kotlin.QthConverter
import com.rtbishop.look4sat.framework.PreferencesProvider
import com.rtbishop.look4sat.framework.api.SatDataRemote
import com.rtbishop.look4sat.framework.api.SatDataService
import com.rtbishop.look4sat.framework.db.RoomConverters
import com.rtbishop.look4sat.framework.db.SatDataDao
import com.rtbishop.look4sat.framework.db.SatDataDb
import com.rtbishop.look4sat.framework.db.SatDataLocal
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton
import javax.net.ssl.HostnameVerifier

@Module
@InstallIn(SingletonComponent::class)
object SatDataModule {

    @Provides
    fun provideQthConverter(): QthConverter {
        return QthConverter()
    }

    @Provides
    fun provideMoshi(): Moshi {
        return Moshi.Builder().build()
    }

    @Provides
    @Singleton
    fun providePreferenceSource(
        moshi: Moshi,
        qthConverter: QthConverter,
        locationManager: LocationManager,
        preferences: SharedPreferences
    ): PreferencesSource {
        return PreferencesProvider(moshi, qthConverter, locationManager, preferences)
    }

    @Provides
    @Singleton
    fun providePassesRepo(
        preferencesSource: PreferencesSource,
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher
    ): SatPassRepository {
        return SatPassRepository(preferencesSource, defaultDispatcher)
    }

    @Provides
    @Singleton
    fun provideSatelliteRepo(
        preferencesSource: PreferencesSource,
        satLocalSource: SatDataLocalSource,
        satRemoteSource: SatDataRemoteSource,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): SatDataRepository {
        return SatDataRepository(preferencesSource, satLocalSource, satRemoteSource, ioDispatcher)
    }

    @Provides
    fun provideLocalDataSource(satDataDao: SatDataDao): SatDataLocalSource {
        return SatDataLocal(satDataDao)
    }

    @Provides
    fun provideSatDataDao(db: SatDataDb): SatDataDao {
        return db.satelliteDao()
    }

    @Provides
    fun provideSatelliteDb(@ApplicationContext context: Context, moshi: Moshi): SatDataDb {
        RoomConverters.initialize(moshi)
        return Room.databaseBuilder(context, SatDataDb::class.java, "SatelliteDb").build()
    }

    @Provides
    fun provideRemoteDataSource(satDataService: SatDataService): SatDataRemoteSource {
        return SatDataRemote(satDataService)
    }

    @Provides
    fun provideSatDataService(): SatDataService {
        val verifier = HostnameVerifier { _, _ -> true }
        val httpClient = OkHttpClient.Builder().hostnameVerifier(verifier).build()
        return Retrofit.Builder()
            .baseUrl("https://db.satnogs.org/api/")
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(SatDataService::class.java)
    }
}
