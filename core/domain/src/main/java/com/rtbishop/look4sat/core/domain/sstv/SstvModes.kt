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

import kotlin.math.round

internal class PixelBuffer(var width: Int, var height: Int) {
    var pixels = IntArray(width * height)
    var line = 0
}

internal sealed interface SstvMode {
    val name: String
    val visCode: Int
    val width: Int
    val height: Int
    val firstPixelSampleIndex: Int
    val firstSyncPulseIndex: Int
    val scanLineSamples: Int

    fun resetState() {}

    fun decodeScanLine(
        pixelBuffer: PixelBuffer,
        scratch: FloatArray,
        scanLine: FloatArray,
        scopeWidth: Int,
        syncPulseIndex: Int,
        lineSamples: Int,
        freqOffset: Float
    ): Boolean
}

internal class RgbMode(
    override val name: String,
    override val visCode: Int,
    private val hPixels: Int,
    private val vPixels: Int,
    override val firstSyncPulseIndex: Int,
    override val scanLineSamples: Int,
    override val firstPixelSampleIndex: Int,
    private val redBegin: Int,
    private val redLen: Int,
    private val greenBegin: Int,
    private val greenLen: Int,
    private val blueBegin: Int,
    private val blueLen: Int,
    private val endSamples: Int,
) : SstvMode {
    override val width get() = hPixels
    override val height get() = vPixels
    private val ema = Ema.withCutoff(hPixels.toDouble(), (2 * greenLen).toDouble(), 2)

    override fun decodeScanLine(
        pixelBuffer: PixelBuffer,
        scratch: FloatArray,
        scanLine: FloatArray,
        scopeWidth: Int,
        syncPulseIndex: Int,
        lineSamples: Int,
        freqOffset: Float
    ): Boolean {
        val begin = firstPixelSampleIndex
        if (syncPulseIndex + begin < 0 || syncPulseIndex + endSamples > scanLine.size) return false
        ema.reset()
        for (i in 0 until endSamples - begin) scratch[i] = ema.process(scanLine[syncPulseIndex + begin + i])
        ema.reset()
        for (i in endSamples - begin - 1 downTo 0) scratch[i] = freqToLevel(ema.process(scratch[i]), freqOffset)
        for (i in 0 until hPixels) {
            val r = redBegin + (i * redLen) / hPixels
            val g = greenBegin + (i * greenLen) / hPixels
            val b = blueBegin + (i * blueLen) / hPixels
            pixelBuffer.pixels[i] = ColorConverter.rgb(scratch[r], scratch[g], scratch[b])
        }
        pixelBuffer.width = hPixels; pixelBuffer.height = 1
        return true
    }

    companion object {
        fun martin(variant: String, code: Int, channelSec: Double, sampleRate: Int): RgbMode {
            val sync = 0.004862
            val sep = 0.000572
            val scanLine = sync + sep + 3 * (channelSec + sep)
            val gEnd = sep + channelSec
            val bBegin = gEnd + sep
            val bEnd = bBegin + channelSec
            val rBegin = bEnd + sep
            val rEnd = rBegin + channelSec
            return fromSeconds(
                name = "Martin $variant",
                code = code,
                firstSyncSec = 0.0,
                scanLineSec = scanLine,
                beginSec = sep,
                rBeginSec = rBegin,
                rEndSec = rEnd,
                gBeginSec = sep,
                gEndSec = gEnd,
                bBeginSec = bBegin,
                bEndSec = bEnd,
                endSec = rEnd,
                sr = sampleRate
            )
        }

        fun scottie(variant: String, code: Int, channelSec: Double, sampleRate: Int): RgbMode {
            val sync = 0.009
            val sep = 0.0015
            val firstSync = sync + 2 * (sep + channelSec)
            val scanLine = sync + 3 * (channelSec + sep)
            val bEnd = -sync
            val bBegin = bEnd - channelSec
            val gEnd = bBegin - sep
            val gBegin = gEnd - channelSec
            val rEnd = sep + channelSec
            return fromSeconds(
                name = "Scottie $variant",
                code = code,
                firstSyncSec = firstSync,
                scanLineSec = scanLine,
                beginSec = gBegin,
                rBeginSec = sep,
                rEndSec = rEnd,
                gBeginSec = gBegin,
                gEndSec = gEnd,
                bBeginSec = bBegin,
                bEndSec = bEnd,
                endSec = rEnd,
                sr = sampleRate
            )
        }

        fun wraaseSc2180(sampleRate: Int): RgbMode {
            val sync = 0.0055225
            val porch = 0.0005
            val ch = 0.235
            val scanLine = sync + porch + 3 * ch
            val rEnd = porch + ch
            val gEnd = rEnd + ch
            val bEnd = gEnd + ch
            return fromSeconds(
                name = "Wraase SC2-180",
                code = 55,
                firstSyncSec = 0.0,
                scanLineSec = scanLine,
                beginSec = porch,
                rBeginSec = porch,
                rEndSec = rEnd,
                gBeginSec = rEnd,
                gEndSec = gEnd,
                bBeginSec = gEnd,
                bEndSec = bEnd,
                endSec = bEnd,
                sr = sampleRate
            )
        }

        private fun fromSeconds(
            name: String, code: Int, w: Int = 320, h: Int = 256,
            firstSyncSec: Double, scanLineSec: Double, beginSec: Double,
            rBeginSec: Double, rEndSec: Double, gBeginSec: Double, gEndSec: Double,
            bBeginSec: Double, bEndSec: Double, endSec: Double, sr: Int
        ): RgbMode {
            val begin = round(beginSec * sr).toInt()
            return RgbMode(
                name = name, visCode = code, hPixels = w, vPixels = h,
                firstSyncPulseIndex = round(firstSyncSec * sr).toInt(),
                scanLineSamples = round(scanLineSec * sr).toInt(),
                firstPixelSampleIndex = begin,
                redBegin = round(rBeginSec * sr).toInt() - begin,
                redLen = round((rEndSec - rBeginSec) * sr).toInt(),
                greenBegin = round(gBeginSec * sr).toInt() - begin,
                greenLen = round((gEndSec - gBeginSec) * sr).toInt(),
                blueBegin = round(bBeginSec * sr).toInt() - begin,
                blueLen = round((bEndSec - bBeginSec) * sr).toInt(),
                endSamples = round(endSec * sr).toInt()
            )
        }
    }
}

