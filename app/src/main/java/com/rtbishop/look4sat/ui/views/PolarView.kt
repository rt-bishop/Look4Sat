package com.rtbishop.look4sat.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import androidx.core.content.ContextCompat
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.data.SatPass
import com.rtbishop.look4sat.utility.GeneralUtils

class PolarView(context: Context) : View(context) {

    constructor(context: Context, satPass: SatPass) : this(context) {
        pass = satPass
    }

    private lateinit var pass: SatPass
    private val polarWidth = resources.displayMetrics.widthPixels
    private val polarCenter = polarWidth / 2f
    private val scale = resources.displayMetrics.density
    private val radius = polarWidth * 0.49f
    private val txtSize = scale * 15
    private val path: Path = Path()
    var azimuth = 0f

    private val radarPaint = Paint().apply {
        isAntiAlias = true
        color = ContextCompat.getColor(context, R.color.themeLight)
        style = Paint.Style.STROKE
        strokeWidth = scale
    }
    private val txtPaint = Paint().apply {
        isAntiAlias = true
        color = ContextCompat.getColor(context, R.color.themeAccent)
        textSize = txtSize
    }
    private val trackPaint = Paint().apply {
        isAntiAlias = true
        color = ContextCompat.getColor(context, R.color.satTrack)
        style = Paint.Style.STROKE
        strokeWidth = scale
    }
    private val satPaint = Paint().apply {
        isAntiAlias = true
        color = ContextCompat.getColor(context, R.color.themeAccent)
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        canvas.rotate(-azimuth, polarCenter, polarCenter)
        canvas.translate(polarCenter, polarCenter)
        drawRadarView(canvas)
        drawRadarText(canvas)
        if (!pass.tle.isDeepspace) {
            drawPassTrajectory(canvas, pass)
        }
        drawSatellite(canvas, pass)
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
        val startTime = satPass.pass.startTime
        val endTime = satPass.pass.endTime
        while (startTime.before(endTime)) {
            val satPos = satPass.predictor.getSatPos(startTime)
            val x = GeneralUtils.sph2CartX(satPos.azimuth, satPos.elevation, radius.toDouble())
            val y = GeneralUtils.sph2CartY(satPos.azimuth, satPos.elevation, radius.toDouble())
            if (startTime.compareTo(satPass.pass.startTime) == 0) {
                path.moveTo(x, -y)
            } else {
                path.lineTo(x, -y)
            }
            startTime.time += 15000
        }
        cvs.drawPath(path, trackPaint)
    }

    private fun drawSatellite(cvs: Canvas, satPass: SatPass) {
        val date = GeneralUtils.getDateFor(System.currentTimeMillis())
        val satPos = satPass.predictor.getSatPos(date)
        if (satPos.elevation > 0) {
            val x = GeneralUtils.sph2CartX(satPos.azimuth, satPos.elevation, radius.toDouble())
            val y = GeneralUtils.sph2CartY(satPos.azimuth, satPos.elevation, radius.toDouble())
            cvs.drawCircle(x, -y, txtSize / 3, satPaint)
        }
    }
}