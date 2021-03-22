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
package com.rtbishop.look4sat.ui.polarScreen

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import androidx.core.content.ContextCompat
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.data.model.SatPass
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

class PolarView(context: Context) : View(context) {
    
    private lateinit var satPass: SatPass
    private val polarWidth = resources.displayMetrics.widthPixels
    private val polarCenter = polarWidth / 2f
    private val scale = resources.displayMetrics.density
    private val radius = polarWidth * 0.48f
    private val txtSize = scale * 16f
    private val path: Path = Path()
    private val piDiv2 = Math.PI / 2.0

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
    
    fun setPass(satPass: SatPass) {
        this.satPass = satPass
    }
    
    override fun onDraw(canvas: Canvas) {
        canvas.translate(polarCenter, polarCenter)
        drawRadarView(canvas)
        drawRadarText(canvas)
        if (!satPass.satellite.tle.isDeepspace) drawPassTrajectory(canvas, satPass)
        drawSatellite(canvas, satPass)
    }
    
    private fun drawRadarView(cvs: Canvas) {
        cvs.drawLine(-radius, 0f, radius, 0f, radarPaint)
        cvs.drawLine(0f, -radius, 0f, radius, radarPaint)
        cvs.drawCircle(0f, 0f, radius, radarPaint)
        cvs.drawCircle(0f, 0f, (radius / 3) * 2, radarPaint)
        cvs.drawCircle(0f, 0f, radius / 3, radarPaint)
    }

    private fun drawRadarText(cvs: Canvas) {
        cvs.drawText("N", scale, -radius + txtSize - scale * 2, txtPaint)
        cvs.drawText("30°", scale, -((radius / 3) * 2) - scale * 2, txtPaint)
        cvs.drawText("60°", scale, -(radius / 3) - scale * 2, txtPaint)
        cvs.drawText("90°", scale, -scale * 2, txtPaint)
    }

    private fun drawPassTrajectory(cvs: Canvas, satPass: SatPass) {
        val startTime = satPass.pass.getStartTime()
        val endTime = satPass.pass.getEndTime()
        while (startTime.before(endTime)) {
            val satPos = satPass.predictor.getSatPos(startTime)
            val x = sph2CartX(satPos.azimuth, satPos.elevation, radius.toDouble())
            val y = sph2CartY(satPos.azimuth, satPos.elevation, radius.toDouble())
            if (startTime.compareTo(satPass.pass.getStartTime()) == 0) {
                path.moveTo(x, -y)
            } else {
                path.lineTo(x, -y)
            }
            startTime.time += 15000
        }
        cvs.drawPath(path, trackPaint)
    }

    private fun drawSatellite(cvs: Canvas, satPass: SatPass) {
        val date = Date(System.currentTimeMillis())
        val satPos = satPass.predictor.getSatPos(date)
        if (satPos.elevation > 0) {
            val x = sph2CartX(satPos.azimuth, satPos.elevation, radius.toDouble())
            val y = sph2CartY(satPos.azimuth, satPos.elevation, radius.toDouble())
            cvs.drawCircle(x, -y, txtSize / 2.4f, satPaint)
        }
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
