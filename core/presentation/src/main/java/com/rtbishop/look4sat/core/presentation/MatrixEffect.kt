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
package com.rtbishop.look4sat.core.presentation

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.isActive
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

@Immutable
data class MatrixStyle(
    val bgColor: Color = Color.Black,
    val bodyColor: Color = Color(0xFF29C94A),
    val headColor: Color = Color(0xFFC2FFC6),
    val tailAlphaFloor: Float = 0.14f, // 0.14f - 0.18f,
    val fontSize: TextUnit = 14.sp, // 11.sp - 14.sp
    val minStreamLength: Int = 8, // 6 - 8
    val maxStreamLength: Int = 28,  // 20 - 28
    val minSpeedRps: Float = 10f, // 10f - 15f
    val maxSpeedRps: Float = 38f, // 38f - 45f
    val resetPauseSec: ClosedFloatingPointRange<Float> = 0.1f..1.0f,
)

@Composable
fun MatrixEffect(
    modifier: Modifier = Modifier,
    isRunning: Boolean = true,
    style: MatrixStyle = MatrixStyle(),
    symbols: String = DEFAULT_SYMBOLS,
) {
    val density = LocalDensity.current
    val renderer = remember { MatrixRenderer() }
    var frameSignal by remember { mutableIntStateOf(0) }

    LaunchedEffect(isRunning, style, symbols) {
        if (!isRunning) return@LaunchedEffect

        var previousNanos = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (previousNanos == 0L) previousNanos = now
                val deltaSec = ((now - previousNanos).coerceAtMost(MAX_STEP_NANOS)).toFloat() / NANOS_TO_SECONDS
                previousNanos = now
                renderer.update(deltaSec, style)
                frameSignal++
            }
        }
    }

    Canvas(modifier = modifier) {
        frameSignal
        renderer.ensureLayout(size, density.density, style, symbols)
        renderer.draw(this, style)
    }
}

private const val MAX_STEP_NANOS = 33_333_333L
private const val NANOS_TO_SECONDS = 1_000_000_000f
private const val DEFAULT_SYMBOLS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789<>=*+-~:;/[]{}()"

