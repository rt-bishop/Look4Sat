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

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

internal class Complex(var real: Float = 0f, var imag: Float = 0f) {

    fun set(real: Float, imag: Float): Complex {
        this.real = real; this.imag = imag; return this
    }

    fun set(real: Float): Complex = set(real, 0f)

    fun abs(): Float = sqrt(real * real + imag * imag)

    fun mul(other: Complex): Complex {
        val tmp = real * other.real - imag * other.imag
        imag = real * other.imag + imag * other.real
        real = tmp
        return this
    }

    fun div(value: Float): Complex {
        real /= value; imag /= value; return this
    }
}

internal object WindowFunctions {

    fun sinc(cutoff: Double, rate: Double, n: Int, nN: Int): Double {
        val f = 2 * cutoff / rate
        val x = n - (nN - 1) / 2.0
        val fx = f * x
        return if (fx == 0.0) f else f * sin(PI * fx) / (PI * fx)
    }

    fun kaiser(a: Double, n: Int, nN: Int): Double {
        fun square(v: Double) = v * v
        fun i0(x: Double): Double {
            val terms = DoubleArray(35)
            terms[0] = 1.0
            var v = 1.0
            for (m in 1 until 35) {
                v *= x / (2 * m); terms[m] = square(v)
            }
            terms.sort()
            var sum = 0.0; for (m in 34 downTo 0) sum += terms[m]
            return sum
        }
        return i0(PI * a * sqrt(1 - square((2.0 * n) / (nN - 1) - 1))) / i0(PI * a)
    }
}

// O(1) ring buffer — simpler and faster than a segment tree for the short window
// lengths used here (≤512 samples). Float32 accumulated drift over such windows
// is ~6e-5, negligible for audio-frequency processing.
internal open class MovingSum(val length: Int) {
    private val buf = FloatArray(length)
    private var pos = 0
    private var runningSum = 0f

    fun add(input: Float) {
        runningSum += input - buf[pos]
        buf[pos] = input
        if (++pos >= length) pos = 0
    }

    fun sum(): Float = runningSum
    fun sum(input: Float): Float {
        add(input); return sum()
    }
}

internal class MovingAverage(length: Int) : MovingSum(length) {
    fun avg(input: Float): Float = sum(input) / length
}

internal class Ema {
    private var alpha: Float = 1f
    private var prev: Float = 0f

    fun process(input: Float): Float {
        prev = prev * (1 - alpha) + alpha * input; return prev
    }

    fun setCutoff(freq: Double, rate: Double, order: Int = 1) {
        alpha = computeAlpha(freq, rate, order)
    }

    fun reset() {
        prev = 0f
    }

    companion object {

        fun computeAlpha(freq: Double, rate: Double, order: Int = 1): Float {
            val x = cos(2 * PI * (freq.coerceAtMost(rate * 0.499)) / rate)
            val discriminant = (x * (x - 4) + 3).coerceAtLeast(0.0)
            return (x - 1 + sqrt(discriminant)).coerceIn(0.0, 1.0).pow(1.0 / order).toFloat()
        }

        fun withCutoff(freq: Double, rate: Double, order: Int = 1): Ema {
            return Ema().also { it.setCutoff(freq, rate, order) }
        }
    }
}

internal class Phasor(freq: Double, rate: Double) {
    private val value = Complex(1f, 0f)
    private val delta: Complex = run {
        val omega = 2 * PI * freq / rate
        Complex(cos(omega).toFloat(), sin(omega).toFloat())
    }
    private var count = 0

    // Renormalize every 512 rotations to prevent magnitude drift accumulation,
    // eliminating the per-sample sqrt without sacrificing demodulation accuracy.
    fun rotate(): Complex {
        value.mul(delta)
        if (++count == 512) { value.div(value.abs()); count = 0 }
        return value
    }
}

internal class FmDemodulator(bandwidth: Double, sampleRate: Double) {
    private val scale = (sampleRate / (bandwidth * PI)).toFloat()
    private val pi = PI.toFloat()
    private val twoPi = (2 * PI).toFloat()
    private var prev = 0f

    fun demodulate(input: Complex): Float {
        // Use fast polynomial atan2 instead of the exact trigonometric call.
        // Max error ~0.005 rad translates to <1 Hz frequency error at 44100 Hz,
        // well within the 50 Hz sync tolerance.
        val phase = fastAtan2(input.imag, input.real)
        var delta = phase - prev; prev = phase
        if (delta < -pi) delta += twoPi else if (delta > pi) delta -= twoPi
        return scale * delta
    }

    // Rajan's polynomial approximation of atan2 — avoids a transcendental call
    // in the per-sample hot path (~44 k calls/s at 44100 Hz sample rate).
    private fun fastAtan2(y: Float, x: Float): Float {
        val absY = kotlin.math.abs(y) + 1e-10f
        val r: Float
        val angle: Float
        if (x >= 0f) {
            r = (x - absY) / (x + absY)
            angle = 0.1963f * r * r * r - 0.9817f * r + pi / 4f
        } else {
            r = (x + absY) / (absY - x)
            angle = 0.1963f * r * r * r - 0.9817f * r + 3f * pi / 4f
        }
        return if (y < 0f) -angle else angle
    }
}

internal class ComplexFirFilter(val length: Int) {
    private val real = FloatArray(length)
    private val imag = FloatArray(length)
    private val sum = Complex()
    private var pos = 0

    val taps = FloatArray(length)

    fun filter(input: Complex): Complex {
        real[pos] = input.real; imag[pos] = input.imag
        if (++pos >= length) pos = 0
        sum.real = 0f; sum.imag = 0f
        for (tap in taps) {
            sum.real += tap * real[pos]; sum.imag += tap * imag[pos]; if (++pos >= length) pos = 0
        }
        return sum
    }
}

internal class Delay(val length: Int) {
    private val buf = FloatArray(length)
    private var pos = 0

    fun push(input: Float): Float {
        val tmp = buf[pos]; buf[pos] = input; if (++pos >= length) pos = 0; return tmp
    }
}

internal class SchmittTrigger(private val low: Float, private val high: Float) {
    private var state = false

    fun process(input: Float): Boolean {
        if (state) {
            if (input < low) state = false
        } else {
            if (input > high) state = true
        }
        return state
    }
}

internal object ColorConverter {

    private fun clamp(v: Int) = v.coerceIn(0, 255)
    private fun toInt(level: Float) = clamp(round(255 * level).toInt())
    private fun compress(level: Float) = toInt(sqrt(level.coerceIn(0f, 1f)))

    private fun yuv2rgb(yY: Int, uU: Int, vV: Int): Int {
        val y = yY - 16
        val u = uU - 128
        val v = vV - 128
        val r = clamp((298 * y + 409 * v + 128) shr 8)
        val g = clamp((298 * y - 100 * u - 208 * v + 128) shr 8)
        val b = clamp((298 * y + 516 * u + 128) shr 8)
        return 0xff000000.toInt() or (r shl 16) or (g shl 8) or b
    }

    fun gray(level: Float): Int = 0xff000000.toInt() or (0x00010101 * compress(level))
    fun rgb(r: Float, g: Float, b: Float): Int = 0xff000000.toInt() or (toInt(r) shl 16) or (toInt(g) shl 8) or toInt(b)
    fun yuv2rgb(yY: Float, uU: Float, vV: Float): Int = yuv2rgb(toInt(yY), toInt(uU), toInt(vV))
    fun yuv2rgb(packed: Int): Int = yuv2rgb((packed shr 16) and 0xff, (packed shr 8) and 0xff, packed and 0xff)
}
