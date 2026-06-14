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
package com.rtbishop.look4sat.feature.radar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rtbishop.look4sat.core.domain.model.SatRadio
import com.rtbishop.look4sat.core.domain.predict.CelestialComputer
import com.rtbishop.look4sat.core.domain.predict.OrbitalObject
import com.rtbishop.look4sat.core.domain.predict.OrbitalPass
import com.rtbishop.look4sat.core.domain.predict.OrbitalPos
import com.rtbishop.look4sat.core.domain.repository.IMainContainer
import com.rtbishop.look4sat.core.domain.repository.IRadioTrackingService
import com.rtbishop.look4sat.core.domain.repository.IReporter
import com.rtbishop.look4sat.core.domain.repository.ISatelliteRepo
import com.rtbishop.look4sat.core.domain.repository.ISensorsRepo
import com.rtbishop.look4sat.core.domain.repository.ISettingsRepo
import com.rtbishop.look4sat.core.domain.sstv.SstvDecoder
import com.rtbishop.look4sat.core.domain.usecase.IAudioCapture
import com.rtbishop.look4sat.core.domain.usecase.IAddToCalendar
import com.rtbishop.look4sat.core.domain.usecase.ISaveImage
import com.rtbishop.look4sat.core.domain.usecase.IShowToast
import com.rtbishop.look4sat.core.domain.utility.round
import com.rtbishop.look4sat.core.domain.utility.toDegrees
import com.rtbishop.look4sat.core.domain.utility.toTimerString
import com.rtbishop.look4sat.core.presentation.formatFrequency
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class RadarViewModel(
    private val bluetoothReporter: IReporter,
    private val networkReporter: IReporter,
    private val satelliteRepo: ISatelliteRepo,
    private val settingsRepo: ISettingsRepo,
    private val sensorsRepo: ISensorsRepo,
    private val addToCalendar: IAddToCalendar,
    private val trackingService: IRadioTrackingService,
    private val audioCapture: IAudioCapture,
    private val saveImage: ISaveImage,
    private val showToast: IShowToast
) : ViewModel() {

    private val stationPos = settingsRepo.stationPosition.value
    private val magDeclination = sensorsRepo.getMagDeclination(stationPos)
    private var transponders: List<SatRadio> = emptyList()
    private var sstvDecoder: SstvDecoder? = null
    private var sstvRecordingJob: Job? = null

    // Celestial positions change slowly, recompute at most once per minute
    private var lastCelestialUpdateMs = 0L
    private var cachedSunPos: CelestialComputer.SunPosition? = null
    private var cachedMoonPos: CelestialComputer.MoonPosition? = null

    private val _uiState = MutableStateFlow(
        RadarState(
            isUtc = settingsRepo.otherSettings.value.stateOfUtc,
            orientationValues = sensorsRepo.sensorData.value,
            shouldShowSweep = settingsRepo.otherSettings.value.stateOfSweep,
            shouldUseCompass = settingsRepo.otherSettings.value.stateOfSensors,
            sstv = SstvSubState(selectedMode = settingsRepo.otherSettings.value.sstvMode)
        )
    )
    val uiState: StateFlow<RadarState> = _uiState

    init {
        collectCompassSensor()
        collectSettingsChanges()
        collectPassAndStartTickLoop()
        collectRadioTrackingState()
    }

    private fun collectCompassSensor() {
        if (!settingsRepo.otherSettings.value.stateOfSensors) return
        viewModelScope.launch {
            sensorsRepo.enableSensor()
            sensorsRepo.sensorData.collect { data ->
                val orientationValues = (data.first + magDeclination) to data.second
                _uiState.update { it.copy(orientationValues = orientationValues) }
            }
        }
    }

    private fun collectSettingsChanges() {
        viewModelScope.launch {
            settingsRepo.otherSettings.collectLatest { settings ->
                _uiState.update {
                    it.copy(
                        isUtc = settings.stateOfUtc,
                        shouldShowSweep = settings.stateOfSweep,
                        shouldUseCompass = settings.stateOfSensors
                    )
                }
            }
        }
    }

    // --- Pass loading split into focused functions ---

    private fun collectPassAndStartTickLoop() {
        viewModelScope.launch {
            val pass = findCurrentPass() ?: return@launch
            val allRadios = loadPassData(pass)
            while (isActive) {
                tickPass(pass, allRadios)
                delay(1000.milliseconds)
            }
        }
    }

    private fun findCurrentPass(): OrbitalPass? {
        val passes = satelliteRepo.passes.value
        val (catNum, aosTime) = satelliteRepo.selectedPass.value
        return passes.find { it.catNum == catNum && it.aosTime == aosTime }
            ?: passes.firstOrNull()
    }

    // Loads transmitters and satellite track for pass, sets initial state, returns full radio list
    private suspend fun loadPassData(pass: OrbitalPass): List<SatRadio> {
        _uiState.update { it.copy(currentPass = pass) }
        val allRadios = satelliteRepo.getRadiosWithId(pass.catNum)
        transponders = allRadios.filter { it.downlinkLow != null }
        if (allRadios.isNotEmpty()) {
            val firstUuid = allRadios.first().uuid
            _uiState.update { it.copy(transceivers = it.transceivers.copy(selectedUuid = firstUuid)) }
            transponders.find { it.uuid == firstUuid }?.let { trackingService.setTransponder(it) }
        }
        if (!pass.isDeepSpace) {
            val track = satelliteRepo.getTrack(pass.orbitalObject, stationPos, pass.aosTime, pass.losTime)
            _uiState.update { it.copy(satTrack = track) }
        }
        return allRadios
    }

    // --- Per-second tick ---

    private suspend fun tickPass(pass: OrbitalPass, allRadios: List<SatRadio>) {
        val timeNow = System.currentTimeMillis()
        val pos = satelliteRepo.getPosition(pass.orbitalObject, stationPos, timeNow)

        // Recompute celestial positions at most once per minute (they move very slowly)
        if (timeNow - lastCelestialUpdateMs >= 60_000L) {
            cachedSunPos = CelestialComputer.getSunPosition(stationPos, timeNow)
            cachedMoonPos = CelestialComputer.getMoonPosition(stationPos, timeNow)
            lastCelestialUpdateMs = timeNow
        }

        val (time, isAos) = computeTimer(pass.isDeepSpace, pass.aosTime, pass.losTime, timeNow)
        _uiState.update {
            it.copy(
                currentTime = time, isTimeAos = isAos,
                orbitalPos = pos, sunPosition = cachedSunPos, moonPosition = cachedMoonPos
            )
        }
        processRadios(allRadios, pass.orbitalObject, timeNow)
        sendPassData(pos)
    }

    private fun collectRadioTrackingState() {
        viewModelScope.launch {
            trackingService.state.collect { svc ->
                _uiState.update { state ->
                    state.copy(
                        radioControl = state.radioControl.copy(
                            txPanel = RadioPanelState(
                                label = "TX (Uplink)",
                                isConnected = svc.txConnected,
                                frequencyHz = svc.txFrequencyHz,
                                frequencyDisplay = svc.txFrequencyHz?.let { formatFrequency(it) } ?: "---",
                                mode = svc.txMode
                            ),
                            rxPanel = RadioPanelState(
                                label = "RX (Downlink)",
                                isConnected = svc.rxConnected,
                                frequencyHz = svc.rxFrequencyHz,
                                frequencyDisplay = svc.rxFrequencyHz?.let { formatFrequency(it) } ?: "---",
                                mode = svc.rxMode
                            ),
                            txBaseFrequencyHz = svc.txBaseFrequencyHz,
                            ctcssTone = svc.ctcssTone,
                            isTracking = svc.isActive,
                            selectedTransponderUuid = svc.selectedTransponder?.uuid,
                            errorMessage = svc.errorMessage
                        )
                    )
                }
            }
        }
    }

    override fun onCleared() {
        sensorsRepo.disableSensor()
        super.onCleared()
    }

    fun onAction(action: RadarAction) {
        when (action) {
            is RadarAction.AddToCalendar -> addToCalendar(action.name, action.aosTime, action.losTime)
            is RadarAction.SelectTransmitter -> {
                // Compute toggle state before the update so we don't read post-update value
                val isTogglingOff = _uiState.value.transceivers.selectedUuid == action.uuid
                val newUuid = if (isTogglingOff) null else action.uuid
                _uiState.update { it.copy(transceivers = it.transceivers.copy(selectedUuid = newUuid)) }
                // Only update the tracking service when selecting a different transponder to
                // avoid resetting a user-adjusted TX base on re-expand
                if (!isTogglingOff) {
                    transponders.find { it.uuid == action.uuid }?.let { trackingService.setTransponder(it) }
                }
            }
            is RadarAction.SetTxFrequency -> trackingService.setTxBaseFrequency(action.frequencyHz)
            is RadarAction.AdjustTxFrequency -> trackingService.adjustTxBaseFrequency(action.deltaHz)
            is RadarAction.SetCtcssTone -> trackingService.setCtcssTone(action.toneHz)
            RadarAction.ToggleTracking -> {
                val svc = trackingService.state.value
                if (svc.isActive) {
                    trackingService.stopTracking()
                } else {
                    val pass = _uiState.value.currentPass ?: return
                    val transponder = svc.selectedTransponder ?: return
                    trackingService.startTracking(pass, transponder, svc.txBaseFrequencyHz)
                }
            }
            RadarAction.ConnectRadios -> viewModelScope.launch { trackingService.connectRadios() }
            RadarAction.DisconnectRadios -> viewModelScope.launch { trackingService.disconnectRadios() }

            // SSTV actions
            is RadarAction.SstvPermissionResult -> {
                _uiState.update { it.copy(sstv = it.sstv.copy(hasPermission = action.granted)) }
                if (action.granted) initSstvDecoder()
            }
            RadarAction.SstvStartRecording -> startSstvRecording()
            RadarAction.SstvStopRecording -> stopSstvRecording()
            RadarAction.SstvSaveImage -> {
                val frame = _uiState.value.sstv.currentFrame ?: return
                val pixels = frame.imagePixels ?: return
                _uiState.update { it.copy(sstv = it.sstv.copy(isSaving = true)) }
                viewModelScope.launch {
                    saveImage(pixels, frame.imageWidth, frame.imageHeight, frame.modeName)
                    _uiState.update { it.copy(sstv = it.sstv.copy(isSaving = false)) }
                    showToast("Image saved")
                }
            }
            is RadarAction.SstvSelectMode -> {
                sstvDecoder?.lockMode(action.modeName)
                _uiState.update { it.copy(sstv = it.sstv.copy(selectedMode = action.modeName)) }
                settingsRepo.updateOtherSettings { it.copy(sstvMode = action.modeName) }
            }
            RadarAction.SstvReset -> {
                sstvDecoder?.clearPixels()
                _uiState.update { it.copy(sstv = it.sstv.copy(currentFrame = null)) }
            }
        }
    }

    private fun computeTimer(isDeepSpace: Boolean, aosTime: Long, losTime: Long, timeNow: Long): Pair<String, Boolean> {
        return when {
            isDeepSpace -> 0L.toTimerString() to false
            aosTime > timeNow -> (aosTime - timeNow).toTimerString() to true
            else -> (losTime - timeNow).toTimerString() to false
        }
    }

    private fun sendPassData(orbitalPos: OrbitalPos) {
        val rc = settingsRepo.rcSettings.value
        sendReporterData(
            networkReporter, orbitalPos,
            rc.rotatorState, rc.rotatorFormat,
            rc.frequencyState, rc.frequencyFormat
        )
        sendReporterData(
            bluetoothReporter, orbitalPos,
            rc.bluetoothRotatorState, rc.bluetoothRotatorFormat,
            rc.bluetoothFrequencyState, rc.bluetoothFrequencyFormat
        )
    }

    private fun sendReporterData(
        reporter: IReporter,
        orbitalPos: OrbitalPos,
        rotatorEnabled: Boolean,
        rotatorFormat: String,
        frequencyEnabled: Boolean,
        frequencyFormat: String
    ) {
        // Only send rotator commands when the satellite is above the horizon
        if (rotatorEnabled && orbitalPos.aboveHorizon) {
            val azimuth = orbitalPos.azimuth.toDegrees().round(2)
            val elevation = orbitalPos.elevation.toDegrees().round(2)
            reporter.reportRotation(rotatorFormat, azimuth, elevation)
        }
        if (frequencyEnabled) {
            _uiState.value.transceivers.selectedFrequency?.let { freq ->
                reporter.reportFrequency(frequencyFormat, freq)
            }
        }
    }

    private suspend fun processRadios(radios: List<SatRadio>, orbitalObject: OrbitalObject, time: Long) {
        val transmitters = satelliteRepo.getRadios(orbitalObject, stationPos, radios, time)
        _uiState.update { state ->
            val freq = if (state.transceivers.selectedUuid != null) {
                val selectedRadio = transmitters.firstOrNull { it.uuid == state.transceivers.selectedUuid }
                selectedRadio?.let { radio ->
                    val low = radio.downlinkLow
                    val high = radio.downlinkHigh
                    when {
                        low != null && high != null -> (low + high) / 2
                        low != null -> low
                        else -> null
                    }
                }
            } else null

            val current = state.transceivers
            if (current.transmitters == transmitters && current.selectedFrequency == freq) {
                return@update state
            }
            state.copy(transceivers = current.copy(transmitters = transmitters, selectedFrequency = freq))
        }
    }

    private fun initSstvDecoder() {
        if (sstvDecoder == null) {
            val decoder = SstvDecoder(sampleRate = audioCapture.sampleRate)
            sstvDecoder = decoder
            decoder.lockMode(_uiState.value.sstv.selectedMode)
            _uiState.update { it.copy(sstv = it.sstv.copy(supportedModes = decoder.supportedModes)) }
            viewModelScope.launch {
                decoder.frames.collect { frame ->
                    _uiState.update { it.copy(sstv = it.sstv.copy(currentFrame = frame)) }
                }
            }
        }
    }

    private fun startSstvRecording() {
        if (sstvRecordingJob?.isActive == true) return
        initSstvDecoder()
        _uiState.update { it.copy(sstv = it.sstv.copy(status = SstvStatus.Recording)) }
        sstvRecordingJob = viewModelScope.launch {
            audioCapture.audioFlow().collect { buffer ->
                sstvDecoder?.feedSamples(buffer)
            }
        }
    }

    private fun stopSstvRecording() {
        sstvRecordingJob?.cancel()
        sstvRecordingJob = null
        _uiState.update { it.copy(sstv = it.sstv.copy(status = SstvStatus.Idle)) }
    }

    companion object {

        val CTCSS_TONES = listOf(
            67.0, 69.3, 71.9, 74.4, 77.0, 79.7, 82.5, 85.4, 88.5, 91.5,
            94.8, 97.4, 100.0, 103.5, 107.2, 110.9, 114.8, 118.8, 123.0, 127.3, 131.8, 136.5,
            141.3, 146.2, 151.4, 156.7, 162.2, 167.9, 173.8, 179.9, 186.2, 192.8, 203.5, 210.7,
            218.1, 225.7, 233.6, 241.8, 250.3
        )


        fun factory(container: IMainContainer) = viewModelFactory {
            initializer {
                RadarViewModel(
                    bluetoothReporter = container.provideBluetoothReporter(),
                    networkReporter = container.provideNetworkReporter(),
                    satelliteRepo = container.satelliteRepo,
                    settingsRepo = container.settingsRepo,
                    sensorsRepo = container.provideSensorsRepo(),
                    addToCalendar = container.provideAddToCalendar(),
                    trackingService = container.radioTrackingService,
                    audioCapture = container.provideAudioCapture(),
                    saveImage = container.provideSaveImage(),
                    showToast = container.provideShowToast()
                )
            }
        }
    }
}
