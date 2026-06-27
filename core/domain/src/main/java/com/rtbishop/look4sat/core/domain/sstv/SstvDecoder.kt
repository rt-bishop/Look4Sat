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
package com.rtbishop.look4sat.core.domain.sstv

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.sqrt

class SstvFrame(
    val scopePixels: IntArray?,
    val scopeWidth: Int,
    val scopeHeight: Int,
    val imagePixels: IntArray?,
    val imageWidth: Int,
    val imageHeight: Int,
    val modeName: String,
    val imageComplete: Boolean,
    val inputRms: Float,
    val appliedGain: Float,
    val syncHitRate: Float,
    val predictedLineBursts: Int,
    val maxPredictedStreak: Int,
    val timingErrorSamples: Int
)

/**
 * Decoder quality metrics for diagnostics and logging. Useful for profiling decode
 * performance on noisy recordings.
 */
data class SstvQualityMetrics(
    val syncHitRate: Float,
    val predictedLineBursts: Int,
    val maxPredictedStreak: Int,
    val timingErrorSamples: Int
)

enum class LineRecoveryStrategy {
    Look4SatLimited,
    Robot36Compatible
}

class SstvDiagnosticsHandle internal constructor(
    val enabled: Boolean,
    val metrics: StateFlow<SstvQualityMetrics?>
)

