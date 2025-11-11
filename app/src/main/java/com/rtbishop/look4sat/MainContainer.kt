package com.rtbishop.look4sat

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
import org.osmdroid.config.Configuration

class MainContainer(private val context: Context) {

    private val localSource = provideLocalSource()
    private val mainHandler = CoroutineExceptionHandler { _, error ->
        println("MainHandler: $error")
    }
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + mainHandler)
    val settingsRepo = provideSettingsRepo()
    val selectionRepo = provideSelectionRepo()
    val satelliteRepo = provideSatelliteRepo()
    val databaseRepo = provideDatabaseRepo()

    fun provideAppVersionName(): String {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return packageInfo.versionName ?: "4.0.0"
    }

    fun provideAddToCalendar(): IAddToCalendar {
        return AddToCalendar(context)
    }

    fun provideShowToast(): IShowToast {
        return ShowToast(context)
    }

    fun provideBluetoothReporter(): BluetoothReporter {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return BluetoothReporter(manager, CoroutineScope(Dispatchers.IO))
    }

    fun provideNetworkReporter(): NetworkReporter {
        return NetworkReporter(CoroutineScope(Dispatchers.IO))
    }

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
        val builder = Room.databaseBuilder(context, Look4SatDb::class.java, "Look4SatDBv313")
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
        val mapPrefsFileName = "${context.packageName}_osmdroid"
        val mapPreferences = context.getSharedPreferences(mapPrefsFileName, Context.MODE_PRIVATE)
        Configuration.getInstance().load(context, mapPreferences)
        return SettingsRepo(manager, appPreferences)
    }
}
