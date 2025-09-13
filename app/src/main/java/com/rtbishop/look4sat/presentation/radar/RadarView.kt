@file:OptIn(ExperimentalTextApi::class)

package com.rtbishop.look4sat.presentation.radar

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.sp
import com.rtbishop.look4sat.domain.predict.PI_2
import com.rtbishop.look4sat.domain.predict.SatPos
import com.rtbishop.look4sat.domain.utility.toRadians
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadarViewCompose(item: SatPos, items: List<SatPos>, azimElev: Pair<Float, Float>) {
    val radarColor = Color(0xFFDCDCDC)
    val primaryColor = Color(0xFFFFE082)
    val secondaryColor = Color(0xFFDC0000)
    val strokeWidth = 4f
    val animTransition = rememberInfiniteTransition()
    val animSpec = infiniteRepeatable<Float>(tween(1000))
    val animScale = animTransition.animateFloat(16f, 64f, animSpec)
    val measurer = rememberTextMeasurer()
    val sweepDegrees = remember { mutableStateOf(0f) }
    val trackCreated = remember { mutableStateOf(false) }
    val trackPath = remember { mutableStateOf(Path()) }
    val trackEffect = remember { mutableStateOf(PathEffect.cornerPathEffect(0f)) }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val radius = (size.minDimension / 2f) * 0.95f
        if (!trackCreated.value) {
            trackPath.value = createTrackPath(items, radius)
            trackEffect.value = createTrackEffect(trackPath.value)
            trackCreated.value = true
        }
        rotate(-azimElev.first) {
            drawSweep(center, sweepDegrees.value, radius, primaryColor)
            drawRadar(radius, radarColor, strokeWidth, 3)
            drawInfo(radius, primaryColor, measurer, 3)
            translate(center.x, center.y) {
                drawTrack(trackPath.value, trackEffect.value, secondaryColor, primaryColor)
                if (item.elevation > 0) drawPosition(item, radius, animScale.value, primaryColor)
                drawAim(azimElev.first, azimElev.second, radius, strokeWidth, secondaryColor)
            }
            sweepDegrees.value = (sweepDegrees.value + 360 / 12.0f / 60) % 360
        }
    }
}

private fun DrawScope.drawRadar(radius: Float, color: Color, width: Float, circles: Int) {
    for (i in 0 until circles) {
        val circleRadius = radius - radius / circles.toFloat() * i.toFloat()
        drawCircle(color, circleRadius, style = Stroke(width))
    }
    drawLine(color, center.copy(x = center.x - radius), center.copy(x = center.x + radius), width)
    drawLine(color, center.copy(y = center.y - radius), center.copy(y = center.y + radius), width)
}

private fun DrawScope.drawInfo(radius: Float, color: Color, measurer: TextMeasurer, circles: Int) {
    for (i in 0 until circles) {
        val textY = (radius - radius / circles * i) - 32f
        val textDeg = " ${(90 / circles) * (circles - i)}°"
        drawText(measurer, textDeg, center.copy(y = textY), style = TextStyle(color, 15.sp))
    }
}

private fun DrawScope.drawTrack(path: Path, effect: PathEffect, color: Color, effectColor: Color) {
    drawPath(path, color, style = Stroke(4f))
    drawPath(path, effectColor, style = Stroke(pathEffect = effect))
}

private fun DrawScope.drawPosition(item: SatPos, radius: Float, posRadius: Float, color: Color) {
    val satX = sph2CartX(item.azimuth, item.elevation, radius.toDouble())
    val satY = sph2CartY(item.azimuth, item.elevation, radius.toDouble())
    drawCircle(color, 16f, center.copy(satX, -satY))
    drawCircle(color.copy(alpha = 1 - (posRadius / 64f)), posRadius, center.copy(satX, -satY))
}

private fun DrawScope.drawAim(azim: Float, elev: Float, radius: Float, width: Float, color: Color) {
    val size = 36f
    val azimRadians = azim.toDouble().toRadians()
    val tempElevRadians = elev.toDouble().toRadians()
    val elevRadians = if (tempElevRadians > 0.0) 0.0 else tempElevRadians
    val aimX = sph2CartX(azimRadians, -elevRadians, radius.toDouble())
    val aimY = sph2CartY(azimRadians, -elevRadians, radius.toDouble())
    try {
        drawLine(color, center.copy(aimX - size, -aimY), center.copy(aimX + size, -aimY), width)
        drawLine(color, center.copy(aimX, -aimY - size), center.copy(aimX, -aimY + size), width)
        drawCircle(color, size / 2, center.copy(aimX, -aimY), style = Stroke(width))
    } catch (exception: Exception) {
//        Timber.d(exception)
    }
}

private fun DrawScope.drawSweep(center: Offset, degrees: Float, radius: Float, color: Color) {
    val colors = listOf(Color.Transparent, color.copy(alpha = 0.5f), color)
    val colorStops = listOf(0.64f, 0.995f, 1f)
    val brush = ShaderBrush(SweepGradientShader(center, colors, colorStops))
    rotate(-90 + degrees, center) { drawCircle(brush, radius, style = Fill) }
}

private fun createTrackPath(positions: List<SatPos>, radius: Float): Path {
    val trackPath = Path()
    positions.forEachIndexed { index, satPos ->
        val passX = sph2CartX(satPos.azimuth, satPos.elevation, radius.toDouble())
        val passY = sph2CartY(satPos.azimuth, satPos.elevation, radius.toDouble())
        if (index == 0) trackPath.moveTo(passX, -passY) else trackPath.lineTo(passX, -passY)
    }
    return trackPath
}

private fun createTrackEffect(trackPath: Path): PathEffect {
    val shape = Path()
    val shapeRadius = 16f
    val angle = 120.0.toRadians()
    shape.moveTo((shapeRadius * cos(angle)).toFloat(), (shapeRadius * sin(angle)).toFloat())
    for (i in 1 until 3) {
        val x = (shapeRadius * cos(angle - angle * i)).toFloat()
        val y = (shapeRadius * sin(angle - angle * i)).toFloat()
        shape.lineTo(x, y)
    }
    shape.close()
    val trackLength = PathMeasure().apply { setPath(trackPath, false) }.length
    val advance = trackLength / 2f
    val phase = trackLength / 4f
    return PathEffect.stampedPathEffect(shape, advance, phase, StampedPathEffectStyle.Rotate)
}

private fun sph2CartX(azim: Double, elev: Double, r: Double): Float {
    val radius = r * (PI_2 - elev) / PI_2
    return (radius * cos(PI_2 - azim)).toFloat()
}

private fun sph2CartY(azim: Double, elev: Double, r: Double): Float {
    val radius = r * (PI_2 - elev) / PI_2
    return (radius * sin(PI_2 - azim)).toFloat()
}
