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
import android.graphics.LinearGradient
import android.graphics.RadialGradient
import android.graphics.Shader
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.core.graphics.withRotation
import kotlinx.coroutines.isActive
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

@Immutable
data class BubblesStyle(
    val bgColor: Color = Color.Black,
    val bubbleCount: Int = 16,
    val minBubbleCount: Int = 8,
    val maxBubbleCount: Int = 16,
    val adaptiveBubbleCount: Boolean = true,
    val spawnIntervalMs: Long = 800L,
    val bubbleRadiusFraction: Float = 0.24f,
    val adaptiveSizing: Boolean = true,
    val referenceMinSizePx: Float = 360f,
    val referenceAreaPx: Float = 360f * 800f,
    val minBubbleRadiusPx: Float = 16f,
    val maxBubbleRadiusPx: Float = 256f,
    val speedScale: Float = 0.99f,
    val minVelocity: Float = 0.8f,
    val maxVelocity: Float = 3.2f,
    val hueRotationSpeedDps: Float = 60f, // degrees per second
)

@Composable
fun BubblesEffect(
    modifier: Modifier = Modifier,
    isRunning: Boolean = true,
    style: BubblesStyle = BubblesStyle(),
) {
    val renderer = remember { BubblesRenderer() }
    var frameSignal by remember { mutableIntStateOf(0) }

    // Animation loop
    LaunchedEffect(isRunning, style) {
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
        renderer.ensureLayout(size)
        renderer.draw(this, style)
    }
}

private const val MAX_STEP_NANOS = 16_666_667L // ~60 FPS
private const val NANOS_TO_SECONDS = 1_000_000_000f
private val SHELL_GRADIENT_STOPS = floatArrayOf(0f, 0.52f, 0.66f, 0.79f, 0.90f, 0.968f, 0.993f, 1f)
private val INNER_GRADIENT_STOPS = floatArrayOf(0f, 0.34f, 0.68f, 1f)

private data class Velocity(var x: Float, var y: Float)

private data class Bubble(
    var x: Float,
    var y: Float,
    val radius: Float,
    val velocity: Velocity,
    val baseHue: Float, // 0-360, unique for each bubble
)

private class BubblesRenderer {
    private var width = 0f
    private var height = 0f
    private val bubbles = mutableListOf<Bubble>()
    private val random = Random(System.currentTimeMillis())
    private var globalHueRotation = 0f // degrees, rotates all bubbles hues
    private var spawnAccumulatorSec = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shellColors = IntArray(8)
    private val innerColors = IntArray(4)

    fun ensureLayout(size: Size) {
        val targetWidth = size.width
        val targetHeight = size.height
        if (targetWidth <= 0f || targetHeight <= 0f) return
        val shouldRebuild = width != targetWidth || height != targetHeight
        if (shouldRebuild) {
            width = targetWidth
            height = targetHeight
            bubbles.clear()
            spawnAccumulatorSec = 0f
        }
    }

    fun spawnBubble(style: BubblesStyle): Boolean {
        if (width <= 0f || height <= 0f) return false
        val radius = resolveBubbleRadius(style)
        val (velocityMin, velocityMax) = resolveVelocityRange(style)
        val bubble = Bubble(
            x = radius,
            y = height - radius,
            radius = radius,
            velocity = Velocity(
                x = randomInRange(velocityMin, velocityMax),
                y = -randomInRange(velocityMin, velocityMax),
            ),
            baseHue = random.nextFloat() * 360f, // Random starting hue for this bubble
        )
        bubbles.add(bubble)
        return true
    }

