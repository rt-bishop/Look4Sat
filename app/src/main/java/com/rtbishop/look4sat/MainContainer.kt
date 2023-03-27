package com.rtbishop.look4sat

import android.bluetooth.BluetoothManager
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.LocationManager
import androidx.room.Room
import com.rtbishop.look4sat.data.DataParser
import com.rtbishop.look4sat.data.DataRepository
import com.rtbishop.look4sat.data.SatelliteRepository
import com.rtbishop.look4sat.data.SelectionRepository
import com.rtbishop.look4sat.domain.IDataRepository
import com.rtbishop.look4sat.domain.ISatelliteRepository
import com.rtbishop.look4sat.domain.ISensorRepository
import com.rtbishop.look4sat.domain.ISettingsRepository
import com.rtbishop.look4sat.framework.BluetoothReporter
import com.rtbishop.look4sat.framework.NetworkReporter
import com.rtbishop.look4sat.framework.SensorRepository
import com.rtbishop.look4sat.framework.SettingsRepository
import com.rtbishop.look4sat.framework.data.MIGRATION_1_2
import com.rtbishop.look4sat.framework.data.MainDatabase
import com.rtbishop.look4sat.framework.data.local.EntriesStorage
import com.rtbishop.look4sat.framework.data.local.RadiosStorage
import com.rtbishop.look4sat.framework.data.remote.FileSource
import com.rtbishop.look4sat.framework.data.remote.NetworkSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.osmdroid.config.Configuration

class MainContainer(private val context: Context) {

    val bluetoothReporter = provideBluetoothReporter()
    val networkReporter = provideNetworkReporter()
    val settingsRepository = provideSettingsRepository()
    val dataRepository = provideDataRepository(settingsRepository)
    val sensorRepository = provideSensorRepository()
    val satelliteRepository = provideSatelliteRepository()

    private fun provideBluetoothReporter(): BluetoothReporter {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return BluetoothReporter(manager, CoroutineScope(Dispatchers.IO))
    }

    private fun provideNetworkReporter(): NetworkReporter {
        return NetworkReporter(CoroutineScope(Dispatchers.IO))
    }

    private fun provideDataRepository(settings: ISettingsRepository): IDataRepository {
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

    fun provideSelectionRepository(): SelectionRepository {
        return SelectionRepository(Dispatchers.Default, dataRepository, settingsRepository)
    }

    private fun provideSensorRepository(): ISensorRepository {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        return SensorRepository(manager, sensor)
    }

    private fun provideSatelliteRepository(): ISatelliteRepository {
        return SatelliteRepository(Dispatchers.Default)
    }

    private fun provideSettingsRepository(): ISettingsRepository {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val preferences = context.getSharedPreferences("default", Context.MODE_PRIVATE)
        Configuration.getInstance().load(context, preferences)
        return SettingsRepository(manager, preferences)
    }
}
