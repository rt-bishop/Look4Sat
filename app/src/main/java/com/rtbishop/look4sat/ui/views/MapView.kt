package com.rtbishop.look4sat.ui.views

import android.content.Context
import android.graphics.*
import android.view.View
import androidx.core.content.ContextCompat
import com.github.amsacode.predict4java.GroundStationPosition
import com.github.amsacode.predict4java.Position
import com.github.amsacode.predict4java.SatPos
import com.github.amsacode.predict4java.TLE
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.data.SatPass
import com.rtbishop.look4sat.utility.GeneralUtils
import java.util.*
import kotlin.math.abs

class MapView(context: Context) : View(context) {

    constructor(context: Context, groundStationPosition: GroundStationPosition) : this(context) {
        gsp = groundStationPosition
    }

    private lateinit var passList: List<SatPass>
    private lateinit var gsp: GroundStationPosition
    private lateinit var selectedSat: TLE
    private val scale = resources.displayMetrics.density
    private val groundTrackPaint = Paint().apply {
        isAntiAlias = true
        color = ContextCompat.getColor(context, R.color.satTrack)
        style = Paint.Style.STROKE
        strokeWidth = scale
    }
    private val satPaintPrim = Paint().apply {
        isAntiAlias = true
        color = ContextCompat.getColor(context, R.color.satPrimary)
        style = Paint.Style.STROKE
        strokeWidth = scale
    }
    private val satPaintSec = Paint().apply {
        isAntiAlias = true
        color = ContextCompat.getColor(context, R.color.satSecondary)
        style = Paint.Style.STROKE
        strokeWidth = scale
    }
    private val txtPaint = Paint().apply {
        isAntiAlias = true
        color = ContextCompat.getColor(context, R.color.satPrimary)
        style = Paint.Style.FILL
        strokeWidth = scale
        textSize = 16f
        setShadowLayer(2f, 2f, 2f, Color.BLACK)
    }
    private val outlinePaint = Paint().apply {
        isAntiAlias = true
        color = Color.BLACK
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = scale
        textSize = 16f
    }
    private val rect = Rect()
    private var checked = 0
    private var degLon = 0f
    private var degLat = 0f

    fun setList(satPassList: List<SatPass>) {
        passList = satPassList
    }

    fun setChecked(item: Int) {
        checked = item
        selectedSat = passList[checked].tle
    }

    override fun onDraw(canvas: Canvas) {
        degLon = width / 360f
        degLat = height / 180f
        canvas.translate(width / 2f, height / 2f)
        drawHomeLoc(canvas)
        val currentTime = GeneralUtils.getDateFor(System.currentTimeMillis())
        val orbitalPeriod = (24 * 60 / selectedSat.meanmo).toInt()
        val predictor = passList[checked].predictor
        val positions = predictor.getPositions(currentTime, 60, 0, orbitalPeriod * 3)
        drawGroundTrack(canvas, positions)
        passList.forEach { satPass -> drawSat(canvas, satPass, currentTime, satPaintSec) }
        drawSat(canvas, passList[checked], currentTime, satPaintPrim)
    }

    private fun drawHomeLoc(canvas: Canvas) {
        val lon = gsp.longitude.toFloat()
        val lat = gsp.latitude.toFloat() * -1
        val cx = lon * degLon
        val cy = lat * degLat
        val gspName = context.getString(R.string.map_gsp)
        txtPaint.getTextBounds(gspName, 0, gspName.length, rect)
        canvas.drawCircle(cx, cy, scale * 2, txtPaint)
        canvas.drawText(gspName, cx - rect.width() / 2, cy - txtPaint.textSize, outlinePaint)
        canvas.drawText(gspName, cx - rect.width() / 2, cy - txtPaint.textSize, txtPaint)

    }

    private fun drawGroundTrack(canvas: Canvas, list: List<SatPos>) {
        val path = Path()
        var lon: Float
        var lat: Float
        var lastLon = 181f

        list.withIndex().forEach { (index, satPos) ->
            lon = GeneralUtils.rad2Deg(satPos.longitude).toFloat()
            lat = GeneralUtils.rad2Deg(satPos.latitude).toFloat() * -1

            if (lon > 180f) lon -= 360f

            lon *= degLon
            lat *= degLat

            if (index == 0 || abs(lon - lastLon) > 180) path.moveTo(lon, lat)
            else path.lineTo(lon, lat)

            lastLon = lon
        }
        canvas.drawPath(path, groundTrackPaint)
    }

    private fun drawSat(canvas: Canvas, satPass: SatPass, date: Date, paint: Paint) {
        val tle = satPass.tle
        val predictor = satPass.predictor
        val satPosNow = predictor.getSatPos(date)
        val footprintPosList = satPosNow.rangeCircle
        drawFootprint(canvas, footprintPosList, paint)
        drawName(canvas, satPosNow, tle.name)
    }

    private fun drawFootprint(canvas: Canvas, list: List<Position>, paint: Paint) {
        val path = Path()
        var lon: Float
        var lat: Float
        var lastLon = 181f

        list.withIndex().forEach { (index, position) ->
            lon = position.lon.toFloat()
            lat = position.lat.toFloat() * -1

            if (lon > 180f) lon -= 360f

            lon *= degLon
            lat *= degLat

            if (index == 0 || abs(lon - lastLon) > 180) path.moveTo(lon, lat)
            else path.lineTo(lon, lat)

            lastLon = lon
        }
        canvas.drawPath(path, paint)
    }

    private fun drawName(canvas: Canvas, position: SatPos, name: String) {
        var lon = GeneralUtils.rad2Deg(position.longitude).toFloat()
        val lat = GeneralUtils.rad2Deg(position.latitude).toFloat() * -1

        if (lon > 180f) lon -= 360f

        val cx = lon * degLon
        val cy = lat * degLat

        canvas.drawCircle(cx, cy, scale * 2, txtPaint)
        txtPaint.getTextBounds(name, 0, name.length, rect)
        canvas.drawText(name, cx - rect.width() / 2, cy - txtPaint.textSize, outlinePaint)
        canvas.drawText(name, cx - rect.width() / 2, cy - txtPaint.textSize, txtPaint)
    }
}