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

import android.bluetooth.BluetoothManager
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
import com.rtbishop.look4sat.framework.data.FileDataSource
import com.rtbishop.look4sat.framework.data.LocalDatabase
import com.rtbishop.look4sat.framework.data.LocalEntrySource
import com.rtbishop.look4sat.framework.data.LocalRadioSource
import com.rtbishop.look4sat.framework.data.MIGRATION_1_2
import com.rtbishop.look4sat.framework.data.RemoteDataSource
import com.rtbishop.look4sat.presentation.radarScreen.BTReporter
import com.rtbishop.look4sat.utility.DataParser
import com.rtbishop.look4sat.utility.DataReporter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object BaseModule {

    @Provides
    @Singleton
    fun provideDataRepository(
        @ApplicationContext context: Context,
        settings: ISettingsManager
    ): IDataRepository {
        val db = Room.databaseBuilder(context, LocalDatabase::class.java, "Look4SatDb")
            .addMigrations(MIGRATION_1_2).fallbackToDestructiveMigration().build()
        val parser = DataParser(Dispatchers.Default)
        val fileSource = FileDataSource(context.contentResolver, Dispatchers.IO)
        val entries = LocalEntrySource(db.entriesDao())
        val radios = LocalRadioSource(db.radiosDao())
        val remoteSource = RemoteDataSource(OkHttpClient.Builder().build(), Dispatchers.IO)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        return DataRepository(parser, fileSource, entries, radios, remoteSource, scope, settings)
    }

    @Provides
    @Singleton
    fun provideDataReporter(): DataReporter = DataReporter(CoroutineScope(Dispatchers.IO))

    @Provides
    @Singleton
    fun provideBTReporter(manager: BluetoothManager): BTReporter {
        return BTReporter(manager, CoroutineScope(Dispatchers.IO))
    }

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
