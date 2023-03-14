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
package com.rtbishop.look4sat

import android.bluetooth.BluetoothManager
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.LocationManager
import androidx.room.Room
import com.rtbishop.look4sat.data.DataParser
import com.rtbishop.look4sat.data.DataRepository
import com.rtbishop.look4sat.domain.*
import com.rtbishop.look4sat.framework.*
import com.rtbishop.look4sat.framework.data.*
import com.rtbishop.look4sat.framework.data.local.EntriesStorage
import com.rtbishop.look4sat.framework.data.local.RadiosStorage
import com.rtbishop.look4sat.framework.data.remote.FileSource
import com.rtbishop.look4sat.framework.data.remote.NetworkSource
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
object MainModule {

    @Provides
    @Singleton
    fun provideBluetoothReporter(@ApplicationContext context: Context): BluetoothReporter {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return BluetoothReporter(manager, CoroutineScope(Dispatchers.IO))
    }

    @Provides
    @Singleton
    fun provideNetworkReporter(): NetworkReporter = NetworkReporter(CoroutineScope(Dispatchers.IO))

    @Provides
    @Singleton
    fun provideDataRepository(
        @ApplicationContext context: Context, settings: ISettingsSource
    ): IDataRepository {
        val db = Room.databaseBuilder(context, MainDatabase::class.java, "Look4SatDb")
            .addMigrations(MIGRATION_1_2).fallbackToDestructiveMigration().build()
        val parser = DataParser(Dispatchers.Default)
        val fileSource = FileSource(context.contentResolver, Dispatchers.IO)
        val entries = EntriesStorage(db.entriesDao())
        val radios = RadiosStorage(db.radiosDao())
        val remoteSource = NetworkSource(Dispatchers.IO)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        return DataRepository(parser, fileSource, entries, radios, remoteSource, scope, settings)
    }

    @Provides
    @Singleton
    fun provideLocationSource(
        @ApplicationContext context: Context, settings: ISettingsSource
    ): ILocationSource {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return LocationSource(manager, settings)
    }

    @Provides
    @Singleton
    fun provideOrientationSource(@ApplicationContext context: Context): OrientationSource {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        return OrientationSource(manager, sensor)
    }

    @Provides
    @Singleton
    fun provideSatelliteManager(): ISatelliteManager = SatelliteManager(Dispatchers.Default)

    @Provides
    @Singleton
    fun provideSettingsSource(@ApplicationContext context: Context): ISettingsSource {
        return SettingsSource(context.getSharedPreferences("default", Context.MODE_PRIVATE))
    }
}