private fun Color.toArgb(): Int {
    val a = (alpha.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
    val r = (red.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
    val g = (green.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
    val b = (blue.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}

private class MatrixRenderer {
    private var columns = 0
    private var rows = 0
    private var width = 0f
    private var height = 0f
    private var fontSizePx = 0f
    private var charWidth = 0f
    private var charHeight = 0f
    private var baselineOffset = 0f
    private var symbolSet = ""
    private var symbols = charArrayOf()

    private var glyphs = charArrayOf()
    private var streams = emptyArray<StreamState>()

    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.MONOSPACE }
    private val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.MONOSPACE }
    private val charBuffer = CharArray(1)
    private val random = Random(System.currentTimeMillis())

    fun ensureLayout(size: Size, density: Float, style: MatrixStyle, symbols: String) {
        val targetFontPx = style.fontSize.value * density
        val targetWidth = size.width
        val targetHeight = size.height
        val targetSymbols = symbols.ifBlank { DEFAULT_SYMBOLS }

        if (targetWidth <= 0f || targetHeight <= 0f) return

        val shouldRebuild = width != targetWidth ||
            height != targetHeight ||
            fontSizePx != targetFontPx ||
            symbolSet != targetSymbols

        if (!shouldRebuild) return

        width = targetWidth
        height = targetHeight
        fontSizePx = targetFontPx
        symbolSet = targetSymbols
        this.symbols = targetSymbols.toCharArray()

        bodyPaint.textSize = targetFontPx
        headPaint.textSize = targetFontPx
        charWidth = max(bodyPaint.measureText("W"), 1f)
        val metrics = bodyPaint.fontMetrics
        charHeight = max(metrics.descent - metrics.ascent, 1f)
        baselineOffset = -metrics.ascent

        columns = max((width / charWidth).toInt(), 1)
        rows = max((height / charHeight).toInt() + 2, 1)

        glyphs = CharArray(columns * rows) { randomGlyph() }
        streams = Array(columns) { StreamState.random(rows, style, random) }
    }

    fun update(deltaSeconds: Float, style: MatrixStyle) {
        if (columns == 0 || rows == 0 || deltaSeconds <= 0f) return

        for (column in streams.indices) {
            val stream = streams[column]
            stream.pauseSec -= deltaSeconds
            if (stream.pauseSec > 0f) continue

            val previousHead = floor(stream.headRow).toInt()
            stream.headRow += stream.speedRps * deltaSeconds
            val newHead = floor(stream.headRow).toInt()

            if (newHead > previousHead) {
                for (row in (previousHead + 1)..newHead) {
                    if (row in 0 until rows) {
                        glyphs[row * columns + column] = randomGlyph()
                    }
                }
            }

            if (newHead - stream.length > rows) {
                stream.reset(rows, style, random)
            }
        }
    }

    fun draw(scope: androidx.compose.ui.graphics.drawscope.DrawScope, style: MatrixStyle) {
        if (columns == 0 || rows == 0) return

        scope.drawRect(style.bgColor)

        bodyPaint.color = style.bodyColor.toArgb()
        headPaint.color = style.headColor.toArgb()
        val tailAlphaFloor = style.tailAlphaFloor.coerceIn(0f, 1f)

        val canvas = scope.drawContext.canvas.nativeCanvas
        for (column in streams.indices) {
            val stream = streams[column]
            if (stream.pauseSec > 0f) continue

            val head = floor(stream.headRow).toInt()
            val startRow = max(0, head - stream.length + 1)
            val endRow = min(rows - 1, head)
            if (startRow > endRow) continue

            for (row in endRow downTo startRow) {
                val tailIndex = head - row

                val paint = if (tailIndex == 0) headPaint else bodyPaint
                if (tailIndex != 0) {
                    val normalized = ((stream.length - tailIndex).toFloat() / stream.length).coerceIn(0f, 1f)
                    val alpha = tailAlphaFloor + (1f - tailAlphaFloor) * normalized
                    paint.alpha = (alpha * 255).toInt()
                } else {
                    paint.alpha = 255
                }

                val glyph = glyphs[row * columns + column]
                charBuffer[0] = glyph
                val x = column * charWidth
                val y = row * charHeight + baselineOffset
                canvas.drawText(charBuffer, 0, 1, x, y, paint)
            }
        }
    }

    private fun randomGlyph(): Char = symbols[random.nextInt(symbols.size)]
}

private data class StreamState(
    var headRow: Float,
    var length: Int,
    var speedRps: Float,
    var pauseSec: Float,
) {
    fun reset(rows: Int, style: MatrixStyle, random: Random) {
        val randomOffset = random.nextFloat() * rows
        headRow = -randomOffset
        length = random.nextInt(
            from = style.minStreamLength.coerceAtLeast(2),
            until = (style.maxStreamLength.coerceAtLeast(style.minStreamLength + 1) + 1),
        )
        speedRps = random.nextFloat() * (style.maxSpeedRps - style.minSpeedRps) + style.minSpeedRps
        pauseSec = random.nextFloat() * (style.resetPauseSec.endInclusive - style.resetPauseSec.start) +
            style.resetPauseSec.start
    }

    companion object {
        fun random(rows: Int, style: MatrixStyle, random: Random): StreamState {
            val length = random.nextInt(
                from = style.minStreamLength.coerceAtLeast(2),
                until = (style.maxStreamLength.coerceAtLeast(style.minStreamLength + 1) + 1),
            )
            return StreamState(
                headRow = -random.nextFloat() * rows,
                length = length,
                speedRps = random.nextFloat() * (style.maxSpeedRps - style.minSpeedRps) + style.minSpeedRps,
                pauseSec = random.nextFloat() * (style.resetPauseSec.endInclusive - style.resetPauseSec.start) +
                    style.resetPauseSec.start,
            )
        }
    }
}