class SstvDecoder(
    sampleRate: Int = 44100,
    scopeWidth: Int = 320,
    scopeHeight: Int = 256,
    private val channelSelect: Int = 0,
    targetRmsLevel: Float = 0.25f,
    private val includeScopeData: Boolean = false,
    private val enableRmsNormalization: Boolean = true,
    preFilterCutoffHz: Double = 500.0,
    enablePreFilter: Boolean = true,
    private val enableDiagnosticsHandle: Boolean = false,
    lineRecoveryStrategy: LineRecoveryStrategy = LineRecoveryStrategy.Look4SatLimited
) {
    private val scopeBuffer = PixelBuffer(scopeWidth, scopeHeight * 2)
    private val imageBuffer = PixelBuffer(scopeWidth, scopeHeight)
    private val decoder = DecoderEngine(scopeBuffer, imageBuffer, "Raw", sampleRate, lineRecoveryStrategy)
    private val _frames = MutableSharedFlow<SstvFrame>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private var lastInputRms = 0f
    private var lastAppliedGain = 1f
    private val _qualityMetrics = MutableStateFlow<SstvQualityMetrics?>(null)
    private val preFilter = if (enablePreFilter) HighPassFilter(preFilterCutoffHz, sampleRate.toDouble()) else null
    private val diagnosticsHandle = SstvDiagnosticsHandle(enableDiagnosticsHandle, _qualityMetrics)
    val frames: SharedFlow<SstvFrame> = _frames
    val supportedModes: List<String> = decoder.allModes.map { it.name }

    suspend fun feedSamples(samples: FloatArray) = withContext(Dispatchers.Default) {
        // Optional pre-filtering: remove DC offset and subsonic noise that can mask
        // weak signals and corrupt the RMS normalization baseline.
        preFilter?.apply(samples)

        // Optional RMS normalization: bring input to a consistent level so the
        // FM demodulator operates in a predictable region. However, this amplifies
        // noise proportionally. Aggressive RMS targets (e.g., 0.25) can hurt weak
        // signals by boosting noise floor. Safer defaults: 0.35-0.50 for noisy inputs.
        // Disable entirely for direct line-level inputs (e.g., receiver discriminator).
        val gain = if (enableRmsNormalization) normalise(samples) else GainInfo(0f, 1f)
        lastInputRms = gain.inputRms
        lastAppliedGain = gain.appliedGain
        val hasNewLines = decoder.process(samples, channelSelect)
        if (hasNewLines) emitFrame()
    }

    fun lockMode(modeName: String) = decoder.setMode(modeName)

    fun clearPixels() {
        imageBuffer.line = -1
        imageBuffer.pixels.fill(0)
        decoder.resetQuality()
        if (enableDiagnosticsHandle) _qualityMetrics.value = null
    }

    fun getDiagnosticsHandle(): SstvDiagnosticsHandle? = diagnosticsHandle.takeIf { it.enabled }

    /**
     * Export quality metrics for logging/diagnostics. Useful for profiling decode
     * performance on noisy recordings. Returns null before first frame is emitted.
     */
    @Suppress("unused")
    fun getQualityMetrics(): SstvQualityMetrics? {
        if (enableDiagnosticsHandle) return _qualityMetrics.value
        val q = decoder.quality()
        return SstvQualityMetrics(
            syncHitRate = q.syncHitRate,
            predictedLineBursts = q.predictedLineBursts,
            maxPredictedStreak = q.maxPredictedStreak,
            timingErrorSamples = q.timingErrorSamples
        )
    }

    private fun emitFrame() {
        val imageWidth = imageBuffer.width
        val imageHeight = imageBuffer.height
        val quality = decoder.quality()
        if (enableDiagnosticsHandle) {
            _qualityMetrics.value = SstvQualityMetrics(
                syncHitRate = quality.syncHitRate,
                predictedLineBursts = quality.predictedLineBursts,
                maxPredictedStreak = quality.maxPredictedStreak,
                timingErrorSamples = quality.timingErrorSamples
            )
        }
        val imageComplete = imageBuffer.line >= imageHeight && imageBuffer.line > 0
        // Copy only the active image region — imageBuffer.pixels is pre-allocated
        // at the maximum possible size (PD-290: 800×616), so we must not copyOf()
        // the entire array and send padding pixels to the observer.
        val imagePixels = if (imageBuffer.line > 0) imageBuffer.pixels.copyOf(imageWidth * imageHeight) else null
        val modeName = decoder.currentMode.name
        val scopePixels = if (includeScopeData) scopeBuffer.pixels.copyOf() else null
        val scopeWidth = if (includeScopeData) scopeBuffer.width else 0
        val scopeHeight = if (includeScopeData) scopeBuffer.height else 0
        _frames.tryEmit(
            SstvFrame(
                scopePixels = scopePixels,
                scopeWidth = scopeWidth,
                scopeHeight = scopeHeight,
                imagePixels = imagePixels,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                modeName = modeName,
                imageComplete = imageComplete,
                inputRms = lastInputRms,
                appliedGain = lastAppliedGain,
                syncHitRate = quality.syncHitRate,
                predictedLineBursts = quality.predictedLineBursts,
                maxPredictedStreak = quality.maxPredictedStreak,
                timingErrorSamples = quality.timingErrorSamples
            )
        )
    }

    // Target RMS level for the normalizer. 0.25 leaves headroom while keeping the
    // FM demodulator well above its noise floor regardless of input gain.
    // TUNING GUIDE:
    //   - 0.20-0.25: Aggressive, best for clean direct-coupled inputs, worst for mic noise
    //   - 0.35-0.40: Moderate, good balance for typical phone/mic inputs (RECOMMENDED)
    //   - 0.50-0.60: Conservative, best for noisy environments, reduces amplitude resolution
    // Disable RMS normalization entirely if using a professional receiver discriminator output.
    private val targetRms = targetRmsLevel.coerceIn(0.05f, 0.8f)

    private class GainInfo(
        val inputRms: Float,
        val appliedGain: Float
    )

    // Bring the buffer to a fixed RMS so that microphone and direct-coupled inputs
    // both decode reliably. The guard prevents amplifying pure silence into noise.
    private fun normalise(buffer: FloatArray): GainInfo {
        var sumSq = 0f
        for (s in buffer) sumSq += s * s
        val rms = sqrt(sumSq / buffer.size)
        if (rms > 1e-6f) {
            val gain = targetRms / rms
            for (i in buffer.indices) buffer[i] *= gain
            return GainInfo(rms, gain)
        }
        return GainInfo(rms, 1f)
    }
}

/**
 * Simple high-pass filter to remove DC offset and subsonic interference before RMS
 * normalization. Improves noise floor estimation and prevents low-freq noise from
 * corrupting the gain calculation.
 *
 * Design: First-order butterworth (pole at cutoff frequency). Fast, minimal latency,
 * suitable for real-time preprocessing.
 */
internal class HighPassFilter(cutoffHz: Double, sampleRateHz: Double) {
    private val alpha: Float
    private var prevInput = 0f
    private var prevOutput = 0f

    init {
        // First-order pole placement: alpha = wc / (wc + ws) where wc = 2*pi*fc, ws = 2*pi*fs
        val omega = 2f * PI.toFloat() * (cutoffHz / sampleRateHz).toFloat()
        alpha = omega / (omega + 1f)
    }

