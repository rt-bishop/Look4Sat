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
import androidx.room.Room
import com.rtbishop.look4sat.data.DefaultRepository
import com.rtbishop.look4sat.data.SettingsHandler
import com.rtbishop.look4sat.domain.DataParser
import com.rtbishop.look4sat.domain.DataReporter
import com.rtbishop.look4sat.domain.DataRepository
import com.rtbishop.look4sat.domain.LocationHandler
import com.rtbishop.look4sat.domain.predict.Predictor
import com.rtbishop.look4sat.framework.LocationProvider
import com.rtbishop.look4sat.framework.SettingsProvider
import com.rtbishop.look4sat.framework.local.*
import com.rtbishop.look4sat.framework.remote.RemoteSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides
    @Singleton
    fun provideSatelliteRepo(
        settingsHandler: SettingsHandler,
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher
    ): DataRepository {
        val dataParser = DataParser(defaultDispatcher)
        val db = Room.databaseBuilder(context, SatelliteDb::class.java, "SatelliteDb")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .fallbackToDestructiveMigration().build()
        val localSource = LocalSource(db.entriesDao(), db.transmittersDao())
        val remoteSource = RemoteSource(ioDispatcher)
        return DefaultRepository(dataParser, localSource, remoteSource, settingsHandler)
    }

    @Provides
    @Singleton
    fun provideSettingsHandler(sharedPreferences: SharedPreferences): SettingsHandler {
        return SettingsProvider(sharedPreferences)
    }

    @Provides
    @Singleton
    fun provideLocationHandler(
        @ApplicationContext context: Context,
        settingsProvider: SettingsProvider
    ): LocationHandler {
        return LocationProvider(context, settingsProvider)
    }

    @Provides
    @Singleton
    fun providePredictor(@DefaultDispatcher dispatcher: CoroutineDispatcher): Predictor {
        return Predictor(dispatcher)
    }

    @Provides
    @Singleton
    fun provideDataReporter(@IoDispatcher dispatcher: CoroutineDispatcher): DataReporter {
        return DataReporter(dispatcher)
    }
}
