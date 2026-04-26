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

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StampedPathEffectStyle
import androidx.compose.ui.graphics.SweepGradientShader
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.rtbishop.look4sat.core.domain.predict.CelestialComputer
import com.rtbishop.look4sat.core.domain.predict.OrbitalPos
import com.rtbishop.look4sat.core.domain.predict.PI_2
import com.rtbishop.look4sat.core.domain.utility.toRadians
import com.rtbishop.look4sat.core.presentation.R
import kotlin.math.cos
import kotlin.math.sin

private const val CIRCLES = 3
private const val STROKE_WIDTH = 6f
private const val SWEEP_INCREMENT = 360f / 12f / 60f

@Composable
fun RadarViewCompose(
    item: OrbitalPos,
    items: List<OrbitalPos>,
    azimElev: Pair<Float, Float>,
    shouldShowSweep: Boolean,
    shouldUseCompass: Boolean,
    modifier: Modifier = Modifier,
    sunPosition: CelestialComputer.SunPosition? = null,
    moonPosition: CelestialComputer.MoonPosition? = null,
) {
    val aimColor = MaterialTheme.colorScheme.error
    val primaryColor = MaterialTheme.colorScheme.primary
    val radarColor = MaterialTheme.colorScheme.secondary
    val sunColor = MaterialTheme.colorScheme.primary
    val animTransition = rememberInfiniteTransition(label = "animScale")
    val animScale by animTransition.animateFloat(
        initialValue = 16f,
        targetValue = 64f,
        animationSpec = infiniteRepeatable(tween(1000)),
        label = "animScale"
    )
    val measurer = rememberTextMeasurer()
    val sunPainter = painterResource(R.drawable.ic_sun)
    val moonPainter = painterResource(R.drawable.ic_moon)
    var sweepDegrees by remember { mutableFloatStateOf(0f) }
    var cachedRadius by remember { mutableFloatStateOf(0f) }
    var trackPath by remember { mutableStateOf(Path()) }
    var trackEffect by remember { mutableStateOf(PathEffect.cornerPathEffect(0f)) }

    Canvas(modifier = modifier.aspectRatio(1f)) {
        val radius = size.minDimension / 2f * 0.95f
        if (radius != cachedRadius) {
            trackPath = createTrackPath(items, radius)
            trackEffect = createTrackEffect(trackPath)
            cachedRadius = radius
        }
        rotate(if (shouldUseCompass) -azimElev.first else 0f) {
            if (shouldShowSweep) drawSweep(center, sweepDegrees, radius, primaryColor)
            drawRadar(radius, radarColor)
            drawElevationLabels(radius, primaryColor, measurer)
            translate(center.x, center.y) {
                drawTrack(trackPath, trackEffect, aimColor, primaryColor)
                if (item.elevation > 0) {
                    drawPosition(item, radius, animScale, primaryColor)
                }
                sunPosition?.let { sun ->
                    if (sun.elevation > 0) drawBodyIcon(sun.azimuth, sun.elevation, radius, sunColor, sunPainter, 52f)
                }
                moonPosition?.let { moon ->
                    if (moon.elevation > 0) drawBodyIcon(
                        moon.azimuth,
                        moon.elevation,
                        radius,
                        radarColor,
                        moonPainter,
                        52f
                    )
                }
                if (shouldUseCompass) drawAim(azimElev.first, azimElev.second, radius, aimColor)
            }
            sweepDegrees = (sweepDegrees + SWEEP_INCREMENT) % 360f
        }
    }
}

private fun DrawScope.drawRadar(radius: Float, color: Color) {
    val step = radius / CIRCLES
    for (i in 0 until CIRCLES) {
        drawCircle(color, radius - step * i, style = Stroke(STROKE_WIDTH))
    }
    drawLine(color, Offset(center.x - radius, center.y), Offset(center.x + radius, center.y), STROKE_WIDTH)
    drawLine(color, Offset(center.x, center.y - radius), Offset(center.x, center.y + radius), STROKE_WIDTH)
}

