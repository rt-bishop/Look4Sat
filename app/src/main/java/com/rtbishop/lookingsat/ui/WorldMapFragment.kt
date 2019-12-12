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
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.github.amsacode.predict4java.PassPredictor
import com.github.amsacode.predict4java.Position
import com.github.amsacode.predict4java.SatPos
import com.github.amsacode.predict4java.TLE
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
    //    private lateinit var fab: FloatingActionButton
    private lateinit var selectedSat: TLE
    private lateinit var predictor: PassPredictor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(activity as MainActivity).get(MainViewModel::class.java)
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
        mapFrame = view.findViewById(R.id.worldmap_frame)
//        fab = view.findViewById(R.id.worldmap_fab)
//        fab.setOnClickListener { showSelectSatDialog() }

        val delay = viewModel.updateFreq
        val tleString = arrayOf(
            "METEOR-M2 2",
            "1 44387U 19038A   19336.45319619 -.00000025  00000-0  80323-5 0  9996",
            "2 44387  98.5920 295.7459 0002200  37.8724 322.2609 14.23335951 21369"
        )

        selectedSat = TLE(tleString)
        predictor = PassPredictor(selectedSat, viewModel.gsp.value)

//        selectedSat = viewModel.selectedSingleSat
//        lifecycleScope.launch {
//            predictor = PassPredictor(selectedSat, viewModel.gsp.value)
//        }

        trackView = TrackView(activity as MainActivity)
        mapFrame.addView(trackView)
        service.scheduleAtFixedRate({ trackView.invalidate() }, 0, delay, TimeUnit.MILLISECONDS)
    }

//    private fun showSelectSatDialog() {
//        val tleMainList = viewModel.tleMainList
//        val tleNameArray = arrayOfNulls<String>(tleMainList.size)
//        var selection = viewModel.selectedSingleSat
//
//        tleMainList.withIndex().forEach { (position, tle) ->
//            tleNameArray[position] = tle.name
//        }
//
//        val builder = AlertDialog.Builder(activity as MainActivity)
//        builder.setTitle("Select Sat to track")
//            .setSingleChoiceItems(tleNameArray, -1) { _, which ->
//                selection = tleMainList[which]
//            }
//            .setPositiveButton("Ok") { _, _ ->
//                viewModel.updateSelectedSingleSat(selection)
//            }
//            .setNegativeButton("Cancel") { dialog, _ ->
//                dialog.cancel()
//            }
//            .create()
//            .show()
//    }

    inner class TrackView(context: Context) : View(context) {
        private val groundTrackPaint = Paint().apply {
            isAntiAlias = true
            color = resources.getColor(R.color.satTrack, (activity as MainActivity).theme)
            style = Paint.Style.STROKE
            strokeWidth = scale
        }
        private val footprintPaint = Paint().apply {
            isAntiAlias = true
            color = resources.getColor(R.color.satFootprint, (activity as MainActivity).theme)
            style = Paint.Style.STROKE
            strokeWidth = scale
        }
        private val homeLocPaint = Paint().apply {
            isAntiAlias = true
            color = resources.getColor(R.color.satFootprint, (activity as MainActivity).theme)
            style = Paint.Style.FILL
            strokeWidth = scale
        }
        private val scale = resources.displayMetrics.density
        private val gsp = viewModel.gsp.value!!

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val frameWidth = mapFrame.measuredWidth
            val frameHeight = mapFrame.measuredHeight
            val currentTime = getDateFor(System.currentTimeMillis())
            val orbitalPeriod = (24 * 60 / selectedSat.meanmo).toInt()
            val satPos = predictor.getSatPos(currentTime)
            val footprintPositions = satPos.rangeCircle
            val positions = predictor.getPositions(currentTime, 60, 0, orbitalPeriod * 3)
            drawGroundTrack(canvas, frameWidth, frameHeight, positions)
            drawFootprint(canvas, frameWidth, frameHeight, footprintPositions)
            var longitude = rad2Deg(satPos.longitude)
            if (longitude > 180.0) longitude -= 180.0
            else longitude += 180.0
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
            canvas.drawCircle(cx, cy, scale * 2, homeLocPaint)
        }

        private fun getDateFor(value: Long): Date {
            return Date(value)
        }

        private fun rad2Deg(value: Double): Double {
            return value * 180 / Math.PI
        }
    }
}