    fun apply(buffer: FloatArray) {
        for (i in buffer.indices) {
            val input = buffer[i]
            prevOutput = alpha * (prevOutput + input - prevInput)
            buffer[i] = prevOutput
            prevInput = input
        }
    }
}

internal class DecoderQuality(
    val syncHitRate: Float,
    val predictedLineBursts: Int,
    val maxPredictedStreak: Int,
    val timingErrorSamples: Int
)

internal enum class SyncPulseWidth { FiveMs, NineMs, TwentyMs }

internal class SyncPulseDetector(sampleRate: Int) {

    companion object {
        const val SYNC_FREQ = 1200.0
        const val BLACK_FREQ = 1500.0
        const val WHITE_FREQ = 2300.0
    }

    var detectedWidth: SyncPulseWidth = SyncPulseWidth.NineMs; private set
    var pulseOffset: Int = 0; private set
    var freqOffset: Float = 0f; private set

    private val bandwidth = WHITE_FREQ - BLACK_FREQ
    private val fm = FmDemodulator(bandwidth, sampleRate.toDouble())
    private val min5ms: Int = round(0.0025 * sampleRate).toInt()
    private val max5ms: Int = round(0.007 * sampleRate).toInt()
    private val max9ms: Int = round(0.0145 * sampleRate).toInt()
    private val max20ms: Int = round(0.025 * sampleRate).toInt()
    private val filterDelay: Int
    private val avgFilter: MovingAverage
    private val delayLine: Delay
    private val lowPass: ComplexFirFilter
    private val oscillator: Phasor
    private val syncFreqValue: Float
    private val syncFreqTolerance: Float
    private val trigger: SchmittTrigger
    private var counter = 0
    private var baseBand = Complex()

    init {
        val filterLen = round(0.0025 * sampleRate).toInt() or 1
        filterDelay = (filterLen - 1) / 2
        avgFilter = MovingAverage(filterLen)
        delayLine = Delay(filterLen)
        val loFreq = 1000.0
        val hiFreq = 2800.0
        val cutoff = (hiFreq - loFreq) / 2
        val lpLen = round(0.002 * sampleRate).toInt() or 1
        lowPass = ComplexFirFilter(lpLen)
        for (i in 0 until lpLen)
            lowPass.taps[i] = (WindowFunctions.kaiser(2.0, i, lpLen) * WindowFunctions.sinc(
                cutoff,
                sampleRate.toDouble(),
                i,
                lpLen
            )).toFloat()
        val center = (loFreq + hiFreq) / 2
        oscillator = Phasor(-center, sampleRate.toDouble())
        syncFreqValue = ((SYNC_FREQ - center) * 2 / bandwidth).toFloat()
        syncFreqTolerance = (50 * 2 / bandwidth).toFloat()
        val porchFreq = 1500.0
        val hiThresh = (SYNC_FREQ + porchFreq) / 2
        val loThresh = (SYNC_FREQ + hiThresh) / 2
        trigger = SchmittTrigger(
            ((loThresh - center) * 2 / bandwidth).toFloat(),
            ((hiThresh - center) * 2 / bandwidth).toFloat()
        )
    }

    fun process(buffer: FloatArray, channelSelect: Int): Boolean {
        var detected = false
        val channels = if (channelSelect > 0) 2 else 1
        // NOTE: buffer[i] is overwritten in-place with the FM-demodulated frequency
        // value for every mono sample (channelSelect == 0). DecoderEngine.process()
        // reads back these values to populate scanLineBuffer. Callers must not reuse
        // the buffer after this call.
        for (i in 0 until buffer.size / channels) {
            when (channelSelect) {
                1 -> baseBand.set(buffer[2 * i])
                2 -> baseBand.set(buffer[2 * i + 1])
                3 -> baseBand.set(buffer[2 * i] + buffer[2 * i + 1])
                4 -> baseBand.set(buffer[2 * i], buffer[2 * i + 1])
                else -> baseBand.set(buffer[i])
            }
            baseBand = lowPass.filter(baseBand.mul(oscillator.rotate()))
            val freq = fm.demodulate(baseBand)
            val avg = avgFilter.avg(freq)
            val delayed = delayLine.push(avg)
            buffer[i] = freq
            if (!trigger.process(avg)) {
                ++counter
            } else if (counter !in min5ms..max20ms || abs(delayed - syncFreqValue) > syncFreqTolerance) {
                counter = 0
            } else {
                detectedWidth = when {
                    counter < max5ms -> SyncPulseWidth.FiveMs
                    counter < max9ms -> SyncPulseWidth.NineMs
                    else -> SyncPulseWidth.TwentyMs
                }
                pulseOffset = i - filterDelay
                freqOffset = delayed - syncFreqValue
                detected = true
                counter = 0
            }
        }
        return detected
    }
}

