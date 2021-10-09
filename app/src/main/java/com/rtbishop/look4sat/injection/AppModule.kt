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

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.hardware.SensorManager
import android.location.LocationManager
import androidx.preference.PreferenceManager
import androidx.room.Room
import com.rtbishop.look4sat.domain.Constants
import com.rtbishop.look4sat.framework.PreferencesSource
import com.rtbishop.look4sat.framework.local.Converters
import com.rtbishop.look4sat.framework.local.MIGRATION_1_2
import com.rtbishop.look4sat.framework.local.SatelliteDao
import com.rtbishop.look4sat.framework.local.SatelliteDb
import com.rtbishop.look4sat.framework.remote.SatelliteApi
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver {
        return context.contentResolver
    }

    @Provides
    fun provideLocationManager(@ApplicationContext context: Context): LocationManager {
        return context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    @Provides
    fun provideSensorManager(@ApplicationContext context: Context): SensorManager {
        return context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    @Provides
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

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
    ): PreferencesSource {
        return PreferencesSource(moshi, locationManager, preferences)
    }

    @Provides
    @Singleton
    fun provideSatelliteApi(): SatelliteApi {
        return Retrofit.Builder()
            .baseUrl(Constants.URL_BASE)
            .addConverterFactory(MoshiConverterFactory.create())
            .build().create(SatelliteApi::class.java)
    }

    @Provides
    @Singleton
    fun provideSatelliteDao(db: SatelliteDb): SatelliteDao {
        return db.satelliteDao()
    }

    @Provides
    @Singleton
    fun provideSatelliteDb(@ApplicationContext context: Context, moshi: Moshi): SatelliteDb {
        Converters.initialize(moshi)
        return Room.databaseBuilder(context, SatelliteDb::class.java, "SatelliteDb")
            .addMigrations(MIGRATION_1_2).build()
    }
}
