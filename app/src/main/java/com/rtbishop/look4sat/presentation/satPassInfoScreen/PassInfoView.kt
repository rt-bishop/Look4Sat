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
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import androidx.core.content.ContextCompat
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.domain.predict4kotlin.SatPass
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

class PassInfoView(context: Context) : View(context) {

    private lateinit var satPass: SatPass
    private val polarWidth = resources.displayMetrics.widthPixels
    private val polarCenter = polarWidth / 2f
    private val scale = resources.displayMetrics.density
    private val radius = polarWidth * 0.48f
    private val txtSize = scale * 16f
    private val satTrack: Path = Path()
    private val piDiv2 = Math.PI / 2.0
    private var azimuth: Float = 0f
    private var pitch: Float = 0f
    private var roll: Float = 0f

    private val radarPaint = Paint().apply {
        isAntiAlias = true
        color = ContextCompat.getColor(context, R.color.greyLight)
        style = Paint.Style.STROKE
        strokeWidth = scale * 2f
    }
    private val txtPaint = Paint().apply {
        isAntiAlias = true
        color = ContextCompat.getColor(context, R.color.themeLight)
        textSize = txtSize
    }
    private val trackPaint = Paint().apply {
        isAntiAlias = true
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = scale * 2f
    }
    private val satPaint = Paint().apply {
        isAntiAlias = true
        color = ContextCompat.getColor(context, R.color.themeLight)
        style = Paint.Style.FILL
    }
    private val orientPaint = Paint().apply {
        isAntiAlias = true
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = scale * 2f
    }

    fun setPass(satPass: SatPass) {
        this.satPass = satPass
        createPassTrajectory(satPass)
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
        canvas.translate(polarCenter, polarCenter)
        drawRadarView(canvas)
        drawRadarText(canvas)
        if (!satPass.isDeepSpace) canvas.drawPath(satTrack, trackPaint)
        drawSatellite(canvas, satPass)
        drawOrientation(canvas, azimuth, pitch)
    }

    private fun createPassTrajectory(satPass: SatPass) {
        val startTime = satPass.aosDate
        val endTime = satPass.losDate
        while (startTime.before(endTime)) {
            val satPos = satPass.predictor.getSatPos(startTime)
            val passX = sph2CartX(satPos.azimuth, satPos.elevation, radius.toDouble())
            val passY = sph2CartY(satPos.azimuth, satPos.elevation, radius.toDouble())
            if (startTime.compareTo(satPass.aosDate) == 0) {
                satTrack.moveTo(passX, -passY)
            } else {
                satTrack.lineTo(passX, -passY)
            }
            startTime.time += 15000
        }
    }

    private fun drawRadarView(canvas: Canvas) {
        canvas.drawLine(-radius, 0f, radius, 0f, radarPaint)
        canvas.drawLine(0f, -radius, 0f, radius, radarPaint)
        canvas.drawCircle(0f, 0f, radius, radarPaint)
        canvas.drawCircle(0f, 0f, (radius / 3) * 2, radarPaint)
        canvas.drawCircle(0f, 0f, radius / 3, radarPaint)
    }

    private fun drawRadarText(canvas: Canvas) {
        canvas.drawText("N", scale, -radius + txtSize - scale * 2, txtPaint)
        canvas.drawText("30°", scale, -((radius / 3) * 2) - scale * 2, txtPaint)
        canvas.drawText("60°", scale, -(radius / 3) - scale * 2, txtPaint)
        canvas.drawText("90°", scale, -scale * 2, txtPaint)
    }

    private fun drawSatellite(canvas: Canvas, satPass: SatPass) {
        val satPos = satPass.predictor.getSatPos(Date())
        if (satPos.elevation > 0) {
            val x = sph2CartX(satPos.azimuth, satPos.elevation, radius.toDouble())
            val y = sph2CartY(satPos.azimuth, satPos.elevation, radius.toDouble())
            canvas.drawCircle(x, -y, txtSize / 2.4f, satPaint)
        }
    }

    private fun drawOrientation(canvas: Canvas, azimuth: Float, pitch: Float) {
        val azimuthRad = Math.toRadians(azimuth.toDouble())
        val tmpElevation = Math.toRadians(pitch.toDouble())
        val elevationRad = if (tmpElevation > 0.0) 0.0 else tmpElevation
        val orientX = sph2CartX(azimuthRad, -elevationRad, radius.toDouble())
        val orientY = sph2CartY(azimuthRad, -elevationRad, radius.toDouble())
        canvas.drawLine(orientX - txtSize, -orientY, orientX + txtSize, -orientY, orientPaint)
        canvas.drawLine(orientX, -orientY - txtSize, orientX, -orientY + txtSize, orientPaint)
        canvas.drawCircle(orientX, -orientY, txtSize / 2, orientPaint)
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