    fun update(deltaSeconds: Float, style: BubblesStyle) {
        if (width <= 0f || height <= 0f) return

        spawnMissingBubbles(deltaSeconds, style)

        // Rotate hue for all bubbles
        globalHueRotation += style.hueRotationSpeedDps * deltaSeconds
        if (globalHueRotation >= 360f) globalHueRotation -= 360f
        val frameScale = deltaSeconds * 60f
        for (i in bubbles.indices) {
            val bubble = bubbles[i]
            // Update position
            bubble.x += bubble.velocity.x * frameScale
            bubble.y += bubble.velocity.y * frameScale
            // Bounce off walls
            if (bubble.x > width - bubble.radius) {
                bubble.x = width - bubble.radius
                bubble.velocity.x *= -1
            }
            if (bubble.x < bubble.radius) {
                bubble.x = bubble.radius
                bubble.velocity.x *= -1
            }
            if (bubble.y > height - bubble.radius) {
                bubble.y = height - bubble.radius
                bubble.velocity.y *= -1
            }
            if (bubble.y < bubble.radius) {
                bubble.y = bubble.radius
                bubble.velocity.y *= -1
            }
        }
        // Collision detection and resolution
        for (i in bubbles.indices) {
            for (j in (i + 1) until bubbles.size) {
                val b1 = bubbles[i]
                val b2 = bubbles[j]
                if (isCollided(b1, b2)) resolveCollision(b1, b2)
            }
        }
    }

    private fun spawnMissingBubbles(deltaSeconds: Float, style: BubblesStyle) {
        val targetCount = resolveTargetBubbleCount(style)
        if (bubbles.size >= targetCount) return

        val spawnIntervalSec = style.spawnIntervalMs.coerceAtLeast(1L) / 1_000f
        spawnAccumulatorSec += deltaSeconds

        while (bubbles.size < targetCount && spawnAccumulatorSec >= spawnIntervalSec) {
            if (!spawnBubble(style)) break
            spawnAccumulatorSec -= spawnIntervalSec
        }
    }

    fun draw(scope: DrawScope, style: BubblesStyle) {
        if (width <= 0f || height <= 0f) return
        scope.drawRect(style.bgColor)
        val canvas = scope.drawContext.canvas.nativeCanvas
        for (i in bubbles.indices) {
            drawBubbleWithGradient(canvas, bubbles[i])
        }
    }

    private fun drawBubbleWithGradient(canvas: android.graphics.Canvas, bubble: Bubble) {
        val hue = (bubble.baseHue + globalHueRotation) % 360f
        val lit = 53.33f + maxOf(0f, (70f - kotlin.math.abs(244f - hue)) / 4f)
        val rgb = hueToRgb(hue, lit)
        val shellColor = mixWithWhite(rgb, 0.04f)
        val innerColor = mixWithWhite(rgb, 0.10f)
        val shimmerColor = mixWithWhite(rgb, 0.35f)
        // Outer shell
        val shellGradient = RadialGradient(
            bubble.x, bubble.y, bubble.radius,
            buildShellColors(shellColor),
            SHELL_GRADIENT_STOPS,
            Shader.TileMode.CLAMP
        )
        paint.style = Paint.Style.FILL
        paint.shader = shellGradient
        canvas.drawCircle(bubble.x, bubble.y, bubble.radius, paint)
        // Subtle thin rim
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = maxOf(1f, bubble.radius * 0.024f)
        paint.color = colorWithAlpha(mixWithWhite(shellColor, 0.08f), 0.30f)
        canvas.drawCircle(bubble.x, bubble.y, bubble.radius - paint.strokeWidth * 0.5f, paint)
        // Top internal bubble
        val innerTop = bubble.y - bubble.radius * 0.96f
        val innerBottom = bubble.y + bubble.radius * 0.48f
        val innerLeft = bubble.x - bubble.radius * 0.84f
        val innerRight = bubble.x + bubble.radius * 0.84f
        val innerGradient = LinearGradient(
            bubble.x,
            innerTop,
            bubble.x,
            innerBottom,
            buildInnerColors(innerColor),
            INNER_GRADIENT_STOPS,
            Shader.TileMode.CLAMP
        )
        paint.style = Paint.Style.FILL
        paint.shader = innerGradient
        canvas.drawOval(
            innerLeft,
            innerTop,
            innerRight,
            innerBottom,
            paint
        )
        // Top-left shimmer
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.color = colorWithAlpha(shimmerColor, 0.95f)
        val shimmerCx = bubble.x - bubble.radius * 0.40f
        val shimmerCy = bubble.y - bubble.radius * 0.72f
        val shimmerHalfWidth = bubble.radius * 0.06f
        val shimmerHalfHeight = bubble.radius * 0.21f
        canvas.withRotation(60f, shimmerCx, shimmerCy) {
            drawOval(
                shimmerCx - shimmerHalfWidth,
                shimmerCy - shimmerHalfHeight,
                shimmerCx + shimmerHalfWidth,
                shimmerCy + shimmerHalfHeight,
                paint
            )
        }
    }