private fun DrawScope.drawElevationLabels(radius: Float, color: Color, measurer: TextMeasurer) {
    val step = radius / CIRCLES
    val degStep = 90 / CIRCLES
    val style = TextStyle(color, 15.sp)
    for (i in 0 until CIRCLES) {
        val textY = (radius - step * i) - 32f
        drawText(measurer, " ${degStep * (CIRCLES - i)}°", Offset(center.x, textY), style = style)
    }
}

private fun DrawScope.drawTrack(path: Path, effect: PathEffect, color: Color, effectColor: Color) {
    drawPath(path, color, style = Stroke(STROKE_WIDTH))
    drawPath(path, effectColor, style = Stroke(pathEffect = effect))
}

private fun DrawScope.drawPosition(item: OrbitalPos, radius: Float, posRadius: Float, color: Color) {
    val pos = sph2Cart(item.azimuth, item.elevation, radius.toDouble())
    drawCircle(color, 16f, pos)
    if (!item.eclipsed) drawCircle(color.copy(alpha = 1 - (posRadius / 64f)), posRadius, pos)
}

private fun DrawScope.drawAim(azim: Float, elev: Float, radius: Float, color: Color) {
    val size = 36f
    val azimRad = azim.toDouble().toRadians()
    val elevRad = elev.toDouble().toRadians().coerceAtMost(0.0)
    val pos = sph2Cart(azimRad, -elevRad, radius.toDouble())
    drawLine(color, Offset(pos.x - size, pos.y), Offset(pos.x + size, pos.y), STROKE_WIDTH)
    drawLine(color, Offset(pos.x, pos.y - size), Offset(pos.x, pos.y + size), STROKE_WIDTH)
    drawCircle(color, size / 2, pos, style = Stroke(STROKE_WIDTH))
}

private fun DrawScope.drawSweep(center: Offset, degrees: Float, radius: Float, color: Color) {
    val colors = listOf(Color.Transparent, color.copy(alpha = 0.5f), color)
    val colorStops = listOf(0.64f, 0.995f, 1f)
    val brush = ShaderBrush(SweepGradientShader(center, colors, colorStops))
    rotate(-90 + degrees, center) { drawCircle(brush, radius, style = Fill) }
}

private fun createTrackPath(positions: List<OrbitalPos>, radius: Float): Path {
    val trackPath = Path()
    positions.forEachIndexed { index, pos ->
        val offset = sph2Cart(pos.azimuth, pos.elevation, radius.toDouble())
        if (index == 0) trackPath.moveTo(offset.x, offset.y) else trackPath.lineTo(offset.x, offset.y)
    }
    return trackPath
}

private fun createTrackEffect(trackPath: Path): PathEffect {
    val shapeRadius = 24f
    val angle = 120.0.toRadians()
    val shape = Path().apply {
        moveTo((shapeRadius * cos(angle)).toFloat(), (shapeRadius * sin(angle)).toFloat())
        for (i in 1 until 3) {
            lineTo(
                x = (shapeRadius * cos(angle - angle * i)).toFloat(),
                y = (shapeRadius * sin(angle - angle * i)).toFloat()
            )
        }
        close()
    }
    val trackLength = PathMeasure().apply { setPath(trackPath, false) }.length
    return PathEffect.stampedPathEffect(shape, trackLength / 2f, trackLength / 4f, StampedPathEffectStyle.Rotate)
}

private fun DrawScope.drawBodyIcon(
    azimDeg: Double,
    elevDeg: Double,
    radius: Float,
    color: Color,
    painter: Painter,
    iconSize: Float
) {
    val azimRad = azimDeg.toRadians()
    val elevRad = elevDeg.toRadians()
    val pos = sph2Cart(azimRad, elevRad, radius.toDouble())
    val half = iconSize / 2f
    withTransform({
        translate(pos.x - half, pos.y - half)
    }) {
        with(painter) {
            draw(Size(iconSize, iconSize), colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(color))
        }
    }
}

private fun sph2Cart(azim: Double, elev: Double, r: Double): Offset {
    val radius = r * (PI_2 - elev) / PI_2
    return Offset(
        x = (radius * cos(PI_2 - azim)).toFloat(),
        y = -(radius * sin(PI_2 - azim)).toFloat()
    )
}