internal class DecoderEngine(
    private val scopeBuffer: PixelBuffer,
    private val imageBuffer: PixelBuffer,
    rawName: String,
    sampleRate: Int,
    lineRecoveryStrategy: LineRecoveryStrategy
) {
    private val pixelBuffer = PixelBuffer(800, 2)
    private val detector = SyncPulseDetector(sampleRate)
    private val pulseFilter: MovingAverage
    private val pulseFilterDelay: Int
    private val scanLineBuffer: FloatArray
    private val scratch: FloatArray
    private val sync5ms = IntArray(5)
    private val sync9ms = IntArray(5)
    private val sync20ms = IntArray(5)
    private val lines5ms = IntArray(4)
    private val lines9ms = IntArray(4)
    private val lines20ms = IntArray(4)
    private val offsets5ms = FloatArray(5)
    private val offsets9ms = FloatArray(5)
    private val offsets20ms = FloatArray(5)
    private val visFreqs = FloatArray(10)
    private val scanLineMin: Int
    private val syncTolerance: Int
    private val lineTolerance: Int
    private val leaderLen: Int
    private val leaderTol: Int
    private val transition: Int
    private val visBitLen: Int
    private val visLen: Int
    private val rawMode: SstvMode
    private val modes5ms: ArrayList<SstvMode>
    private val modes9ms: ArrayList<SstvMode>
    private val modes20ms: ArrayList<SstvMode>

    var currentMode: SstvMode; private set
    val allModes: List<SstvMode> get() = modes5ms + modes9ms + modes20ms

    private var lockMode = false
    private var sample = 0
    private var leaderBreak = 0
    private var lastSync = 0
    private var curLineSamples: Int
    private var lastOffset = 0f
    private var syncChecks = 0
    private var syncHits = 0
    private var predictedLineBursts = 0
    private var predictedStreak = 0
    private var maxPredictedStreak = 0
    private var timingErrorSamples = 0

    // Look4Sat strategy limits synthetic lines to avoid visible vertical collapse.
    // Robot36 strategy preserves legacy behavior by allowing unlimited synthesis.
    private val maxConsecutivePredictedLines = when (lineRecoveryStrategy) {
        LineRecoveryStrategy.Look4SatLimited -> 2
        LineRecoveryStrategy.Robot36Compatible -> Int.MAX_VALUE
    }

    init {
        imageBuffer.line = -1
        // Pre-allocate for the largest possible mode (PD-290: 800×616 = 492 800 ints)
        // so that handleHeader/processPulse can reuse the array with a fill(0) instead
        // of allocating a fresh IntArray on every new image, reducing GC pressure.
        imageBuffer.pixels = IntArray(800 * 616)
        val pfLen = round(0.0025 * sampleRate).toInt() or 1
        pulseFilterDelay = (pfLen - 1) / 2
        pulseFilter = MovingAverage(pfLen)
        scanLineBuffer = FloatArray(round(7.0 * sampleRate).toInt())
        scratch = FloatArray(round(1.1 * sampleRate).toInt())
        leaderLen = round(0.3 * sampleRate).toInt()
        leaderTol = round(0.06 * sampleRate).toInt()
        transition = round(0.0005 * sampleRate).toInt()
        visBitLen = round(0.03 * sampleRate).toInt()
        visLen = round(0.3 * sampleRate).toInt()
        scanLineMin = round(0.05 * sampleRate).toInt()
        syncTolerance = round(0.03 * sampleRate).toInt()
        lineTolerance = round(0.001 * sampleRate).toInt()
        rawMode = RawMode(rawName, sampleRate)
        val robot36 = Robot36Mode(sampleRate)
        currentMode = robot36
        curLineSamples = robot36.scanLineSamples
        modes5ms = arrayListOf(
            RgbMode.wraaseSc2180(sampleRate),
            RgbMode.martin("1", 44, 0.146432, sampleRate),
            RgbMode.martin("2", 40, 0.073216, sampleRate)
        )
        modes9ms = arrayListOf(
            robot36, Robot72Mode(sampleRate),
            RgbMode.scottie("1", 60, 0.138240, sampleRate),
            RgbMode.scottie("2", 56, 0.088064, sampleRate),
            RgbMode.scottie("DX", 76, 0.3456, sampleRate)
        )
        modes20ms = arrayListOf(
            PdMode("50", 93, 320, 256, 0.09152, sampleRate),
            PdMode("90", 99, 320, 256, 0.17024, sampleRate),
            PdMode("120", 95, 640, 496, 0.1216, sampleRate),
            PdMode("160", 98, 512, 400, 0.195584, sampleRate),
            PdMode("180", 96, 640, 496, 0.18304, sampleRate),
            PdMode("240", 97, 640, 496, 0.24448, sampleRate),
            PdMode("290", 94, 800, 616, 0.2288, sampleRate)
        )
    }

    fun process(recordBuffer: FloatArray, channelSelect: Int): Boolean {
        var newLines = false
        val detected = detector.process(recordBuffer, channelSelect)
        syncChecks++
        if (detected) {
            syncHits++
            predictedStreak = 0
        }
        var syncIdx = sample + detector.pulseOffset
        val channels = if (channelSelect > 0) 2 else 1
        for (j in 0 until recordBuffer.size / channels) {
            if (sample >= scanLineBuffer.size) {
                shift(curLineSamples)
                syncIdx -= curLineSamples
                if (sample >= scanLineBuffer.size) sample = scanLineBuffer.size - 1
            }
            scanLineBuffer[sample++] = recordBuffer[j]
        }
        if (detected) {
            when (detector.detectedWidth) {
                SyncPulseWidth.FiveMs -> newLines = processPulse(modes5ms, offsets5ms, sync5ms, lines5ms, syncIdx)
                SyncPulseWidth.NineMs -> {
                    leaderBreak = syncIdx; newLines = processPulse(modes9ms, offsets9ms, sync9ms, lines9ms, syncIdx)
                }

                SyncPulseWidth.TwentyMs -> {
                    leaderBreak = syncIdx; newLines = processPulse(modes20ms, offsets20ms, sync20ms, lines20ms, syncIdx)
                }
            }
        } else if (handleHeader()) {
            predictedStreak = 0
            newLines = true
        } else if (sample > lastSync + (curLineSamples * 5) / 4) {
            newLines = decodePredictedLine()
        }
        return newLines
    }

    fun setMode(name: String) {
        val mode = allModes.firstOrNull { it.name == name }
        if (mode == currentMode) {
            lockMode = true; return
        }
        if (mode != null) {
            lockMode = true; imageBuffer.line = -1; currentMode = mode; curLineSamples = mode.scanLineSamples; return
        }
        lockMode = false
    }

    fun quality(): DecoderQuality {
        val hitRate = if (syncChecks > 0) syncHits.toFloat() / syncChecks else 0f
        return DecoderQuality(
            syncHitRate = hitRate,
            predictedLineBursts = predictedLineBursts,
            maxPredictedStreak = maxPredictedStreak,
            timingErrorSamples = timingErrorSamples
        )
    }

    fun resetQuality() {
        syncChecks = 0
        syncHits = 0
        predictedLineBursts = 0
        predictedStreak = 0
        maxPredictedStreak = 0
        timingErrorSamples = 0
    }

    private fun mean(a: IntArray): Double = a.sumOf { it.toDouble() } / a.size
    private fun stdDev(a: IntArray, m: Double): Double {
        var s = 0.0; for (v in a) s += (v - m) * (v - m); return sqrt(s / a.size)
    }

    private fun meanF(a: FloatArray): Float {
        var s = 0f; for (v in a) s += v; return s / a.size
    }

    private fun detectMode(modes: ArrayList<SstvMode>, samples: Int): SstvMode {
        var best: SstvMode = rawMode
        var bestD = Int.MAX_VALUE
        for (m in modes) {
            val d = abs(samples - m.scanLineSamples); if (d <= lineTolerance && d < bestD) {
                bestD = d; best = m
            }
        }
        return best
    }

    // scopeBuffer is twice the display height. Each decoded scan line is written
    // to both the current rolling position (top half, wraps at height/2) and the
    // same row offset in the bottom half. The UI displays a window that always
    // spans the half-height boundary, giving a seamless non-wrapping scroll effect.
    private fun copyUnscaled() {
        val w = minOf(scopeBuffer.width, pixelBuffer.width)
        for (row in 0 until pixelBuffer.height) {
            val line = scopeBuffer.width * scopeBuffer.line
            pixelBuffer.pixels.copyInto(scopeBuffer.pixels, line, row * pixelBuffer.width, row * pixelBuffer.width + w)
            scopeBuffer.pixels.fill(0, line + w, line + scopeBuffer.width)

            scopeBuffer.pixels.copyInto(
                scopeBuffer.pixels,
                scopeBuffer.width * (scopeBuffer.line + scopeBuffer.height / 2),
                line,
                line + scopeBuffer.width
            )
            scopeBuffer.line = (scopeBuffer.line + 1) % (scopeBuffer.height / 2)
        }
    }

    private fun copyScaled(scale: Int) {
        for (row in 0 until pixelBuffer.height) {
            val line = scopeBuffer.width * scopeBuffer.line
            for (col in 0 until pixelBuffer.width) for (i in 0 until scale) scopeBuffer.pixels[line + col * scale + i] =
                pixelBuffer.pixels[pixelBuffer.width * row + col]
            scopeBuffer.pixels.fill(0, line + pixelBuffer.width * scale, line + scopeBuffer.width)

            scopeBuffer.pixels.copyInto(
                scopeBuffer.pixels,
                scopeBuffer.width * (scopeBuffer.line + scopeBuffer.height / 2),
                line,
                line + scopeBuffer.width
            )
            scopeBuffer.line = (scopeBuffer.line + 1) % (scopeBuffer.height / 2)
            repeat(scale - 1) {

                scopeBuffer.pixels.copyInto(
                    scopeBuffer.pixels, scopeBuffer.width * scopeBuffer.line, line, line + scopeBuffer.width
                )

                scopeBuffer.pixels.copyInto(
                    scopeBuffer.pixels,
                    scopeBuffer.width * (scopeBuffer.line + scopeBuffer.height / 2),
                    line,
                    line + scopeBuffer.width
                )
                scopeBuffer.line = (scopeBuffer.line + 1) % (scopeBuffer.height / 2)
            }
        }
    }

    private fun copyLines(ok: Boolean) {
        if (!ok) return
        var finish = false
        if (imageBuffer.line in 0 until imageBuffer.height && imageBuffer.width == pixelBuffer.width) {
            val w = imageBuffer.width
            for (row in 0 until pixelBuffer.height) {
                if (imageBuffer.line >= imageBuffer.height) break
                pixelBuffer.pixels.copyInto(imageBuffer.pixels, imageBuffer.line * w, row * w, row * w + w)
                imageBuffer.line++
            }
            finish = imageBuffer.line == imageBuffer.height
        }
        val scale = scopeBuffer.width / pixelBuffer.width
        if (scale <= 1) copyUnscaled() else copyScaled(scale)
        if (finish) drawLines(0xff000000.toInt(), 10)
    }

    private fun decodePredictedLine(): Boolean {
        // Avoid long streaks of synthetic lines; once we exceed the cap we wait for
        // real sync to reduce visible vertical compression on weak/noisy signals.
        if (predictedStreak >= maxConsecutivePredictedLines) return false
        val expectedSync = lastSync + curLineSamples
        timingErrorSamples = sample - expectedSync
        val decoded = currentMode.decodeScanLine(
            pixelBuffer,
            scratch,
            scanLineBuffer,
            scopeBuffer.width,
            lastSync,
            curLineSamples,
            lastOffset
        )
        copyLines(decoded)
        lastSync = expectedSync
        predictedLineBursts++
        predictedStreak++
        if (predictedStreak > maxPredictedStreak) maxPredictedStreak = predictedStreak
        return decoded
    }

    private fun drawLines(color: Int, count: Int) {
        repeat(count) {
            scopeBuffer.pixels.fill(
                color,
                scopeBuffer.line * scopeBuffer.width,
                (scopeBuffer.line + 1) * scopeBuffer.width
            )
            scopeBuffer.pixels.fill(
                color,
                (scopeBuffer.line + scopeBuffer.height / 2) * scopeBuffer.width,
                (scopeBuffer.line + 1 + scopeBuffer.height / 2) * scopeBuffer.width
            )
            scopeBuffer.line = (scopeBuffer.line + 1) % (scopeBuffer.height / 2)
        }
    }

    private fun adjust(pulses: IntArray, shift: Int) {
        for (i in pulses.indices) pulses[i] -= shift
    }

    private fun shift(amount: Int) {
        if ((amount <= 0) || (amount > sample)) return
        sample -= amount; leaderBreak -= amount; lastSync -= amount
        adjust(sync5ms, amount); adjust(sync9ms, amount); adjust(sync20ms, amount)
        // Discard already-decoded samples by sliding the live region back to index 0.
        // System.arraycopy handles the overlapping regions correctly and is a native
        // memcpy on JVM, so this is fast despite moving the full remaining window.
        scanLineBuffer.copyInto(scanLineBuffer, 0, amount, amount + sample)
    }

    private fun handleHeader(): Boolean {
        if (leaderBreak < visBitLen + leaderTol || sample < leaderBreak + leaderLen + leaderTol + visLen + visBitLen) return false
        val bp = leaderBreak; leaderBreak = 0
        var preFreq = 0f
        for (i in 0 until leaderTol) preFreq += scanLineBuffer[bp - visBitLen - leaderTol + i]
        val toneFreq = 1900f
        val center = 1900f
        val tol = 50f
        val halfBw = 400f
        preFreq = preFreq * halfBw / leaderTol + center
        if (abs(preFreq - toneFreq) > tol) return false
        var ldrFreq = 0f
        for (i in transition until leaderLen - leaderTol) ldrFreq += scanLineBuffer[bp + i]
        val ldrOffset = ldrFreq / (leaderLen - transition - leaderTol)
        ldrFreq = ldrOffset * halfBw + center
        if (abs(ldrFreq - toneFreq) > tol) return false
        val stopFreq = 1200f
        val pulseThr = ((stopFreq + toneFreq) / 2 - center) / halfBw
        var vBegin = bp + leaderLen - leaderTol
        val vEnd = bp + leaderLen + leaderTol + visBitLen
        repeat(pulseFilter.length) { pulseFilter.avg(scanLineBuffer[vBegin++] - ldrOffset) }
        while (++vBegin < vEnd) if (pulseFilter.avg(scanLineBuffer[vBegin] - ldrOffset) < pulseThr) break
        if (vBegin >= vEnd) return false
        vBegin -= pulseFilterDelay
        val visEnd = vBegin + visLen
        visFreqs.fill(0f)
        for (j in 0 until 10) for (i in transition until visBitLen - transition) visFreqs[j] += scanLineBuffer[vBegin + visBitLen * j + i] - ldrOffset
        for (i in 0 until 10) visFreqs[i] = visFreqs[i] * halfBw / (visBitLen - 2 * transition) + center
        if (abs(visFreqs[0] - stopFreq) > tol || abs(visFreqs[9] - stopFreq) > tol) return false
        for (i in 1 until 9) if (abs(visFreqs[i] - 1100f) > tol && abs(visFreqs[i] - 1300f) > tol) return false
        var vis = 0
        for (i in 0 until 8) vis = vis or ((if (visFreqs[i + 1] < stopFreq) 1 else 0) shl i)
        var chk = true; for (i in 0 until 8) chk = chk xor ((vis and (1 shl i)) != 0)
        vis = vis and 127; if (!chk) return false
        val syncThr = ((1200f + 1500f) / 2 - center) / halfBw
        var sIdx = visEnd - visBitLen
        val sMax = visEnd + visBitLen
        repeat(pulseFilter.length) { pulseFilter.avg(scanLineBuffer[sIdx++] - ldrOffset) }
        while (++sIdx < sMax) if (pulseFilter.avg(scanLineBuffer[sIdx] - ldrOffset) > syncThr) break
        if (sIdx >= sMax) return false
        sIdx -= pulseFilterDelay
        val mode: SstvMode
        val pulses: IntArray
        val lines: IntArray
        val f5 = modes5ms.firstOrNull { it.visCode == vis }
        val f9 = modes9ms.firstOrNull { it.visCode == vis }
        val f20 = modes20ms.firstOrNull { it.visCode == vis }
        when {
            f5 != null -> {
                mode = f5; pulses = sync5ms; lines = lines5ms
            }

            f9 != null -> {
                mode = f9; pulses = sync9ms; lines = lines9ms
            }

            f20 != null -> {
                mode = f20; pulses = sync20ms; lines = lines20ms
            }

            else -> {
                if (!lockMode) drawLines(0xffff0000.toInt(), 8); return false
            }
        }
        if (lockMode && mode != currentMode) return false
        mode.resetState()
        imageBuffer.width = mode.width; imageBuffer.height = mode.height
        imageBuffer.pixels.fill(0, 0, mode.width * mode.height); imageBuffer.line = 0
        currentMode = mode
        lastSync = sIdx + mode.firstSyncPulseIndex; curLineSamples = mode.scanLineSamples; lastOffset = ldrOffset
        var oldest = lastSync - (pulses.size - 1) * curLineSamples
        if (mode.firstSyncPulseIndex > 0) oldest -= curLineSamples
        for (i in pulses.indices) pulses[i] = oldest + i * curLineSamples
        lines.fill(curLineSamples)
        shift(lastSync + mode.firstPixelSampleIndex)
        drawLines(0xff00ff00.toInt(), 8); drawLines(0xff000000.toInt(), 10)
        return true
    }

    private fun processPulse(
        modes: ArrayList<SstvMode>,
        freqOffs: FloatArray,
        syncPulses: IntArray,
        lineLen: IntArray,
        latest: Int
    ): Boolean {
        predictedStreak = 0
        for (i in 1 until syncPulses.size) syncPulses[i - 1] = syncPulses[i]
        syncPulses[syncPulses.size - 1] = latest
        for (i in 1 until lineLen.size) lineLen[i - 1] = lineLen[i]
        lineLen[lineLen.size - 1] = syncPulses.last() - syncPulses[syncPulses.size - 2]
        for (i in 1 until freqOffs.size) freqOffs[i - 1] = freqOffs[i]
        freqOffs[freqOffs.size - 1] = detector.freqOffset
        if (lineLen[0] == 0) return false
        val m = mean(lineLen)
        val lineSamples = round(m).toInt()
        if (lineSamples < scanLineMin || lineSamples > scratch.size) return false
        if (stdDev(lineLen, m) > lineTolerance) return false
        var changed = false
        if (lockMode || imageBuffer.line in 0 until imageBuffer.height) {
            if (currentMode != rawMode && abs(lineSamples - currentMode.scanLineSamples) > lineTolerance) return false
            // Try continuous decoding
            if (lockMode && imageBuffer.line == -1 && currentMode != rawMode) {
                currentMode.resetState()
                imageBuffer.width = currentMode.width
                imageBuffer.height = currentMode.height
                imageBuffer.pixels.fill(0, 0, currentMode.width * currentMode.height)
                imageBuffer.line = 0
                drawLines(0xff000000.toInt(), 10); drawLines(0xffffff00.toInt(), 8); drawLines(0xff000000.toInt(), 10)
            }
        } else {
            val prev = currentMode; currentMode = detectMode(modes, lineSamples)
            changed =
                currentMode != prev || abs(curLineSamples - lineSamples) > lineTolerance || abs(lastSync + lineSamples - syncPulses.last()) > syncTolerance
        }
        if (changed) {
            drawLines(0xff000000.toInt(), 10); drawLines(0xff00ffff.toInt(), 8); drawLines(0xff000000.toInt(), 10)
        }
        val offset = meanF(freqOffs)
        if (syncPulses[0] >= lineSamples && changed) {
            val end = syncPulses[0]
            val extra = end / lineSamples
            val first = end - extra * lineSamples
            var p = first; while (p < end) {
                copyLines(
                    currentMode.decodeScanLine(
                        pixelBuffer,
                        scratch,
                        scanLineBuffer,
                        scopeBuffer.width,
                        p,
                        lineSamples,
                        offset
                    )
                ); p += lineSamples
            }
        }
        val start = if (changed) 0 else lineLen.size - 1
        for (i in start until lineLen.size) copyLines(
            currentMode.decodeScanLine(
                pixelBuffer,
                scratch,
                scanLineBuffer,
                scopeBuffer.width,
                syncPulses[i],
                lineLen[i],
                offset
            )
        )
        lastSync = syncPulses.last(); curLineSamples = lineSamples; lastOffset = offset
        shift(lastSync + currentMode.firstPixelSampleIndex)
        return true
    }
}