internal class Robot36Mode(sampleRate: Int) : SstvMode {
    override val name = "Robot 36 Color"
    override val visCode = 8
    override val width = 320
    override val height = 240
    override val firstSyncPulseIndex = 0
    override val scanLineSamples: Int
    override val firstPixelSampleIndex: Int

    private val lumSamples: Int
    private val sepSamples: Int
    private val chromSamples: Int
    private val lumBegin: Int
    private val sepBegin: Int
    private val chromBegin: Int
    private val end: Int
    private val ema: Ema
    private var lastEven = false

    init {
        val syncPorch = 0.003
        val lum = 0.088
        val sep = 0.0045
        val porch = 0.0015
        val chrom = 0.044
        scanLineSamples = round((0.009 + syncPorch + lum + sep + porch + chrom) * sampleRate).toInt()
        lumSamples = round(lum * sampleRate).toInt(); sepSamples = round(sep * sampleRate).toInt()
        chromSamples = round(chrom * sampleRate).toInt()
        lumBegin = round(syncPorch * sampleRate).toInt(); firstPixelSampleIndex = lumBegin
        sepBegin = round((syncPorch + lum) * sampleRate).toInt()
        chromBegin = round((syncPorch + lum + sep + porch) * sampleRate).toInt()
        end = round((syncPorch + lum + sep + porch + chrom) * sampleRate).toInt()
        ema = Ema.withCutoff(width.toDouble(), (2 * lumSamples).toDouble(), 2)
    }

    override fun resetState() {
        lastEven = false
    }

