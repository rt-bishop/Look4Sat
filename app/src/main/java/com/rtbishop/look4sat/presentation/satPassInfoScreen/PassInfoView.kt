/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2021 Arty Bishop (bishop.arty@gmail.com)
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
package com.rtbishop.look4sat.presentation.satPassInfoScreen

import android.content.Context
import android.graphics.*
import android.view.View
import androidx.core.content.ContextCompat
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.domain.predict4kotlin.SatPass
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

class PassInfoView(context: Context) : View(context) {

    private lateinit var satPass: SatPass
    private val scale = resources.displayMetrics.density
    private val radarWidth = resources.displayMetrics.widthPixels
    private val radarCenter = radarWidth / 2f
    private val radarRadius = radarWidth * 0.48f
    private val piDiv2 = Math.PI / 2.0
    private val arrowPath = Path()
    private val satTrack: Path = Path()
    private val strokeSize = scale * 2f
    private val satSize = scale * 8f
    private val txtSize = scale * 16f
    private val radarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.greyLight)
        style = Paint.Style.STROKE
        strokeWidth = strokeSize
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.themeLight)
        textSize = txtSize
    }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = strokeSize
    }
    private val satPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.themeLight)
        style = Paint.Style.FILL
    }
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.themeLight)
        style = Paint.Style.FILL
        strokeWidth = strokeSize
    }
    private var azimuth: Float = 0f
    private var pitch: Float = 0f
    private var roll: Float = 0f

    fun setPass(satPass: SatPass) {
        this.satPass = satPass
        if (!satPass.isDeepSpace) {
            createPassTrajectory(satPass)
            createPassTrajectoryArrow()
        }
    }

    fun setOrientation(azimuth: Float, pitch: Float, roll: Float) {
        this.azimuth = azimuth
        this.pitch = pitch
        this.roll = roll
        this.rotation = -azimuth
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(ContextCompat.getColor(context, R.color.greyDark))
        canvas.translate(radarCenter, radarCenter)
        drawRadarView(canvas)
        drawRadarText(canvas)
        if (!satPass.isDeepSpace) {
            canvas.drawPath(satTrack, trackPaint)
            canvas.drawPath(satTrack, arrowPaint)
        }
        drawSatellite(canvas, satPass)
        drawCrosshair(canvas, azimuth, pitch)
    }

    private fun createPassTrajectory(satPass: SatPass) {
        val currentTime = satPass.aosDate
        while (currentTime.before(satPass.losDate)) {
            val satPos = satPass.predictor.getSatPos(currentTime)
            val passX = sph2CartX(satPos.azimuth, satPos.elevation, radarRadius.toDouble())
            val passY = sph2CartY(satPos.azimuth, satPos.elevation, radarRadius.toDouble())
            if (currentTime.compareTo(satPass.aosDate) == 0) {
                satTrack.moveTo(passX, -passY)
            } else {
                satTrack.lineTo(passX, -passY)
            }
            currentTime.time += 15000
        }
    }

    private fun createPassTrajectoryArrow() {
        val radius = satSize
        val sides = 3
        val angle = 2.0 * Math.PI / sides
        arrowPath.moveTo((radius * cos(angle)).toFloat(), (radius * sin(angle)).toFloat())
        for (i in 1 until sides) {
            val x = (radius * cos(angle - angle * i)).toFloat()
            val y = (radius * sin(angle - angle * i)).toFloat()
            arrowPath.lineTo(x, y)
        }
        arrowPath.close()
        val trackLength = PathMeasure(satTrack, false).length
        val quarter = trackLength / 4f
        val center = trackLength / 2f
        val effect = PathDashPathEffect(arrowPath, center, quarter, PathDashPathEffect.Style.ROTATE)
        arrowPaint.pathEffect = effect
    }

    private fun drawRadarView(canvas: Canvas) {
        canvas.drawLine(-radarRadius, 0f, radarRadius, 0f, radarPaint)
        canvas.drawLine(0f, -radarRadius, 0f, radarRadius, radarPaint)
        canvas.drawCircle(0f, 0f, radarRadius, radarPaint)
        canvas.drawCircle(0f, 0f, (radarRadius / 3) * 2, radarPaint)
        canvas.drawCircle(0f, 0f, radarRadius / 3, radarPaint)
    }

    private fun drawRadarText(canvas: Canvas) {
        canvas.drawText("N", scale, -radarRadius + txtSize - strokeSize, textPaint)
        canvas.drawText("30°", scale, -((radarRadius / 3) * 2) - strokeSize, textPaint)
        canvas.drawText("60°", scale, -(radarRadius / 3) - strokeSize, textPaint)
        canvas.drawText("90°", scale, -strokeSize, textPaint)
    }

    private fun drawSatellite(canvas: Canvas, satPass: SatPass) {
        val satPos = satPass.predictor.getSatPos(Date())
        if (satPos.elevation > 0) {
            val satX = sph2CartX(satPos.azimuth, satPos.elevation, radarRadius.toDouble())
            val satY = sph2CartY(satPos.azimuth, satPos.elevation, radarRadius.toDouble())
            canvas.drawCircle(satX, -satY, satSize, satPaint)
        }
    }

    private fun drawCrosshair(canvas: Canvas, azimuth: Float, pitch: Float) {
        val azimuthRad = Math.toRadians(azimuth.toDouble())
        val tmpElevation = Math.toRadians(pitch.toDouble())
        val elevationRad = if (tmpElevation > 0.0) 0.0 else tmpElevation
        val crossX = sph2CartX(azimuthRad, -elevationRad, radarRadius.toDouble())
        val crossY = sph2CartY(azimuthRad, -elevationRad, radarRadius.toDouble())
        canvas.drawLine(crossX - txtSize, -crossY, crossX + txtSize, -crossY, trackPaint)
        canvas.drawLine(crossX, -crossY - txtSize, crossX, -crossY + txtSize, trackPaint)
        canvas.drawCircle(crossX, -crossY, txtSize / 2, trackPaint)
    }

    private fun sph2CartX(azimuth: Double, elevation: Double, r: Double): Float {
        val radius = r * (piDiv2 - elevation) / piDiv2
        return (radius * cos(piDiv2 - azimuth)).toFloat()
    }

    private fun sph2CartY(azimuth: Double, elevation: Double, r: Double): Float {
        val radius = r * (piDiv2 - elevation) / piDiv2
        return (radius * sin(piDiv2 - azimuth)).toFloat()
    }
}
