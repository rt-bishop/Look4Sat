package com.rtbishop.look4sat

import android.bluetooth.BluetoothManager
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.LocationManager
import androidx.room.Room
import com.rtbishop.look4sat.data.DataParser
import com.rtbishop.look4sat.data.DatabaseRepo
import com.rtbishop.look4sat.data.ISensorSource
import com.rtbishop.look4sat.data.SatelliteRepo
import com.rtbishop.look4sat.data.SelectionRepo
import com.rtbishop.look4sat.domain.IDatabaseRepo
import com.rtbishop.look4sat.domain.ISatelliteRepo
import com.rtbishop.look4sat.domain.ISelectionRepo
import com.rtbishop.look4sat.domain.ISettingsRepo
import com.rtbishop.look4sat.framework.BluetoothReporter
import com.rtbishop.look4sat.framework.NetworkReporter
import com.rtbishop.look4sat.framework.SettingsRepo
import com.rtbishop.look4sat.framework.data.DataSource
import com.rtbishop.look4sat.framework.data.LocalStorage
import com.rtbishop.look4sat.framework.data.MIGRATION_1_2
import com.rtbishop.look4sat.framework.data.MainDatabase
import com.rtbishop.look4sat.framework.data.SensorSource
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
        val dataParser = DataParser(Dispatchers.Default)
        val dataSource = DataSource(context.contentResolver, Dispatchers.IO)
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
