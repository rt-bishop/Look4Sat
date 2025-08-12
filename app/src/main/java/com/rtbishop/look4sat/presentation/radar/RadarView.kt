package com.rtbishop.look4sat.presentation.radar

import android.media.SoundPool
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.rtbishop.look4sat.domain.predict.OrbitalPos
import com.rtbishop.look4sat.domain.predict.PI_2
import com.rtbishop.look4sat.domain.utility.toRadians
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadarViewCompose(
    item: OrbitalPos,
    items: List<OrbitalPos>,
    azimElev: Pair<Float, Float>,
    shouldShowSweep: Boolean,
    shouldUseCompass: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val radarColor = MaterialTheme.colorScheme.secondary
    val trackColor = MaterialTheme.colorScheme.primary
    val aimColor = MaterialTheme.colorScheme.error
    val strokeWidth = 6f
    val animTransition = rememberInfiniteTransition(label = "animScale")
    val animSpec = infiniteRepeatable<Float>(tween(1000))
    val animScale = animTransition.animateFloat(16f, 64f, animSpec, label = "animScale")
    val aimThreshold = 0.05f
    val measurer = rememberTextMeasurer()
    val sweepDegrees = remember { mutableFloatStateOf(0f) }
    val trackCreated = remember { mutableStateOf(false) }
    val trackPath = remember { mutableStateOf(Path()) }
    val trackEffect = remember { mutableStateOf(PathEffect.cornerPathEffect(0f)) }
    val soundPool = remember { mutableStateOf<SoundPool?>(null) }
    val beepSoundId = remember { mutableIntStateOf(0) }
    val aimTargetDifference = remember { mutableFloatStateOf(0f) }

//    LaunchedEffect(item.azimuth, item.elevation, azimElev.first, azimElev.second) {
//        val aimAzimuthRadians = azimElev.first.toDouble().toRadians()
//        val aimElevationRadians = abs(min(azimElev.second, 0f)).toDouble().toRadians()
//        // radius of 0.5 makes the aimTargetDifference range 0.0 to 1.0
//        val radius = 0.5
//        val aimX = sph2CartX(aimAzimuthRadians, aimElevationRadians, radius)
//        val aimY = sph2CartY(aimAzimuthRadians, aimElevationRadians, radius)
//        val satX = sph2CartX(item.azimuth, item.elevation, radius)
//        val satY = sph2CartY(item.azimuth, item.elevation, radius)
//        aimTargetDifference.floatValue = sqrt((satX - aimX).pow(2) + (satY - aimY).pow(2))
//
//
//        val minPlaybackRate = 0.5f
//        val maxPlaybackRate = 2.0f
//        val playbackRate =
//            maxPlaybackRate - (aimTargetDifference.floatValue / (maxPlaybackRate - minPlaybackRate))
//
//        soundPool.value?.setRate(beepSoundId.intValue, playbackRate)
//    }
//
//    LaunchedEffect(aimTargetDifference.floatValue < aimThreshold) {
//        if (aimTargetDifference.floatValue < aimThreshold) {
//            view.performHapticFeedback(HapticFeedbackConstantsCompat.VIRTUAL_KEY)
//            soundPool.value?.pause(beepSoundId.intValue)
//        } else {
//            soundPool.value?.resume(beepSoundId.intValue)
//        }
//    }
//
//    LaunchedEffect(item.elevation > 0) {
//        if(item.elevation > 0) {
//            soundPool.value?.resume(beepSoundId.intValue)
//        } else {
//            soundPool.value?.pause(beepSoundId.intValue)
//        }
//    }
//
//    DisposableEffect(Unit) {
//        val audioAttributes = AudioAttributes.Builder()
//            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
//            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
//            .build()
//
//        soundPool.value = SoundPool.Builder()
//            .setMaxStreams(1)
//            .setAudioAttributes(audioAttributes)
//            .build()
//
//        beepSoundId.intValue = soundPool.value?.load(context, R.raw.beep, 1) ?: 0
//
//        soundPool.value?.setOnLoadCompleteListener { soundPool, _, status ->
//            if (status == 0) {
//                soundPool.play(beepSoundId.intValue, 0.5f, 0.5f, 0, -1, 1f)
//            }
//        }
//
//        onDispose {
//            soundPool.value?.release()
//        }
//    }

    Canvas(modifier = modifier.aspectRatio(1f)) {
        val radius = (size.minDimension / 2f) * 0.95f
        if (!trackCreated.value) {
            trackPath.value = createTrackPath(items, radius)
            trackEffect.value = createTrackEffect(trackPath.value)
            trackCreated.value = true
        }
        rotate(if (shouldUseCompass) -azimElev.first else 0f) {
            if (shouldShowSweep) {
                drawSweep(center, sweepDegrees.floatValue, radius, trackColor)
            }
            drawRadar(radius, radarColor, strokeWidth, 3)
            drawInfo(radius, trackColor, measurer, 3)
            translate(center.x, center.y) {
                drawTrack(trackPath.value, trackEffect.value, aimColor, trackColor)
                if (item.elevation > 0) {
                    drawPosition(item, radius, animScale.value, trackColor)
                }
                if (shouldUseCompass) {
                    drawAim(azimElev.first, azimElev.second, radius, strokeWidth, aimColor)
                }
            }
            sweepDegrees.floatValue = (sweepDegrees.floatValue + 360 / 12.0f / 60) % 360
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
    drawPath(path, color, style = Stroke(6f))
    drawPath(path, effectColor, style = Stroke(pathEffect = effect))
}

private fun DrawScope.drawPosition(item: OrbitalPos, radius: Float, posRadius: Float, color: Color) {
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
        println(exception)
    }
}

private fun DrawScope.drawSweep(center: Offset, degrees: Float, radius: Float, color: Color) {
    val colors = listOf(Color.Transparent, color.copy(alpha = 0.5f), color)
    val colorStops = listOf(0.64f, 0.995f, 1f)
    val brush = ShaderBrush(SweepGradientShader(center, colors, colorStops))
    rotate(-90 + degrees, center) { drawCircle(brush, radius, style = Fill) }
}

private fun createTrackPath(positions: List<OrbitalPos>, radius: Float): Path {
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
    val shapeRadius = 24f
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
