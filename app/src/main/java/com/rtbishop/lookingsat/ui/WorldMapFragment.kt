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

package com.rtbishop.lookingsat.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
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
import com.rtbishop.lookingsat.MainViewModel
import com.rtbishop.lookingsat.R
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToInt

class WorldMapFragment : Fragment() {

    private val service = Executors.newSingleThreadScheduledExecutor()

    private lateinit var viewModel: MainViewModel
    private lateinit var trackView: TrackView
    private lateinit var mapFrame: FrameLayout
    private lateinit var fab: FloatingActionButton
    private lateinit var predictor: PassPredictor
    private lateinit var mainActivity: MainActivity
    private lateinit var selectedSat: TLE
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
        val tleMainList = viewModel.tleMainList
        val selectionList = viewModel.selectionList
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
                Toast.makeText(mainActivity, "No satellites were shortlisted", Toast.LENGTH_SHORT)
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
        builder.setTitle("Select Sat to track")
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
        }
        private val scale = resources.displayMetrics.density
        private val gsp = viewModel.gsp.value!!

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val frameWidth = mapFrame.measuredWidth
            val frameHeight = mapFrame.measuredHeight
            val currentTime = getDateFor(System.currentTimeMillis())
            val orbitalPeriod = (24 * 60 / selectedSat.meanmo).toInt()
            val satPosNow = predictor.getSatPos(currentTime)
            val satPosList = predictor.getPositions(currentTime, 60, 0, orbitalPeriod * 3)
            val footprintPosList = satPosNow.rangeCircle
            drawGroundTrack(canvas, frameWidth, frameHeight, satPosList)
            drawFootprint(canvas, frameWidth, frameHeight, footprintPosList)
            drawHomeLoc(canvas, frameWidth, frameHeight)
        }

        private fun drawGroundTrack(canvas: Canvas, width: Int, height: Int, list: List<SatPos>) {
            val trackPath = Path()
            var trackX: Float
            var trackY: Float
            var prevX = 181f

            list.withIndex().forEach { (index, satPos) ->
                trackX = rad2Deg(satPos.longitude).toFloat()

                if (trackX <= 180.0) trackX += 180
                else trackX -= 180

                trackX *= width / 360f
                trackY =
                    ((90.0 - rad2Deg(satPos.latitude)) * (height / 180.0)).roundToInt().toFloat()

                if (index == 0 || abs(trackX - prevX) > 180) trackPath.moveTo(trackX, trackY)
                else trackPath.lineTo(trackX, trackY)

                prevX = trackX
            }
            canvas.drawPath(trackPath, groundTrackPaint)
        }

        private fun drawFootprint(canvas: Canvas, width: Int, height: Int, list: List<Position>) {
            val printPath = Path()
            var printX: Float
            var printY: Float
            var prevX = 181f

            list.withIndex().forEach { (index, position) ->
                printX = position.lon.toFloat()

                if (printX <= 180f) printX += 180f
                else printX -= 180f

                printX *= width / 360f
                printY = ((90.0 - position.lat) * (height / 180.0)).roundToInt().toFloat()

                if (index == 0 || abs(printX - prevX) > 180) printPath.moveTo(printX, printY)
                else printPath.lineTo(printX, printY)

                prevX = printX
            }
            canvas.drawPath(printPath, footprintPaint)
        }

        private fun drawHomeLoc(canvas: Canvas, frameWidth: Int, frameHeight: Int) {
            canvas.setMatrix(Matrix().apply {
                postTranslate(frameWidth / 2f, frameHeight / 2f)
            })
            val cx = frameWidth / 360f * gsp.longitude.toFloat()
            val cy = frameHeight / 180f * gsp.latitude.toFloat() * -1
            canvas.drawCircle(cx, cy, scale * 2, txtPaint)
            canvas.drawText("GSP", cx - txtPaint.textSize, cy - txtPaint.textSize, txtPaint)
        }

        private fun getDateFor(value: Long): Date {
            return Date(value)
        }

        private fun rad2Deg(value: Double): Double {
            return value * 180 / Math.PI
        }
    }
}