    override fun decodeScanLine(
        pixelBuffer: PixelBuffer,
        scratch: FloatArray,
        scanLine: FloatArray,
        scopeWidth: Int,
        syncPulseIndex: Int,
        lineSamples: Int,
        freqOffset: Float
    ): Boolean {
        if (syncPulseIndex + firstPixelSampleIndex < 0 || syncPulseIndex + end > scanLine.size) return false
        var sep = 0f
        for (i in 0 until sepSamples) sep += scanLine[syncPulseIndex + sepBegin + i]
        sep = sep / sepSamples - freqOffset
        var even = sep < 0
        if (sep < -1.1f || (sep > -0.9f && sep < 0.9f) || sep > 1.1f) even = !lastEven
        lastEven = even
        ema.reset()
        for (i in firstPixelSampleIndex until end) scratch[i] = ema.process(scanLine[syncPulseIndex + i])
        ema.reset()
        for (i in end - 1 downTo firstPixelSampleIndex) scratch[i] = freqToLevel(ema.process(scratch[i]), freqOffset)
        for (i in 0 until width) {
            val lPos = lumBegin + (i * lumSamples) / width
            val cPos = chromBegin + (i * chromSamples) / width
            if (even) {
                // Even line: store Y in the red channel slot and Cr in the blue slot,
                // using ColorConverter.rgb() as a convenient 3×byte packer (not RGB).
                // The odd line will read these back and combine with its own Cb to
                // produce the final YUV→RGB conversion for both rows.
                pixelBuffer.pixels[i] = ColorConverter.rgb(scratch[lPos], 0f, scratch[cPos])
            } else {
                val evenYuv = pixelBuffer.pixels[i]
                // Even pixel packing: 0xAARRGGBB → Y=RR, Cb=GG(unused), Cr=BB
                // Odd pixel:  Y=lPos, Cb=cPos, Cr=(borrowed from even's BB slot)
                // Merge: take Y+Cr from even row (bits 0x00ff00ff) and Cb from odd (0x0000ff00).
                val oddYuv = ColorConverter.rgb(scratch[lPos], scratch[cPos], 0f)
                pixelBuffer.pixels[i] = ColorConverter.yuv2rgb((evenYuv and 0x00ff00ff) or (oddYuv and 0x0000ff00))
                pixelBuffer.pixels[i + width] =
                    ColorConverter.yuv2rgb((oddYuv and 0x00ffff00) or (evenYuv and 0x000000ff))
            }
        }
        pixelBuffer.width = width; pixelBuffer.height = 2
        return !even
    }
}

internal class Robot72Mode(sampleRate: Int) : SstvMode {
    override val name = "Robot 72 Color"
    override val visCode = 12
    override val width = 320
    override val height = 240
    override val firstSyncPulseIndex = 0
    override val scanLineSamples: Int
    override val firstPixelSampleIndex: Int

    private val lumSamples: Int
    private val chromSamples: Int
    private val yBegin: Int
    private val vBegin: Int
    private val uBegin: Int
    private val end: Int
    private val ema: Ema

    init {
        val syncPorch = 0.003
        val lum = 0.138
        val sep = 0.0045
        val porch = 0.0015
        val chrom = 0.069
        scanLineSamples = round((0.009 + syncPorch + lum + 2 * (sep + porch + chrom)) * sampleRate).toInt()
        lumSamples = round(lum * sampleRate).toInt(); chromSamples = round(chrom * sampleRate).toInt()
        yBegin = round(syncPorch * sampleRate).toInt(); firstPixelSampleIndex = yBegin
        vBegin = round((syncPorch + lum + sep + porch) * sampleRate).toInt()
        uBegin = round((syncPorch + lum + sep + porch + chrom + sep + porch) * sampleRate).toInt()
        end = round((syncPorch + lum + 2 * (sep + porch + chrom)) * sampleRate).toInt()
        ema = Ema.withCutoff(width.toDouble(), (2 * lumSamples).toDouble(), 2)
    }

    override fun decodeScanLine(
        pixelBuffer: PixelBuffer,
        scratch: FloatArray,
        scanLine: FloatArray,
        scopeWidth: Int,
        syncPulseIndex: Int,
        lineSamples: Int,
        freqOffset: Float
    ): Boolean {
        if (syncPulseIndex + firstPixelSampleIndex < 0 || syncPulseIndex + end > scanLine.size) return false
        ema.reset()
        for (i in firstPixelSampleIndex until end) scratch[i] = ema.process(scanLine[syncPulseIndex + i])
        ema.reset()
        for (i in end - 1 downTo firstPixelSampleIndex) scratch[i] = freqToLevel(ema.process(scratch[i]), freqOffset)
        for (i in 0 until width) {
            val yP = yBegin + (i * lumSamples) / width
            val uP = uBegin + (i * chromSamples) / width
            val vP = vBegin + (i * chromSamples) / width
            pixelBuffer.pixels[i] = ColorConverter.yuv2rgb(scratch[yP], scratch[uP], scratch[vP])
        }
        pixelBuffer.width = width; pixelBuffer.height = 1
        return true
    }
}

