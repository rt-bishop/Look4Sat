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
import androidx.room.Room
import com.rtbishop.look4sat.domain.IDataRepository
import com.rtbishop.look4sat.domain.ILocationManager
import com.rtbishop.look4sat.domain.ISatelliteManager
import com.rtbishop.look4sat.domain.ISettingsManager
import com.rtbishop.look4sat.domain.data.DataRepository
import com.rtbishop.look4sat.domain.predict.SatelliteManager
import com.rtbishop.look4sat.framework.LocationManager
import com.rtbishop.look4sat.framework.SettingsManager
import com.rtbishop.look4sat.framework.data.*
import com.rtbishop.look4sat.utility.DataParser
import com.rtbishop.look4sat.utility.DataReporter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BaseModule {

    @Provides
    @Singleton
    fun provideDataRepository(@ApplicationContext context: Context): IDataRepository {
        val db = Room.databaseBuilder(context, LocalDatabase::class.java, "Look4SatDb")
            .addMigrations(MIGRATION_1_2).fallbackToDestructiveMigration().build()
        val parser = DataParser(Dispatchers.Default)
        val fileSource = FileDataSource(context.contentResolver, Dispatchers.IO)
        val entries = LocalEntrySource(db.entriesDao())
        val radios = LocalRadioSource(db.radiosDao())
        val remoteSource = RemoteDataSource(Dispatchers.IO)
        val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        return DataRepository(parser, fileSource, entries, radios, remoteSource, repositoryScope)
    }

    @Provides
    @Singleton
    fun provideDataReporter(): DataReporter = DataReporter(CoroutineScope(Dispatchers.IO))

    @Provides
    @Singleton
    fun provideLocationManager(manager: LocationManager): ILocationManager = manager

    @Provides
    @Singleton
    fun provideSatelliteManager(): ISatelliteManager = SatelliteManager(Dispatchers.Default)

    @Provides
    @Singleton
    fun provideSettingsManager(manager: SettingsManager): ISettingsManager = manager
}
