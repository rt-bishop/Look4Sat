package com.rtbishop.look4sat.core.data.injection

import android.bluetooth.BluetoothManager
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.LocationManager
import android.view.WindowManager
import androidx.room.Room
import com.rtbishop.look4sat.core.data.database.Look4SatDb
import com.rtbishop.look4sat.core.data.framework.BluetoothReporter
import com.rtbishop.look4sat.core.data.framework.Ft817Controller
import com.rtbishop.look4sat.core.data.framework.NetworkReporter
import com.rtbishop.look4sat.core.data.framework.RadioTrackingService
import com.rtbishop.look4sat.core.data.repository.DatabaseRepo
import com.rtbishop.look4sat.core.data.repository.SatelliteRepo
import com.rtbishop.look4sat.core.data.repository.SelectionRepo
import com.rtbishop.look4sat.core.data.repository.SensorsRepo
import com.rtbishop.look4sat.core.data.repository.SettingsRepo
import com.rtbishop.look4sat.core.data.source.LocalSource
import com.rtbishop.look4sat.core.data.source.RemoteSource
import com.rtbishop.look4sat.core.data.usecase.AddToCalendar
import com.rtbishop.look4sat.core.data.usecase.ShowToast
import com.rtbishop.look4sat.core.domain.repository.IDatabaseRepo
import com.rtbishop.look4sat.core.domain.repository.IMainContainer
import com.rtbishop.look4sat.core.domain.repository.IRadioController
import com.rtbishop.look4sat.core.domain.repository.IRadioTrackingService
import com.rtbishop.look4sat.core.domain.repository.IReporter
import com.rtbishop.look4sat.core.domain.repository.ISatelliteRepo
import com.rtbishop.look4sat.core.domain.repository.ISelectionRepo
import com.rtbishop.look4sat.core.domain.repository.ISensorsRepo
import com.rtbishop.look4sat.core.domain.repository.ISettingsRepo
import com.rtbishop.look4sat.core.domain.source.ILocalSource
import com.rtbishop.look4sat.core.domain.source.IRemoteSource
import com.rtbishop.look4sat.core.domain.usecase.IAddToCalendar
import com.rtbishop.look4sat.core.domain.usecase.IShowToast
import com.rtbishop.look4sat.core.domain.utility.DataParser
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient

class MainContainer(private val context: Context) : IMainContainer {

    private val localSource = provideLocalSource()
    private val mainHandler = CoroutineExceptionHandler { _, error -> println("MainHandler: $error") }
    override val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + mainHandler)
    override val settingsRepo = provideSettingsRepo()
    override val selectionRepo = provideSelectionRepo()
    override val satelliteRepo = provideSatelliteRepo()
    override val databaseRepo = provideDatabaseRepo()
    override val radioTrackingService: IRadioTrackingService by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        RadioTrackingService(appScope, manager, satelliteRepo, settingsRepo)
    }

    override fun provideAddToCalendar(): IAddToCalendar = AddToCalendar(context)

    override fun provideShowToast(): IShowToast = ShowToast(context)

    override fun provideBluetoothReporter(): IReporter {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val rc = settingsRepo.rcSettings.value
        return BluetoothReporter(
            manager,
            CoroutineScope(Dispatchers.IO),
            rc.bluetoothRotatorAddress,
            rc.bluetoothFrequencyAddress
        )
    }

    override fun provideNetworkReporter(): IReporter {
        val rc = settingsRepo.rcSettings.value
        return NetworkReporter(
            CoroutineScope(Dispatchers.IO),
            rc.rotatorAddress,
            rc.rotatorPort.toIntOrNull() ?: 0,
            rc.frequencyAddress,
            rc.frequencyPort.toIntOrNull() ?: 0
        )
    }

    override fun provideTxRadioController(): IRadioController {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val address = settingsRepo.radioControlSettings.value.txRadioAddress
        return Ft817Controller(manager, address)
    }

    override fun provideRxRadioController(): IRadioController {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val address = settingsRepo.radioControlSettings.value.rxRadioAddress
        return Ft817Controller(manager, address)
    }

    override fun provideSensorsRepo(): ISensorsRepo {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val window = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return SensorsRepo(manager,sensor,window)
    }

    private fun provideDatabaseRepo(): IDatabaseRepo {
        val dbDispatcher = Dispatchers.Default
        val dataParser = DataParser(dbDispatcher)
        val remoteSource = provideRemoteSource()
        return DatabaseRepo(dbDispatcher, dataParser, localSource, remoteSource, settingsRepo)
    }

    private fun provideLocalSource(): ILocalSource {
        val builder = Room.databaseBuilder(context, Look4SatDb::class.java, "Look4SatDBv400")
        val database = builder.fallbackToDestructiveMigration(false).build()
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
        val appVersionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "4.0.4"
        return SettingsRepo(manager, appPreferences, appVersionName)
    }
}
