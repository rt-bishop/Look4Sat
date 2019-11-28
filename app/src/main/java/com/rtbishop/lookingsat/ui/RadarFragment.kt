package com.rtbishop.lookingsat.ui

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.github.amsacode.predict4java.SatPos
import com.rtbishop.lookingsat.R
import com.rtbishop.lookingsat.repo.SatPass
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.sin

class RadarFragment : Fragment() {

    private lateinit var satPass: SatPass
    private lateinit var radarView: RadarView
    private lateinit var radarSkyFrame: FrameLayout
    private lateinit var radarRecycler: RecyclerView
    private val service = Executors.newSingleThreadScheduledExecutor()
    private val delay = 5000L

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
        radarRecycler = view.findViewById(R.id.radar_recycler)

        radarView = RadarView(activity as MainActivity)
        radarSkyFrame.addView(radarView)

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
        private val linePaint = Paint().apply {
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

        override fun onDraw(canvas: Canvas) {
            var satPos: SatPos
            var satPassX: Float
            var satPassY: Float

            bmp.eraseColor(Color.TRANSPARENT)

            cvs.drawLine(center - radius, center, center + radius, center, linePaint)
            cvs.drawLine(center, center - radius, center, center + radius, linePaint)
            cvs.drawCircle(center, center, radius, linePaint)
            cvs.drawCircle(center, center, (radius / 3) * 2, linePaint)
            cvs.drawCircle(center, center, radius / 3, linePaint)

            cvs.drawText("N", center - txtSize / 3, center - radius - scale * 2, txtPaint)
            cvs.drawText("E", center + radius + scale * 2, center + txtSize / 3, txtPaint)
            cvs.drawText("S", center - txtSize / 3, center + radius + txtSize, txtPaint)
            cvs.drawText("W", center - radius - txtSize, center + txtSize / 3, txtPaint)
            cvs.drawText("0°", center + scale, center - scale * 2, txtPaint)
            cvs.drawText("30°", center + scale, center - (radius / 3) - scale * 2, txtPaint)
            cvs.drawText("60°", center + scale, center - ((radius / 3) * 2) - scale * 2, txtPaint)

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

            satPos = satPass.predictor.getSatPos(getCurrentDate())
            if (satPos.elevation > 0) {
                cvs.drawCircle(
                    center + sph2CartX(satPos.azimuth, satPos.elevation, radius.toDouble()),
                    center - sph2CartY(satPos.azimuth, satPos.elevation, radius.toDouble()),
                    txtSize / 3, satPaint
                )
            }
            canvas.drawBitmap(bmp, left.toFloat(), top.toFloat(), null)
        }

        private fun sph2CartX(azimuth: Double, elevation: Double, r: Double): Float {
            val radius = r * (piDiv2 - elevation) / piDiv2
            return (radius * cos(piDiv2 - azimuth)).toFloat()
        }

        private fun sph2CartY(azimuth: Double, elevation: Double, r: Double): Float {
            val radius = r * (piDiv2 - elevation) / piDiv2
            return (radius * sin(piDiv2 - azimuth)).toFloat()
        }

        private fun getCurrentDate(): Date {
            return Date()
        }
    }
}