    private fun isCollided(bubble1: Bubble, bubble2: Bubble): Boolean {
        val dx = bubble1.x - bubble2.x
        val dy = bubble1.y - bubble2.y
        val radius = (bubble1.radius + bubble2.radius) * 0.9f
        return dx * dx + dy * dy < radius * radius
    }

    private fun resolveCollision(particle: Bubble, otherParticle: Bubble) {
        val xVelocityDiff = particle.velocity.x - otherParticle.velocity.x
        val yVelocityDiff = particle.velocity.y - otherParticle.velocity.y
        val xDist = otherParticle.x - particle.x
        val yDist = otherParticle.y - particle.y
        // Prevent accidental overlap
        if (xVelocityDiff * xDist + yVelocityDiff * yDist >= 0) {
            val angle = -atan2(otherParticle.y - particle.y, otherParticle.x - particle.x)
            val m1 = 1f
            val m2 = 1f
            val u1 = rotate(particle.velocity, angle)
            val u2 = rotate(otherParticle.velocity, angle)
            val v1 = Velocity(
                x = (u1.x * (m1 - m2)) / (m1 + m2) + (u2.x * 2 * m2) / (m1 + m2),
                y = u1.y,
            )
            val v2 = Velocity(
                x = (u2.x * (m1 - m2)) / (m1 + m2) + (u1.x * 2 * m2) / (m1 + m2),
                y = u2.y,
            )
            val vFinal1 = rotate(v1, -angle)
            val vFinal2 = rotate(v2, -angle)
            particle.velocity.x = vFinal1.x
            particle.velocity.y = vFinal1.y
            otherParticle.velocity.x = vFinal2.x
            otherParticle.velocity.y = vFinal2.y
        }
    }

    private fun rotate(velocity: Velocity, angle: Float): Velocity {
        return Velocity(
            x = velocity.x * cos(angle) - velocity.y * sin(angle),
            y = velocity.x * sin(angle) + velocity.y * cos(angle),
        )
    }

    private fun buildShellColors(shellColor: Int): IntArray {
        shellColors[0] = colorWithAlpha(shellColor, 0f)
        shellColors[1] = colorWithAlpha(shellColor, 0f)
        shellColors[2] = colorWithAlpha(shellColor, 0.04f)
        shellColors[3] = colorWithAlpha(shellColor, 0.12f)
        shellColors[4] = colorWithAlpha(shellColor, 0.28f)
        shellColors[5] = colorWithAlpha(shellColor, 0.44f)
        shellColors[6] = colorWithAlpha(shellColor, 0.58f)
        shellColors[7] = colorWithAlpha(shellColor, 0.64f)
        return shellColors
    }

    private fun buildInnerColors(innerColor: Int): IntArray {
        innerColors[0] = colorWithAlpha(innerColor, 0.48f)
        innerColors[1] = colorWithAlpha(innerColor, 0.36f)
        innerColors[2] = colorWithAlpha(innerColor, 0.08f)
        innerColors[3] = colorWithAlpha(innerColor, 0f)
        return innerColors
    }

