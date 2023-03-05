@file:OptIn(ExperimentalTextApi::class)

package com.rtbishop.look4sat.presentation.radarScreen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.sp
import com.rtbishop.look4sat.domain.predict.PI_2
import com.rtbishop.look4sat.domain.predict.SatPos
import com.rtbishop.look4sat.domain.predict.TWO_PI
import com.rtbishop.look4sat.utility.toRadians
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadarViewCompose(item: SatPos, items: List<SatPos>, orientation: Triple<Float, Float, Float>) {
    val measurer = rememberTextMeasurer()
    val newTrackPath = remember { mutableStateOf(Path()) }
    val newArrowPath = remember { mutableStateOf(Path()) }
    var isTrackCreated = false
    val yellowColor = Color(0xFFFFE082)
    val whiteColor = Color(0xCCFFFFFF)
    val redColor = Color(0xFFB71C1C)
    val strokeWidth = 4f
    Canvas(modifier = Modifier.fillMaxSize()) {
        val radius = (size.minDimension / 2f) * 0.95f
        if (items.isNotEmpty() && !isTrackCreated) {
            newTrackPath.value = createPassTrajectory(items, radius)
            newArrowPath.value = createPassTrajectoryArrow()
            isTrackCreated = true
        }
        rotate(-orientation.first) {
            drawRadarCirle(radius, whiteColor, strokeWidth)
            drawRadarCross(radius, whiteColor, strokeWidth)
            drawRadarText(radius, yellowColor, measurer)
            translate(center.x, center.y) {
                if (items.isNotEmpty()) {
                    val effect = createPathEffect(newTrackPath.value, newArrowPath.value)
                    drawPath(newTrackPath.value, redColor, style = Stroke(4f))
                    drawPath(newTrackPath.value, yellowColor, style = Stroke(pathEffect = effect))
                }
                drawSatellite(item, radius, yellowColor)
                drawCrosshair(orientation.first, orientation.second, radius, strokeWidth)
            }
        }
    }
}

private fun DrawScope.drawRadarCirle(radius: Float, color: Color, width: Float, circles: Int = 3) {
    for (i in 0 until circles) {
        val circleRadius = radius - radius / circles.toFloat() * i.toFloat()
        drawCircle(color, circleRadius, style = Stroke(width))
    }
}

private fun DrawScope.drawRadarCross(radius: Float, color: Color, width: Float) {
    drawLine(color, center, center.copy(x = center.x - radius), strokeWidth = width)
    drawLine(color, center, center.copy(x = center.x + radius), strokeWidth = width)
    drawLine(color, center, center.copy(y = center.y - radius), strokeWidth = width)
    drawLine(color, center, center.copy(y = center.y + radius), strokeWidth = width)
}

private fun DrawScope.drawRadarText(
    radius: Float, color: Color, measurer: TextMeasurer, circles: Int = 3
) {
    for (i in 0..circles) {
        val textY = (radius - radius / circles * i) + 25f
        val textDeg = " ${(90 / circles) * (circles - i)}°"
        drawText(measurer, textDeg, center.copy(y = textY), style = TextStyle(color, 16.sp))
    }
}

private fun DrawScope.drawSatellite(item: SatPos, radius: Float, color: Color) {
    if (item.elevation > 0) {
        val satX = sph2CartX(item.azimuth, item.elevation, radius.toDouble())
        val satY = sph2CartY(item.azimuth, item.elevation, radius.toDouble())
        drawCircle(color, 16f, center.copy(satX, -satY))
    }
}

private fun DrawScope.drawCrosshair(azimuth: Float, elevation: Float, radius: Float, width: Float) {
    val redColor = Color(0xFFFF0000)
    val size = 36f
    val azimuthRad = azimuth.toDouble().toRadians()
    val tmpElevation = elevation.toDouble().toRadians()
    val elevationRad = if (tmpElevation > 0.0) 0.0 else tmpElevation
    val crossX = sph2CartX(azimuthRad, -elevationRad, radius.toDouble())
    val crossY = sph2CartY(azimuthRad, -elevationRad, radius.toDouble())
    drawLine(
        redColor,
        center.copy(crossX - size, -crossY),
        center.copy(crossX + size, -crossY),
        strokeWidth = width
    )
    drawLine(
        redColor,
        center.copy(crossX, -crossY - size),
        center.copy(crossX, -crossY + size),
        strokeWidth = width
    )
    drawCircle(redColor, size / 2, center.copy(crossX, -crossY), style = Stroke(width))
}

private fun createPassTrajectory(positions: List<SatPos>, radarRadius: Float): Path {
    val trackPath = Path()
    positions.forEachIndexed { index, satPos ->
        val passX = sph2CartX(satPos.azimuth, satPos.elevation, radarRadius.toDouble())
        val passY = sph2CartY(satPos.azimuth, satPos.elevation, radarRadius.toDouble())
        if (index == 0) {
            trackPath.moveTo(passX, -passY)
        } else {
            trackPath.lineTo(passX, -passY)
        }
    }
    return trackPath
}

private fun createPassTrajectoryArrow(): Path {
    val arrowPath = Path()
    val radius = 24f
    val sides = 3
    val angle = TWO_PI / sides
    arrowPath.moveTo((radius * cos(angle)).toFloat(), (radius * sin(angle)).toFloat())
    for (i in 1 until sides) {
        val x = (radius * cos(angle - angle * i)).toFloat()
        val y = (radius * sin(angle - angle * i)).toFloat()
        arrowPath.lineTo(x, y)
    }
    arrowPath.close()
    return arrowPath
}

private fun createPathEffect(trackPath: Path, arrowPath: Path): PathEffect {
    val trackLength = PathMeasure().apply { setPath(trackPath, false) }.length
    val quarter = trackLength / 4f
    val center = trackLength / 2f
    return PathEffect.stampedPathEffect(arrowPath, center, quarter, StampedPathEffectStyle.Rotate)
}

private fun sph2CartX(azimuth: Double, elevation: Double, r: Double): Float {
    val radius = r * (PI_2 - elevation) / PI_2
    return (radius * cos(PI_2 - azimuth)).toFloat()
}

private fun sph2CartY(azimuth: Double, elevation: Double, r: Double): Float {
    val radius = r * (PI_2 - elevation) / PI_2
    return (radius * sin(PI_2 - azimuth)).toFloat()
}
