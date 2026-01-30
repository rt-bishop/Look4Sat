/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2026 Arty Bishop and contributors.
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
package com.rtbishop.look4sat.data.injection

import android.bluetooth.BluetoothManager
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.LocationManager
import androidx.room.Room
import com.rtbishop.look4sat.data.database.Look4SatDb
import com.rtbishop.look4sat.data.framework.BluetoothReporter
import com.rtbishop.look4sat.data.framework.NetworkReporter
import com.rtbishop.look4sat.data.repository.DatabaseRepo
import com.rtbishop.look4sat.data.repository.SatelliteRepo
import com.rtbishop.look4sat.data.repository.SelectionRepo
import com.rtbishop.look4sat.data.repository.SensorsRepo
import com.rtbishop.look4sat.data.repository.SettingsRepo
import com.rtbishop.look4sat.data.source.LocalSource
import com.rtbishop.look4sat.data.source.RemoteSource
import com.rtbishop.look4sat.data.usecase.AddToCalendar
import com.rtbishop.look4sat.data.usecase.ShowToast
import com.rtbishop.look4sat.domain.repository.IDatabaseRepo
import com.rtbishop.look4sat.domain.repository.ISatelliteRepo
import com.rtbishop.look4sat.domain.repository.ISelectionRepo
import com.rtbishop.look4sat.domain.repository.ISensorsRepo
import com.rtbishop.look4sat.domain.repository.ISettingsRepo
import com.rtbishop.look4sat.domain.source.ILocalSource
import com.rtbishop.look4sat.domain.source.IRemoteSource
import com.rtbishop.look4sat.domain.usecase.IAddToCalendar
import com.rtbishop.look4sat.domain.usecase.IShowToast
import com.rtbishop.look4sat.domain.utility.DataParser
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient

class MainContainer(private val context: Context) {

    private val localSource = provideLocalSource()
    private val mainHandler = CoroutineExceptionHandler { _, error -> println("MainHandler: $error") }
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + mainHandler)
    val settingsRepo = provideSettingsRepo()
    val selectionRepo = provideSelectionRepo()
    val satelliteRepo = provideSatelliteRepo()
    val databaseRepo = provideDatabaseRepo()

    fun provideAppVersionName(): String {
        return context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "4.0.0"
    }

    fun provideAddToCalendar(): IAddToCalendar = AddToCalendar(context)

    fun provideShowToast(): IShowToast = ShowToast(context)

    fun provideBluetoothReporter(): BluetoothReporter {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return BluetoothReporter(manager, CoroutineScope(Dispatchers.IO))
    }

    fun provideNetworkReporter(): NetworkReporter = NetworkReporter(CoroutineScope(Dispatchers.IO))

    fun provideSensorsRepo(): ISensorsRepo {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        return SensorsRepo(manager, sensor)
    }

    private fun provideDatabaseRepo(): IDatabaseRepo {
        val dbDispatcher = Dispatchers.Default
        val dataParser = DataParser(dbDispatcher)
        val remoteSource = provideRemoteSource()
        return DatabaseRepo(dbDispatcher, dataParser, localSource, remoteSource, settingsRepo)
    }

    private fun provideLocalSource(): ILocalSource {
        val builder = Room.databaseBuilder(context, Look4SatDb::class.java, "Look4SatDBv400")
        val database = builder.apply { fallbackToDestructiveMigration(false) }.build()
        return LocalSource(database.look4SatDao())
    }

    private fun provideRemoteSource(): IRemoteSource {
        return RemoteSource(Dispatchers.IO, context.contentResolver, OkHttpClient.Builder().build())
    }

    private fun provideSatelliteRepo(): ISatelliteRepo {
        return SatelliteRepo(Dispatchers.Default, localSource, settingsRepo)
    }

    private fun provideSelectionRepo(): ISelectionRepo {
        return SelectionRepo(Dispatchers.Default, localSource, settingsRepo)
    }

    private fun provideSettingsRepo(): ISettingsRepo {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val appPrefsFileName = "${context.packageName}_preferences"
        val appPreferences = context.getSharedPreferences(appPrefsFileName, Context.MODE_PRIVATE)
        return SettingsRepo(manager, appPreferences)
    }
}
