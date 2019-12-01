package com.rtbishop.lookingsat.ui

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.amsacode.predict4java.SatPos
import com.rtbishop.lookingsat.MainViewModel
import com.rtbishop.lookingsat.R
import com.rtbishop.lookingsat.repo.SatPass
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.sin

class RadarFragment : Fragment() {

    private val delay = 5000L
    private val service = Executors.newSingleThreadScheduledExecutor()

    private lateinit var viewModel: MainViewModel
    private lateinit var satPass: SatPass
    private lateinit var radarView: RadarView
    private lateinit var radarSkyFrame: FrameLayout
    private lateinit var transRecycler: RecyclerView
    private lateinit var transAdapter: TransAdapter
    private lateinit var radarSatName: TextView
    private lateinit var radarMaxEl: TextView
    private lateinit var radarAos: TextView
    private lateinit var radarLos: TextView
    private lateinit var transNoFound: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(activity as MainActivity).get(MainViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_radar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        satPass = arguments?.get("satPass") as SatPass
        radarSkyFrame = view.findViewById(R.id.radar_sky_frame)
        transRecycler = view.findViewById(R.id.radar_recycler)
        radarSatName = view.findViewById(R.id.radar_sat_name)
        radarMaxEl = view.findViewById(R.id.radar_maxEl)
        radarAos = view.findViewById(R.id.radar_aos)
        radarLos = view.findViewById(R.id.radar_los)
        transNoFound = view.findViewById(R.id.radar_trans_no_found)

        setupRadarView()
        setupTransRecycler()
    }

    private fun setupTransRecycler() {
        transAdapter = TransAdapter()
        transRecycler.apply {
            layoutManager = LinearLayoutManager(activity as MainActivity)
            adapter = transAdapter
        }
        lifecycleScope.launch {
            val transList = viewModel.getTransmittersForSat(satPass.tle.catnum)
            if (transList.isNotEmpty()) {
                transAdapter.setList(transList)
                transAdapter.notifyDataSetChanged()
            } else {
                transRecycler.visibility = View.INVISIBLE
                transNoFound.visibility = View.VISIBLE
            }
        }
    }

    private fun setupRadarView() {
        radarView = RadarView(activity as MainActivity)
        radarSkyFrame.addView(radarView)

        val aosTime =
            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(satPass.pass.startTime)
        val losTime =
            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(satPass.pass.endTime)
        radarSatName.text = satPass.tle.name
        radarMaxEl.text = String.format("MaxEl: %.1f°", satPass.pass.maxEl)
        radarAos.text = String.format("AOS - %s", aosTime)
        radarLos.text = String.format("LOS - %s", losTime)

        service.scheduleAtFixedRate({ radarView.invalidate() }, delay, delay, TimeUnit.MILLISECONDS)
    }

    inner class RadarView(context: Context) : View(context) {

        private val radarSize = resources.displayMetrics.widthPixels
        private val scale = resources.displayMetrics.density
        private val startTime = satPass.pass.startTime
        private val endTime = satPass.pass.endTime
        private val radius = radarSize * 0.45f
        private val piDiv2 = Math.PI / 2.0
        private val txtSize = scale * 15
        private val center = 0f

        private val bmp: Bitmap = Bitmap.createBitmap(radarSize, radarSize, Bitmap.Config.ARGB_8888)
        private val mtrx: Matrix = Matrix().apply {
            postTranslate(radarSize / 2f, radarSize / 2f)
        }
        private val cvs: Canvas = Canvas().apply {
            setBitmap(bmp)
            setMatrix(mtrx)
        }
        private val radarPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#C0C0C0")
            style = Paint.Style.STROKE
            strokeWidth = scale
        }
        private val txtPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#FFE082")
            textSize = txtSize
        }
        private val passPaint = Paint().apply {
            isAntiAlias = true
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = scale
        }
        private val satPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#FFE082")
            style = Paint.Style.FILL
        }
        private val path: Path = Path()

        private lateinit var satPos: SatPos
        private var satPassX = 0f
        private var satPassY = 0f

        override fun onDraw(canvas: Canvas) {
            bmp.eraseColor(Color.TRANSPARENT)
            drawRadarView()
            drawRadarText()
            drawPassTrajectory()
            drawSatellite()
            canvas.drawBitmap(bmp, left.toFloat(), top.toFloat(), null)
        }

        private fun drawRadarView() {
            cvs.drawLine(center - radius, center, center + radius, center, radarPaint)
            cvs.drawLine(center, center - radius, center, center + radius, radarPaint)
            cvs.drawCircle(center, center, radius, radarPaint)
            cvs.drawCircle(center, center, (radius / 3) * 2, radarPaint)
            cvs.drawCircle(center, center, radius / 3, radarPaint)
        }

        private fun drawRadarText() {
            cvs.drawText("N", center - txtSize / 3, center - radius - scale * 2, txtPaint)
            cvs.drawText("E", center + radius + scale * 2, center + txtSize / 3, txtPaint)
            cvs.drawText("S", center - txtSize / 3, center + radius + txtSize, txtPaint)
            cvs.drawText("W", center - radius - txtSize, center + txtSize / 3, txtPaint)
            cvs.drawText("0°", center + scale, center - scale * 2, txtPaint)
            cvs.drawText("30°", center + scale, center - (radius / 3) - scale * 2, txtPaint)
            cvs.drawText("60°", center + scale, center - ((radius / 3) * 2) - scale * 2, txtPaint)
        }

        private fun drawPassTrajectory() {
            while (startTime.before(endTime)) {
                satPos = satPass.predictor.getSatPos(startTime)
                satPassX = center + sph2CartX(satPos.azimuth, satPos.elevation, radius.toDouble())
                satPassY = center - sph2CartY(satPos.azimuth, satPos.elevation, radius.toDouble())
                if (startTime.compareTo(satPass.pass.startTime) == 0) {
                    path.moveTo(satPassX, satPassY)
                } else {
                    path.lineTo(satPassX, satPassY)
                }
                startTime.time += delay
            }
            cvs.drawPath(path, passPaint)
        }

        private fun drawSatellite() {
            satPos = satPass.predictor.getSatPos(Date())
            if (satPos.elevation > 0) {
                cvs.drawCircle(
                    center + sph2CartX(satPos.azimuth, satPos.elevation, radius.toDouble()),
                    center - sph2CartY(satPos.azimuth, satPos.elevation, radius.toDouble()),
                    txtSize / 3, satPaint
                )
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
}