    private fun resolveBubbleRadius(style: BubblesStyle): Float {
        val minDimension = min(width, height)
        val baseRadius = minDimension * style.bubbleRadiusFraction
        val minRadius = style.minBubbleRadiusPx.coerceAtLeast(1f)
        val maxRadius = maxOf(minRadius, style.maxBubbleRadiusPx)
        if (!style.adaptiveSizing || minDimension <= 0f) {
            return baseRadius.coerceIn(minRadius, maxRadius)
        }

        val reference = style.referenceMinSizePx.coerceAtLeast(1f)
        val dampening = sqrt((reference / minDimension).coerceAtMost(1f))
        return (baseRadius * dampening).coerceIn(minRadius, maxRadius)
    }

    private fun resolveTargetBubbleCount(style: BubblesStyle): Int {
        val baseCount = style.bubbleCount.coerceAtLeast(1)
        if (!style.adaptiveBubbleCount) return baseCount

        val area = width * height
        val referenceArea = style.referenceAreaPx.coerceAtLeast(1f)
        val areaScale = sqrt((area / referenceArea).coerceAtLeast(0.25f))
        val scaledCount = (baseCount * areaScale).toInt()
        val minCount = style.minBubbleCount.coerceAtLeast(1)
        val maxCount = maxOf(minCount, style.maxBubbleCount.coerceAtLeast(1))
        return scaledCount.coerceIn(minCount, maxCount)
    }

    private fun resolveVelocityRange(style: BubblesStyle): Pair<Float, Float> {
        val referenceMaxVelocity = maxOf(height / 300f, 1f)
        val velocityMin = (style.minVelocity * style.speedScale).coerceAtLeast(0.05f)
        val velocityMax = maxOf(velocityMin, min(style.maxVelocity, referenceMaxVelocity) * style.speedScale)
        return velocityMin to velocityMax
    }

    private fun randomInRange(min: Float, max: Float): Float {
        return random.nextFloat() * (max - min) + min
    }
}

private fun hueToRgb(h: Float, l: Float): Int {
    val hNorm = h / 360f
    val lNorm = l / 100f
    val sNorm = 1f
    val c = (1f - kotlin.math.abs(2f * lNorm - 1f)) * sNorm
    val hp = hNorm * 6f
    val x = c * (1f - kotlin.math.abs((hp % 2f) - 1f))
    val m = lNorm - c / 2f
    val (r, g, b) = when {
        hp < 1f -> Triple(c, x, 0f)
        hp < 2f -> Triple(x, c, 0f)
        hp < 3f -> Triple(0f, c, x)
        hp < 4f -> Triple(0f, x, c)
        hp < 5f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    val r8 = ((r + m) * 255).toInt().coerceIn(0, 255)
    val g8 = ((g + m) * 255).toInt().coerceIn(0, 255)
    val b8 = ((b + m) * 255).toInt().coerceIn(0, 255)
    return (0xFF shl 24) or (r8 shl 16) or (g8 shl 8) or b8
}

private fun colorWithAlpha(color: Int, alpha: Float): Int {
    val a = (alpha * 255).toInt().coerceIn(0, 255)
    val r = (color shr 16) and 0xFF
    val g = (color shr 8) and 0xFF
    val b = color and 0xFF
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}

private fun mixWithWhite(color: Int, amount: Float): Int {
    val t = amount.coerceIn(0f, 1f)
    val r = (color shr 16) and 0xFF
    val g = (color shr 8) and 0xFF
    val b = color and 0xFF
    val mixedR = (r + (255 - r) * t).toInt().coerceIn(0, 255)
    val mixedG = (g + (255 - g) * t).toInt().coerceIn(0, 255)
    val mixedB = (b + (255 - b) * t).toInt().coerceIn(0, 255)
    return (0xFF shl 24) or (mixedR shl 16) or (mixedG shl 8) or mixedB
}
