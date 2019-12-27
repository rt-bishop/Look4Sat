/*
 * LookingSat. Amateur radio & weather satellite tracker and passes calculator.
 * Copyright (C) 2019 Arty Bishop (bishop.arty@gmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.rtbishop.look4sat.ui

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.github.amsacode.predict4java.PassPredictor
import com.github.amsacode.predict4java.Position
import com.github.amsacode.predict4java.SatPos
import com.github.amsacode.predict4java.TLE
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.rtbishop.look4sat.MainViewModel
import com.rtbishop.look4sat.R
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class WorldMapFragment : Fragment() {

    private val service = Executors.newSingleThreadScheduledExecutor()

    private lateinit var viewModel: MainViewModel
    private lateinit var trackView: TrackView
    private lateinit var mapFrame: FrameLayout
    private lateinit var fab: FloatingActionButton
    private lateinit var predictor: PassPredictor
    private lateinit var mainActivity: MainActivity
    private lateinit var selectedSat: TLE
    private lateinit var tleMainList: List<TLE>
    private lateinit var selectionList: MutableList<Int>
    private var checkedItem = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivity = activity as MainActivity
        viewModel = ViewModelProvider(mainActivity).get(MainViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_worldmap, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val delay = viewModel.updateFreq
        tleMainList = viewModel.tleMainList
        selectionList = viewModel.selectionList
        mapFrame = view.findViewById(R.id.worldmap_frame)
        fab = view.findViewById(R.id.worldmap_fab)

        if (tleMainList.isNotEmpty() && selectionList.isNotEmpty()) {
            fab.setOnClickListener { showSelectSatDialog(tleMainList, selectionList) }
            selectedSat = tleMainList[selectionList[0]]
            predictor = PassPredictor(selectedSat, viewModel.gsp.value)
            trackView = TrackView(mainActivity)
            mapFrame.addView(trackView)
            service.scheduleAtFixedRate(
                { trackView.invalidate() },
                delay,
                delay,
                TimeUnit.MILLISECONDS
            )
        } else {
            fab.setOnClickListener {
                Toast.makeText(
                    mainActivity,
                    getString(R.string.no_selected_sat),
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }
    }

    private fun showSelectSatDialog(tleMainList: List<TLE>, selectionList: MutableList<Int>) {
        val tleNameArray = arrayOfNulls<String>(selectionList.size).apply {
            selectionList.withIndex().forEach { (index, selection) ->
                this[index] = tleMainList[selection].name
            }
        }

        val builder = AlertDialog.Builder(mainActivity)
        builder.setTitle(getString(R.string.dialog_show_track))
            .setSingleChoiceItems(tleNameArray, checkedItem) { dialog, which ->
                checkedItem = which
                selectedSat = tleMainList[selectionList[which]]
                predictor = PassPredictor(selectedSat, viewModel.gsp.value)
                trackView.invalidate()
                dialog.dismiss()
            }
            .create()
            .show()
    }

    inner class TrackView(context: Context) : View(context) {
        private val gsp = viewModel.gsp.value!!
        private val scale = resources.displayMetrics.density
        private val groundTrackPaint = Paint().apply {
            isAntiAlias = true
            color = resources.getColor(R.color.satTrack, mainActivity.theme)
            style = Paint.Style.STROKE
            strokeWidth = scale
        }
        private val footprintPaint = Paint().apply {
            isAntiAlias = true
            color = resources.getColor(R.color.satFootprint, mainActivity.theme)
            style = Paint.Style.STROKE
            strokeWidth = scale
        }
        private val txtPaint = Paint().apply {
            isAntiAlias = true
            color = resources.getColor(R.color.satFootprint, mainActivity.theme)
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

        override fun onDraw(canvas: Canvas) {
            canvas.translate(width / 2f, height / 2f)
            val degLon = width / 360f
            val degLat = height / 180f
            drawHomeLoc(canvas, degLon, degLat)

            val currentTime = getDateFor(System.currentTimeMillis())
            val orbitalPeriod = (24 * 60 / selectedSat.meanmo).toInt()
            val satPosList = predictor.getPositions(currentTime, 60, 0, orbitalPeriod * 3)
            drawGroundTrack(canvas, degLon, degLat, satPosList)

            selectionList.forEach {
                drawSat(it, currentTime, canvas, degLon, degLat)
            }
        }

        private fun drawHomeLoc(cvs: Canvas, degLon: Float, degLat: Float) {
            val lon = gsp.longitude.toFloat()
            val lat = gsp.latitude.toFloat() * -1
            val cx = lon * degLon
            val cy = lat * degLat
            cvs.drawCircle(cx, cy, scale * 2, txtPaint)
            cvs.drawText(
                context.getString(R.string.map_gsp),
                cx - txtPaint.textSize,
                cy - txtPaint.textSize,
                txtPaint
            )
        }

        private fun drawGroundTrack(cvs: Canvas, degLon: Float, degLat: Float, list: List<SatPos>) {
            val path = Path()
            var lon: Float
            var lat: Float
            var lastLon = 181f

            list.withIndex().forEach { (index, satPos) ->
                lon = rad2Deg(satPos.longitude).toFloat()
                lat = rad2Deg(satPos.latitude).toFloat() * -1

                if (lon > 180f) lon -= 360f

                lon *= degLon
                lat *= degLat

                if (index == 0 || abs(lon - lastLon) > 180) path.moveTo(lon, lat)
                else path.lineTo(lon, lat)

                lastLon = lon
            }
            cvs.drawPath(path, groundTrackPaint)
        }

        private fun drawSat(index: Int, date: Date, canvas: Canvas, degLon: Float, degLat: Float) {
            val tle = tleMainList[index]
            val predictor = PassPredictor(tle, gsp)
            val satPosNow = predictor.getSatPos(date)
            val footprintPosList = satPosNow.rangeCircle
            drawFootprint(canvas, degLon, degLat, footprintPosList)
            drawName(canvas, degLon, degLat, satPosNow, tle.name)
        }

        private fun drawFootprint(cvs: Canvas, degLon: Float, degLat: Float, list: List<Position>) {
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
            cvs.drawPath(path, footprintPaint)
        }

        private fun drawName(cvs: Canvas, degLon: Float, degLat: Float, pos: SatPos, name: String) {
            var lon = rad2Deg(pos.longitude).toFloat()
            val lat = rad2Deg(pos.latitude).toFloat() * -1
            if (lon > 180f) lon -= 360f
            val cx = lon * degLon
            val cy = lat * degLat
            cvs.drawCircle(cx, cy, scale * 2, txtPaint)
            txtPaint.getTextBounds(name, 0, name.length, rect)
            cvs.drawText(name, cx - rect.width() / 2, cy - txtPaint.textSize, outlinePaint)
            cvs.drawText(name, cx - rect.width() / 2, cy - txtPaint.textSize, txtPaint)
        }

        private fun getDateFor(value: Long): Date {
            return Date(value)
        }

        private fun rad2Deg(value: Double): Double {
            return value * 180 / Math.PI
        }
    }
}