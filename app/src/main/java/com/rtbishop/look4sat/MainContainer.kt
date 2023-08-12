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
import com.rtbishop.look4sat.data.repository.DatabaseRepo
import com.rtbishop.look4sat.data.repository.SatelliteRepo
import com.rtbishop.look4sat.data.repository.SelectionRepo
import com.rtbishop.look4sat.data.repository.SettingsRepo
import com.rtbishop.look4sat.data.source.FileSource
import com.rtbishop.look4sat.data.source.LocalSource
import com.rtbishop.look4sat.data.source.RemoteSource
import com.rtbishop.look4sat.data.source.SensorSource
import com.rtbishop.look4sat.domain.repository.IDatabaseRepo
import com.rtbishop.look4sat.domain.repository.ISatelliteRepo
import com.rtbishop.look4sat.domain.repository.ISelectionRepo
import com.rtbishop.look4sat.domain.repository.ISettingsRepo
import com.rtbishop.look4sat.domain.source.IFileSource
import com.rtbishop.look4sat.domain.source.ILocalSource
import com.rtbishop.look4sat.domain.source.IRemoteSource
import com.rtbishop.look4sat.domain.source.ISensorSource
import com.rtbishop.look4sat.domain.utility.DataParser
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.Cache
import okhttp3.OkHttpClient
import org.osmdroid.config.Configuration

class MainContainer(private val context: Context) {

    private val localSource = provideLocalSource()
    val mainScope = provideMainScope()
    val settingsRepo = provideSettingsRepo()
    val selectionRepo = provideSelectionRepo()
    val satelliteRepo = provideSatelliteRepo()
    val databaseRepo = provideDatabaseRepo()

    fun provideBluetoothReporter(): BluetoothReporter {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return BluetoothReporter(manager, CoroutineScope(Dispatchers.IO))
    }

    fun provideNetworkReporter(): NetworkReporter {
        return NetworkReporter(CoroutineScope(Dispatchers.IO))
    }

    fun provideSensorSource(): ISensorSource {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        return SensorSource(manager, sensor)
    }

    private fun provideDatabaseRepo(): IDatabaseRepo {
        val dataParser = DataParser(Dispatchers.Default)
        val fileSource = provideFileSource()
        val remoteSource = provideRemoteSource()
        return DatabaseRepo(Dispatchers.Default, dataParser, fileSource, localSource, remoteSource, settingsRepo)
    }

    private fun provideFileSource(): IFileSource {
        return FileSource(context.contentResolver, Dispatchers.IO)
    }

    private fun provideLocalSource(): ILocalSource {
        val database = Room.databaseBuilder(context, MainDatabase::class.java, "Look4SatDb").apply {
            addMigrations(MIGRATION_1_2)
            fallbackToDestructiveMigration()
        }.build()
        return LocalSource(database.storageDao())
    }

    private fun provideMainScope(): CoroutineScope {
        val mainHandler = CoroutineExceptionHandler { _, error -> println("Look4Sat: $error") }
        return CoroutineScope(SupervisorJob() + Dispatchers.Default + mainHandler)
    }

    private fun provideRemoteSource(): IRemoteSource {
        val cache = Cache(context.cacheDir, MainApplication.MAX_OKHTTP_CACHE_SIZE)
        val httpClient = OkHttpClient.Builder().cache(cache).build()
        return RemoteSource(httpClient, Dispatchers.IO)
    }

    private fun provideSatelliteRepo(): ISatelliteRepo {
        return SatelliteRepo(Dispatchers.Default, localSource, settingsRepo)
    }

    private fun provideSelectionRepo(): ISelectionRepo {
        return SelectionRepo(Dispatchers.Default, localSource, settingsRepo)
    }

    private fun provideSettingsRepo(): ISettingsRepo {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val preferences = context.getSharedPreferences("default", Context.MODE_PRIVATE)
        Configuration.getInstance().load(context, preferences)
        return SettingsRepo(manager, preferences)
    }
}
