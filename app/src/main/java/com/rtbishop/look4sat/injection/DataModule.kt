/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2022 Arty Bishop (bishop.arty@gmail.com)
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
import com.rtbishop.look4sat.data.DataParser
import com.rtbishop.look4sat.data.Repository
import com.rtbishop.look4sat.domain.DataReporter
import com.rtbishop.look4sat.domain.ILocationHandler
import com.rtbishop.look4sat.domain.IRepository
import com.rtbishop.look4sat.domain.ISettings
import com.rtbishop.look4sat.domain.predict.Predictor
import com.rtbishop.look4sat.framework.LocationHandler
import com.rtbishop.look4sat.framework.SettingsHandler
import com.rtbishop.look4sat.framework.data.Provider
import com.rtbishop.look4sat.framework.data.Storage
import com.rtbishop.look4sat.framework.data.StorageDb
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideSatelliteRepo(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher
    ): IRepository {
        val db = Room.databaseBuilder(context, StorageDb::class.java, "Look4SatDb")
            .fallbackToDestructiveMigration().build()
        val dataParser = DataParser(defaultDispatcher)
        val localSource = Storage(db.entriesDao(), db.radiosDao())
        val remoteSource = Provider(context.contentResolver, ioDispatcher)
        val repositoryScope = CoroutineScope(SupervisorJob())
        return Repository(dataParser, localSource, remoteSource, repositoryScope)
    }

    @Provides
    @Singleton
    fun provideSettingsHandler(sharedPreferences: SharedPreferences): ISettings {
        return SettingsHandler(sharedPreferences)
    }

    @Provides
    @Singleton
    fun provideLocationHandler(
        @ApplicationContext context: Context,
        settings: ISettings
    ): ILocationHandler {
        return LocationHandler(context, settings)
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