internal class PdMode(
    variant: String,
    override val visCode: Int,
    private val hPixels: Int,
    private val vPixels: Int,
    channelSec: Double,
    sampleRate: Int
) : SstvMode {
    override val name = "PD $variant"
    override val width get() = hPixels
    override val height get() = vPixels
    override val firstSyncPulseIndex = 0
    override val scanLineSamples: Int
    override val firstPixelSampleIndex: Int

    private val chSamples: Int
    private val yEvenBegin: Int
    private val vAvgBegin: Int
    private val uAvgBegin: Int
    private val yOddBegin: Int
    private val end: Int
    private val ema: Ema

    init {
        val syncPorch = 0.00208
        scanLineSamples = round((0.02 + syncPorch + 4 * channelSec) * sampleRate).toInt()
        chSamples = round(channelSec * sampleRate).toInt()
        yEvenBegin = round(syncPorch * sampleRate).toInt(); firstPixelSampleIndex = yEvenBegin
        vAvgBegin = round((syncPorch + channelSec) * sampleRate).toInt()
        uAvgBegin = round((syncPorch + 2 * channelSec) * sampleRate).toInt()
        yOddBegin = round((syncPorch + 3 * channelSec) * sampleRate).toInt()
        end = round((syncPorch + 4 * channelSec) * sampleRate).toInt()
        ema = Ema.withCutoff(hPixels.toDouble(), (2 * chSamples).toDouble(), 2)
    }

    override fun decodeScanLine(
        pixelBuffer: PixelBuffer,
        scratch: FloatArray,
        scanLine: FloatArray,
        scopeWidth: Int,
        syncPulseIndex: Int,
        lineSamples: Int,
        freqOffset: Float
    ): Boolean {
        if (syncPulseIndex + firstPixelSampleIndex < 0 || syncPulseIndex + end > scanLine.size) return false
        ema.reset()
        for (i in firstPixelSampleIndex until end) scratch[i] = ema.process(scanLine[syncPulseIndex + i])
        ema.reset()
        for (i in end - 1 downTo firstPixelSampleIndex) scratch[i] = freqToLevel(ema.process(scratch[i]), freqOffset)
        for (i in 0 until hPixels) {
            val pos = (i * chSamples) / hPixels
            pixelBuffer.pixels[i] =
                ColorConverter.yuv2rgb(scratch[pos + yEvenBegin], scratch[pos + uAvgBegin], scratch[pos + vAvgBegin])
            pixelBuffer.pixels[i + hPixels] =
                ColorConverter.yuv2rgb(scratch[pos + yOddBegin], scratch[pos + uAvgBegin], scratch[pos + vAvgBegin])
        }
        pixelBuffer.width = hPixels; pixelBuffer.height = 2
        return true
    }
}

internal class RawMode(override val name: String, sampleRate: Int) : SstvMode {
    override val visCode = -1
    override val width = -1
    override val height = -1
    override val firstPixelSampleIndex = 0
    override val firstSyncPulseIndex = -1
    override val scanLineSamples = -1

    private val smallMax = round(0.125 * sampleRate).toInt()
    private val medMax = round(0.175 * sampleRate).toInt()
    private val ema = Ema()

    override fun decodeScanLine(
        pixelBuffer: PixelBuffer,
        scratch: FloatArray,
        scanLine: FloatArray,
        scopeWidth: Int,
        syncPulseIndex: Int,
        lineSamples: Int,
        freqOffset: Float
    ): Boolean {
        if (syncPulseIndex < 0 || syncPulseIndex + lineSamples > scanLine.size) return false
        var px = scopeWidth
        if (lineSamples < smallMax) px /= 2
        if (lineSamples < medMax) px /= 2
        ema.setCutoff(px.toDouble(), (2 * lineSamples).toDouble(), 2); ema.reset()
        for (i in 0 until lineSamples) scratch[i] = ema.process(scanLine[syncPulseIndex + i])
        ema.reset()
        for (i in lineSamples - 1 downTo 0) scratch[i] = freqToLevel(ema.process(scratch[i]), freqOffset)
        for (i in 0 until px) pixelBuffer.pixels[i] = ColorConverter.gray(scratch[(i * lineSamples) / px])
        pixelBuffer.width = px; pixelBuffer.height = 1
        return true
    }
}

private fun freqToLevel(frequency: Float, offset: Float): Float = 0.5f * (frequency - offset + 1f)
