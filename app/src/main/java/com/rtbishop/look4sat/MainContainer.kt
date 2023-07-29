package com.rtbishop.look4sat

import android.bluetooth.BluetoothManager
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.LocationManager
import androidx.room.Room
import com.rtbishop.look4sat.data.database.MIGRATION_1_2
import com.rtbishop.look4sat.data.database.MainDatabase
import com.rtbishop.look4sat.data.framework.BluetoothReporter
import com.rtbishop.look4sat.data.framework.NetworkReporter
import com.rtbishop.look4sat.data.framework.SettingsRepo
import com.rtbishop.look4sat.data.repository.DataParser
import com.rtbishop.look4sat.data.repository.DatabaseRepo
import com.rtbishop.look4sat.data.repository.SatelliteRepo
import com.rtbishop.look4sat.data.repository.SelectionRepo
import com.rtbishop.look4sat.data.source.DataSource
import com.rtbishop.look4sat.data.source.LocalStorage
import com.rtbishop.look4sat.data.source.SensorSource
import com.rtbishop.look4sat.domain.repository.IDatabaseRepo
import com.rtbishop.look4sat.domain.repository.ISatelliteRepo
import com.rtbishop.look4sat.domain.repository.ISelectionRepo
import com.rtbishop.look4sat.domain.repository.ISettingsRepo
import com.rtbishop.look4sat.domain.source.ISensorSource
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import org.osmdroid.config.Configuration

class MainContainer(private val context: Context) {

    private val database = Room.databaseBuilder(context, MainDatabase::class.java, "Look4SatDb")
        .addMigrations(MIGRATION_1_2).fallbackToDestructiveMigration().build()
    private val localStorage = LocalStorage(database.storageDao())
    private val mainHandler = CoroutineExceptionHandler { _, error -> println("Look4Sat: $error") }
    val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + mainHandler)
    val settingsRepo = provideSettingsRepo()
    val databaseRepo = provideDatabaseRepo()
    val satelliteRepo = provideSatelliteRepo()
    val selectionRepo = provideSelectionRepo()

    fun provideBluetoothReporter(): BluetoothReporter {
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return BluetoothReporter(btManager, CoroutineScope(Dispatchers.IO))
    }

    fun provideNetworkReporter(): NetworkReporter {
        return NetworkReporter(CoroutineScope(Dispatchers.IO))
    }

    fun provideSensorSource(): ISensorSource {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        return SensorSource(sensorManager, sensor)
    }

    private fun provideDatabaseRepo(): IDatabaseRepo {
        val httpClient = OkHttpClient.Builder().build()
        val dataParser = DataParser(Dispatchers.Default)
        val dataSource = DataSource(context.contentResolver, httpClient, Dispatchers.IO)
        return DatabaseRepo(Dispatchers.Default, dataParser, dataSource, localStorage, settingsRepo)
    }

    private fun provideSatelliteRepo(): ISatelliteRepo {
        return SatelliteRepo(Dispatchers.Default, localStorage, settingsRepo)
    }

    private fun provideSelectionRepo(): ISelectionRepo {
        return SelectionRepo(Dispatchers.Default, localStorage, settingsRepo)
    }

    private fun provideSettingsRepo(): ISettingsRepo {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val preferences = context.getSharedPreferences("default", Context.MODE_PRIVATE)
        Configuration.getInstance().load(context, preferences)
        return SettingsRepo(locationManager, preferences)
    }
}
