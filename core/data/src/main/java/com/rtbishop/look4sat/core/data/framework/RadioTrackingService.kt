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
package com.rtbishop.look4sat.core.data.framework

import android.bluetooth.BluetoothManager
import android.util.Log
import com.rtbishop.look4sat.core.domain.model.SatRadio
import com.rtbishop.look4sat.core.domain.predict.OrbitalPass
import com.rtbishop.look4sat.core.domain.repository.IRadioController
import com.rtbishop.look4sat.core.domain.repository.IRadioTrackingService
import com.rtbishop.look4sat.core.domain.repository.ISatelliteRepo
import com.rtbishop.look4sat.core.domain.repository.ISettingsRepo
import com.rtbishop.look4sat.core.domain.repository.RadioTrackingState
import com.rtbishop.look4sat.core.domain.utility.TransponderMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RadioTrackingService(
    private val appScope: CoroutineScope,
    private val bluetoothManager: BluetoothManager,
    private val satelliteRepo: ISatelliteRepo,
    private val settingsRepo: ISettingsRepo
) : IRadioTrackingService {

    private val tag = "RadioTracking"
    private val _state = MutableStateFlow(RadioTrackingState())
    override val state: StateFlow<RadioTrackingState> = _state

    private var txController: IRadioController? = null
    private var rxController: IRadioController? = null
    private var trackingJob: Job? = null

    override suspend fun connectRadios() {
        // Disconnect old controllers if any
        txController?.disconnect()
        rxController?.disconnect()

        // Read current addresses from settings
        val rcSettings = settingsRepo.radioControlSettings.value
        val txAddr = rcSettings.txRadioAddress
        val rxAddr = rcSettings.rxRadioAddress

        Log.i(tag, "Connecting TX=$txAddr RX=$rxAddr")

        if (txAddr.isBlank() && rxAddr.isBlank()) {
            _state.update { it.copy(errorMessage = "No radio addresses configured. Set them in Settings → FT-817.") }
            return
        }

        // Create fresh controllers with current addresses
        val tx = Ft817Controller(bluetoothManager, txAddr)
        val rx = Ft817Controller(bluetoothManager, rxAddr)
        txController = tx
        rxController = rx

        _state.update { it.copy(errorMessage = null) }
        val txOk = if (txAddr.isNotBlank()) tx.connect() else false
        val rxOk = if (rxAddr.isNotBlank()) rx.connect() else false
        _state.update {
            it.copy(
                txConnected = txOk,
                rxConnected = rxOk,
                errorMessage = when {
                    !txOk && !rxOk -> "Could not connect to TX and RX radios"
                    !txOk -> "Could not connect to TX radio ($txAddr)"
                    !rxOk -> "Could not connect to RX radio ($rxAddr)"
                    else -> null
                }
            )
        }
    }

    override suspend fun disconnectRadios() {
        stopTracking()
        txController?.disconnect()
        rxController?.disconnect()
        txController = null
        rxController = null
        _state.update {
            it.copy(
                txConnected = false,
                rxConnected = false,
                isActive = false
            )
        }
    }

    override fun startTracking(pass: OrbitalPass, transponder: SatRadio, txBaseFreqHz: Long?) {
        _state.update {
            it.copy(
                isActive = true,
                currentPass = pass,
                selectedTransponder = transponder,
                txBaseFrequencyHz = txBaseFreqHz
            )
        }
        trackingJob?.cancel()
        trackingJob = appScope.launch {
            // Set modes on both radios at tracking start
            val tx = txController
            val rx = rxController
            val txMode = transponder.uplinkMode
            val rxMode = transponder.downlinkMode
                ?: transponder.uplinkMode?.let {
                    TransponderMapper.mapUplinkModeToDownlinkMode(it, transponder.isInverted)
                }
            if (tx != null && tx.isConnected && txMode != null) {
                tx.setMode(txMode)
                Log.i(tag, "TX mode set to $txMode")
            }
            if (rx != null && rx.isConnected && rxMode != null) {
                rx.setMode(rxMode)
                Log.i(tag, "RX mode set to $rxMode")
            }
            // Set CTCSS if FM
            if (txMode?.uppercase() == "FM") {
                _state.value.ctcssTone?.let { tone ->
                    tx?.setCtcssTone(tone)
                    tx?.setCtcssMode(true)
                }
            }
            _state.update { it.copy(txMode = txMode, rxMode = rxMode) }

            // Gpredict-style: 0.0 = sync invalidated, skip dial feedback next cycle
            var lastSetTxFreq = 0.0
            var lastSetRxFreq = 0.0

            while (isActive) {
                val currentState = _state.value
                if (!currentState.isActive) break

                val satPass = currentState.currentPass ?: break
                val xpdr = currentState.selectedTransponder ?: break
                var txBaseFreq = currentState.txBaseFrequencyHz
                val stationPos = settingsRepo.stationPosition.value
                val timeNow = System.currentTimeMillis()

                val pos = satelliteRepo.getPosition(satPass.orbitalObject, stationPos, timeNow)
                val tx = txController
                val rx = rxController
                val isLinearMode = currentState.txMode?.uppercase() in listOf("USB", "LSB", "CW", "CW-R")
                val c = com.rtbishop.look4sat.core.domain.predict.SPEED_OF_LIGHT
                val v = pos.distanceRate * 1000.0
                var skipTxWrite = false
                var skipRxWrite = false

                // --- TX dial feedback ---
                if (isLinearMode && txBaseFreq != null && tx != null && tx.isConnected && lastSetTxFreq > 0.0) {
                    val readResult = tx.readFrequencyAndMode()
                    if (readResult != null) {
                        val (actualTxFreq, _) = readResult
                        if (kotlin.math.abs(actualTxFreq - lastSetTxFreq) >= 10.0) {
                            val newBase = (actualTxFreq.toDouble() * c / (c + v)).toLong()
                            if (newBase > 0) {
                                txBaseFreq = newBase
                                _state.update { it.copy(txBaseFrequencyHz = newBase) }
                                Log.i(tag, "TX dial → base=$newBase (read=$actualTxFreq, lastSet=$lastSetTxFreq)")
                            }
                            lastSetTxFreq = 0.0
                            skipTxWrite = true
                        }
                    }
                }

                // --- RX dial feedback ---
                if (isLinearMode && !skipTxWrite && rx != null && rx.isConnected && lastSetRxFreq > 0.0) {
                    val readResult = rx.readFrequencyAndMode()
                    if (readResult != null) {
                        val (actualRxFreq, _) = readResult
                        if (kotlin.math.abs(actualRxFreq - lastSetRxFreq) >= 10.0) {
                            val rxNominal = (actualRxFreq.toDouble() * c / (c - v)).toLong()
                            val newTxBase = TransponderMapper.mapDownlinkToUplink(rxNominal, xpdr)
                            if (newTxBase != null && newTxBase > 0) {
                                txBaseFreq = newTxBase
                                _state.update { it.copy(txBaseFrequencyHz = newTxBase) }
                                Log.i(tag, "RX dial → txBase=$newTxBase")
                            }
                            lastSetRxFreq = 0.0
                            skipRxWrite = true
                        }
                    }
                }

                // Compute Doppler-corrected frequencies
                val txRadioFreq = txBaseFreq?.let { pos.getUplinkFreq(it) }
                val rxBaseFreq = if (txBaseFreq != null) {
                    TransponderMapper.mapUplinkToDownlink(txBaseFreq, xpdr)
                } else {
                    xpdr.downlinkLow
                }
                val rxRadioFreq = rxBaseFreq?.let { pos.getDownlinkFreq(it) }

                // Command radios (skip one cycle after dial change)
                if (!skipTxWrite && tx != null && tx.isConnected && txRadioFreq != null) {
                    tx.setFrequency(txRadioFreq)
                    lastSetTxFreq = txRadioFreq.toDouble()
                }
                if (!skipRxWrite && rx != null && rx.isConnected && rxRadioFreq != null) {
                    rx.setFrequency(rxRadioFreq)
                    lastSetRxFreq = rxRadioFreq.toDouble()
                }

                _state.update {
                    it.copy(
                        txConnected = tx?.isConnected ?: false,
                        rxConnected = rx?.isConnected ?: false,
                        txFrequencyHz = txRadioFreq,
                        rxFrequencyHz = rxRadioFreq,
                        azimuth = Math.toDegrees(pos.azimuth),
                        elevation = Math.toDegrees(pos.elevation),
                        distance = pos.distance
                    )
                }

                delay(500)
            }
        }
    }

    override fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
        _state.update { it.copy(isActive = false) }
    }

    override fun setTransponder(transponder: SatRadio) {
        appScope.launch {
            val tx = txController
            val rx = rxController
            transponder.uplinkMode?.let { tx?.setMode(it) }
            val rxMode = transponder.downlinkMode
                ?: transponder.uplinkMode?.let {
                    TransponderMapper.mapUplinkModeToDownlinkMode(it, transponder.isInverted)
                }
            rxMode?.let { rx?.setMode(it) }

            if (transponder.uplinkMode?.uppercase() == "FM") {
                _state.value.ctcssTone?.let { tone ->
                    tx?.setCtcssTone(tone)
                    tx?.setCtcssMode(true)
                }
            }
        }
        val txCenter = when {
            transponder.uplinkLow != null && transponder.uplinkHigh != null ->
                (transponder.uplinkLow!! + transponder.uplinkHigh!!) / 2
            transponder.uplinkLow != null -> transponder.uplinkLow!!
            else -> null
        }
        // Show nominal frequencies immediately
        val rxNominal = if (txCenter != null) {
            TransponderMapper.mapUplinkToDownlink(txCenter, transponder)
        } else {
            // Downlink-only transponder (beacon etc.) - use downlink directly
            transponder.downlinkLow
        }
        _state.update {
            it.copy(
                selectedTransponder = transponder,
                txBaseFrequencyHz = txCenter,
                txFrequencyHz = txCenter,
                rxFrequencyHz = rxNominal,
                txMode = transponder.uplinkMode,
                rxMode = transponder.downlinkMode
                    ?: transponder.uplinkMode?.let { m ->
                        TransponderMapper.mapUplinkModeToDownlinkMode(m, transponder.isInverted)
                    }
            )
        }
    }

    override fun setTxBaseFrequency(frequencyHz: Long) {
        _state.update { it.copy(txBaseFrequencyHz = frequencyHz) }
    }

    override fun adjustTxBaseFrequency(deltaHz: Long) {
        val current = _state.value.txBaseFrequencyHz ?: return
        _state.update { it.copy(txBaseFrequencyHz = current + deltaHz) }
    }

    override fun setCtcssTone(toneHz: Double?) {
        _state.update { it.copy(ctcssTone = toneHz) }
        appScope.launch {
            val tx = txController
            if (toneHz != null) {
                tx?.setCtcssTone(toneHz)
                tx?.setCtcssMode(true)
            } else {
                tx?.setCtcssMode(false)
            }
        }
    }

    override fun setMode(txMode: String, rxMode: String) {
        appScope.launch {
            txController?.setMode(txMode)
            rxController?.setMode(rxMode)
        }
        _state.update { it.copy(txMode = txMode, rxMode = rxMode) }
    }
}
