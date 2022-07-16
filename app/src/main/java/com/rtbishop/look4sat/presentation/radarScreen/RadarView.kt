/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2022 Arty Bishop (bishop.arty@gmail.com)
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
package com.rtbishop.look4sat.presentation.radarScreen

import android.content.Context
import android.graphics.*
import android.view.View
import androidx.core.content.ContextCompat
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.domain.predict.PI_2
import com.rtbishop.look4sat.domain.predict.SatPos
import com.rtbishop.look4sat.domain.predict.TWO_PI
import com.rtbishop.look4sat.utility.toRadians
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class RadarView(context: Context) : View(context) {

    private val defaultColor = ContextCompat.getColor(context, R.color.accent)
    private val scale = resources.displayMetrics.density
    private val strokeSize = scale * 2f
    private var position: SatPos? = null
    private var positions: List<SatPos> = emptyList()

    private var radarColor = ContextCompat.getColor(context, R.color.textMain)
    private var radarCircleNum = 3
    private var radarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = radarColor
        style = Paint.Style.STROKE
        strokeWidth = strokeSize
    }

    private var shouldShowSweep = true
    private var sweepColor = defaultColor
    private var sweepDegrees = 0f
    private var sweepSpeed = 15.0f
    private var sweepPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = sweepColor
    }

    private var shouldShowBeacons = true
    private var beaconColor = defaultColor
    private var beaconSize = scale * 7f
    private var beaconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = beaconColor
        style = Paint.Style.FILL
    }

    private var radarTextSize = scale * 16f
    private var radarTextColor = defaultColor
    private var radarTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = radarTextColor
        textSize = radarTextSize
    }

    private var isTrackCreated = false
    private val trackPath: Path = Path()
    private var trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = strokeSize
    }

    private val arrowPath = Path()
    private var arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.accent)
        style = Paint.Style.FILL
        strokeWidth = strokeSize
    }

    private var shouldShowAim = true
    private var aimColor = Color.RED
    private var aimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = aimColor
        style = Paint.Style.STROKE
        strokeWidth = strokeSize
    }

    private var shouldRotateRadar = true
    private var azimuth: Float = 0f
    private var pitch: Float = 0f
    private var roll: Float = 0f

    fun setScanning(isScanning: Boolean) {
        shouldShowSweep = isScanning
    }

    fun setPosition(position: SatPos) {
        this.position = position
    }

    fun setPositions(positions: List<SatPos>) {
        this.positions = positions
    }

    fun setShowAim(showAim: Boolean) {
        shouldShowAim = showAim
    }

    fun setOrientation(azimuth: Float, pitch: Float, roll: Float) {
        this.azimuth = azimuth
        this.pitch = pitch
        this.roll = roll
        if (shouldRotateRadar) {
            this.rotation = -azimuth
        }
    }

    override fun onDraw(canvas: Canvas) {
        val radarWidth = width - paddingLeft - paddingRight
        val radarHeight = height - paddingTop - paddingBottom
        val radarRadius = min(width, height) * 0.48f
        val cx = paddingLeft + radarWidth / 2f
        val cy = paddingTop + radarHeight / 2f

        if (positions.isNotEmpty() && !isTrackCreated) {
            createPassTrajectory(radarRadius)
            createPassTrajectoryArrow()
            isTrackCreated = true
        }

        canvas.drawColor(ContextCompat.getColor(context, R.color.cardRegular))
        drawRadarCircle(canvas, cx, cy, radarRadius)
        drawRadarCross(canvas, cx, cy, radarRadius)
        drawRadarText(canvas, cx, radarRadius)

        canvas.translate(cx, cy)
        if (positions.isNotEmpty()) {
            canvas.drawPath(trackPath, trackPaint)
            canvas.drawPath(trackPath, arrowPaint)
        }
        if (shouldShowBeacons) {
            drawSatellite(canvas, radarRadius)
        }
        if (shouldShowAim) {
            drawCrosshair(canvas, azimuth, pitch, radarRadius)
        }
        if (shouldShowSweep) {
            canvas.translate(-cx, -cy)
            drawRadarSweep(canvas, cx, cy, radarRadius)
            sweepDegrees = (sweepDegrees + 360 / sweepSpeed / 60) % 360
        }
        invalidate()
    }

    private fun drawRadarCircle(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        for (i in 0 until radarCircleNum) {
            canvas.drawCircle(cx, cy, radius - radius / radarCircleNum * i, radarPaint)
        }
    }

    private fun drawRadarCross(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        canvas.drawLine(cx - radius, cy, cx + radius, cy, radarPaint)
        canvas.drawLine(cx, cy - radius, cx, cy + radius, radarPaint)
    }

    private fun drawRadarText(canvas: Canvas, cx: Float, radius: Float) {
        for (i in 0 until radarCircleNum) {
            val degText = " ${(90 / radarCircleNum) * (radarCircleNum - i)}°"
            canvas.drawText(degText, cx, radius - radius / radarCircleNum * i, radarTextPaint)
        }
    }

    private fun drawSatellite(canvas: Canvas, radarRadius: Float) {
        position?.let { satPos ->
            if (satPos.elevation > 0) {
                val satX = sph2CartX(satPos.azimuth, satPos.elevation, radarRadius.toDouble())
                val satY = sph2CartY(satPos.azimuth, satPos.elevation, radarRadius.toDouble())
                canvas.drawCircle(satX, -satY, beaconSize, beaconPaint)
            }
        }
    }

    private fun drawCrosshair(canvas: Canvas, azimuth: Float, pitch: Float, radarRadius: Float) {
        val azimuthRad = azimuth.toDouble().toRadians()
        val tmpElevation = pitch.toDouble().toRadians()
        val elevationRad = if (tmpElevation > 0.0) 0.0 else tmpElevation
        val crossX = sph2CartX(azimuthRad, -elevationRad, radarRadius.toDouble())
        val crossY = sph2CartY(azimuthRad, -elevationRad, radarRadius.toDouble())
        canvas.drawLine(crossX - radarTextSize, -crossY, crossX + radarTextSize, -crossY, aimPaint)
        canvas.drawLine(crossX, -crossY - radarTextSize, crossX, -crossY + radarTextSize, aimPaint)
        canvas.drawCircle(crossX, -crossY, radarTextSize / 2, aimPaint)
    }

    private fun sph2CartX(azimuth: Double, elevation: Double, r: Double): Float {
        val radius = r * (PI_2 - elevation) / PI_2
        return (radius * cos(PI_2 - azimuth)).toFloat()
    }

    private fun sph2CartY(azimuth: Double, elevation: Double, r: Double): Float {
        val radius = r * (PI_2 - elevation) / PI_2
        return (radius * sin(PI_2 - azimuth)).toFloat()
    }

    private fun createPassTrajectory(radarRadius: Float) {
        positions.forEachIndexed { index, satPos ->
            val passX = sph2CartX(satPos.azimuth, satPos.elevation, radarRadius.toDouble())
            val passY = sph2CartY(satPos.azimuth, satPos.elevation, radarRadius.toDouble())
            if (index == 0) {
                trackPath.moveTo(passX, -passY)
            } else {
                trackPath.lineTo(passX, -passY)
            }
        }
    }

    private fun createPassTrajectoryArrow() {
        val radius = beaconSize
        val sides = 3
        val angle = TWO_PI / sides
        arrowPath.moveTo((radius * cos(angle)).toFloat(), (radius * sin(angle)).toFloat())
        for (i in 1 until sides) {
            val x = (radius * cos(angle - angle * i)).toFloat()
            val y = (radius * sin(angle - angle * i)).toFloat()
            arrowPath.lineTo(x, y)
        }
        arrowPath.close()
        val trackLength = PathMeasure(trackPath, false).length
        val quarter = trackLength / 4f
        val center = trackLength / 2f
        val effect = PathDashPathEffect(arrowPath, center, quarter, PathDashPathEffect.Style.ROTATE)
        arrowPaint.pathEffect = effect
    }

    private fun drawRadarSweep(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val sweepGradient = SweepGradient(
            cx,
            cy,
            intArrayOf(
                Color.TRANSPARENT,
                changeAlpha(sweepColor, 0),
                changeAlpha(sweepColor, 164),
                changeAlpha(sweepColor, 255),
                changeAlpha(sweepColor, 255)
            ),
            floatArrayOf(0.0f, 0.55f, 0.996f, 0.999f, 1f)
        )
        sweepPaint.shader = sweepGradient
        canvas.rotate(-90 + sweepDegrees, cx, cy)
        canvas.drawCircle(cx, cy, radius, sweepPaint)
    }

    private fun changeAlpha(color: Int, alpha: Int): Int {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return Color.argb(alpha, red, green, blue)
    }
}
