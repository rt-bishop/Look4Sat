package com.rtbishop.look4sat.ui.fragments

import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.github.amsacode.predict4java.GroundStationPosition
import com.rtbishop.look4sat.Look4SatApp
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.dagger.ViewModelFactory
import com.rtbishop.look4sat.data.Result
import com.rtbishop.look4sat.data.SatPass
import com.rtbishop.look4sat.databinding.FragmentMapOsmBinding
import com.rtbishop.look4sat.ui.MainActivity
import com.rtbishop.look4sat.ui.SharedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.*
import org.osmdroid.views.overlay.gridlines.LatLonGridlineOverlay2
import java.util.*
import javax.inject.Inject

class MapOsmFragment : Fragment(R.layout.fragment_map_osm) {

    @Inject
    lateinit var modelFactory: ViewModelFactory
    private lateinit var mainActivity: MainActivity
    private lateinit var viewModel: SharedViewModel
    private lateinit var binding: FragmentMapOsmBinding
    private var satLayer: FolderOverlay = FolderOverlay()
    private var groundTrackLayer = FolderOverlay()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMapOsmBinding.bind(view)
        mainActivity = requireActivity() as MainActivity
        (mainActivity.application as Look4SatApp).appComponent.inject(this)
        viewModel = ViewModelProvider(mainActivity, modelFactory).get(SharedViewModel::class.java)

        val prefs = PreferenceManager.getDefaultSharedPreferences(mainActivity.applicationContext)
        Configuration.getInstance().load(mainActivity.applicationContext, prefs)

        setupMapView()
        setupObservers()
    }

    private fun setupMapView() {
        binding.mapView.apply {
            setMultiTouchControls(true)
            setTileSource(TileSourceFactory.MAPNIK)
            minZoomLevel = 2.5
            maxZoomLevel = 6.0
            controller.setZoom(minZoomLevel)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false
            overlayManager.tilesOverlay.loadingBackgroundColor = Color.TRANSPARENT
            overlayManager.tilesOverlay.loadingLineColor = Color.TRANSPARENT
            setScrollableAreaLimitDouble(BoundingBox(85.0, 180.0, -85.0, -180.0))

            addColorFilter()
        }
    }

    private fun setupObservers() {
        viewModel.getGSP().observe(viewLifecycleOwner, Observer { stationPosition ->
            when (stationPosition) {
                is Result.Success -> {
                    setUserLocation(stationPosition.data)
                }
            }
        })

        viewModel.getPassList().observe(viewLifecycleOwner, Observer { satPasses ->
            when (satPasses) {
                is Result.Success -> {
                    val filteredPasses = satPasses.data.distinctBy { it.tle }
                    setupSatOverlay(filteredPasses)
                }
            }
        })
    }

    private fun setUserLocation(position: GroundStationPosition) {
        val startPoint = GeoPoint(position.latitude, position.longitude)
        val positionMarker = Marker(binding.mapView).apply {
            this.position = startPoint
            textLabelBackgroundColor = Color.TRANSPARENT
            textLabelForegroundColor = Color.WHITE
            textLabelFontSize = 24
            setTextIcon("GSP")
            setInfoWindow(null)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        binding.mapView.overlays.add(positionMarker)
    }

    private fun setupSatOverlay(passList: List<SatPass>) {
        lifecycleScope.launch {
            while (true) {
                val satMarkers = getSatMarkers(passList)
                binding.mapView.overlays.remove(satLayer)
                satLayer = FolderOverlay()
                satMarkers.forEach { satLayer.add(it) }
                binding.mapView.overlays.add(satLayer)
                binding.mapView.invalidate()
                delay(3000)
            }
        }
    }

    private suspend fun getSatMarkers(passList: List<SatPass>): List<Overlay> =
        withContext(Dispatchers.Default) {
            val satMarkers = mutableListOf<Overlay>()
            val dateNow = Date(System.currentTimeMillis())
            passList.forEach {
                val satMarker = getMarkerForPass(it, dateNow)
                satMarkers.add(satMarker)
            }
            return@withContext satMarkers
        }

    private fun getMarkerForPass(pass: SatPass, date: Date): Marker {
        val satPos = pass.predictor.getSatPos(date)
        val lat = Math.toDegrees(satPos.latitude)
        var lon = Math.toDegrees(satPos.longitude)
        if (lon > 180) lon -= 360
        return Marker(binding.mapView).apply {
            position = GeoPoint(lat, lon)
            textLabelBackgroundColor = Color.TRANSPARENT
            textLabelForegroundColor = Color.WHITE
            textLabelFontSize = 24
            setTextIcon(pass.tle.name)
            setInfoWindow(null)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            setOnMarkerClickListener { _, _ ->
                showSatGroundTrack(pass)
                return@setOnMarkerClickListener true
            }
        }
    }

    private fun showSatGroundTrack(pass: SatPass) {
        val dateNow = Date(System.currentTimeMillis())
        val period = (24 * 60 / pass.tle.meanmo).toInt()
        val positions = pass.predictor.getPositions(dateNow, 15, 3, period)
        val track = Polyline(binding.mapView)
        val trackPoints = mutableListOf<GeoPoint>()

        val trackPaint = Paint().apply {
            strokeWidth = mainActivity.resources.displayMetrics.scaledDensity
            style = Paint.Style.FILL_AND_STROKE
            color = Color.RED
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }

        track.outlinePaint.set(trackPaint)

        positions.forEach {
            val lat = Math.toDegrees(it.latitude)
            var lon = Math.toDegrees(it.longitude)
            if (lon > 180.0) lon -= 360.0
            trackPoints.add(GeoPoint(lat, lon))
        }

        track.setPoints(trackPoints)
        groundTrackLayer = FolderOverlay()
        groundTrackLayer.add(track)
        binding.mapView.overlays.add(groundTrackLayer)
        binding.mapView.invalidate()
    }

    private fun addGridLineOverlay() {
        val overlay = LatLonGridlineOverlay2()
        overlay.setBackgroundColor(Color.TRANSPARENT)
        overlay.setFontColor(Color.WHITE)
        overlay.setFontSizeDp(18)
        binding.mapView.overlays.add(overlay)
    }

    private fun addMapScaleBarOverlay() {
        val dm = mainActivity.resources.displayMetrics
        val overlay = ScaleBarOverlay(binding.mapView).apply {
            setCentred(true)
            setScaleBarOffset(dm.widthPixels / 2, 10)
        }
        binding.mapView.overlays.add(overlay)
    }

    private fun addSomeIcons() {
        val items = mutableListOf<OverlayItem>()
        items.add(OverlayItem("Title", "Description", GeoPoint(0.0, 0.0)))

        val listener = object : ItemizedIconOverlay.OnItemGestureListener<OverlayItem> {
            override fun onItemLongPress(index: Int, item: OverlayItem?): Boolean {
                return true
            }

            override fun onItemSingleTapUp(index: Int, item: OverlayItem?): Boolean {
                Log.d("myTag", "${item?.title}")
                return true
            }
        }

        val overlay = ItemizedIconOverlay(mainActivity, items, listener)
        binding.mapView.overlays.add(overlay)
    }

    private fun addColorFilter() {

        val negativeArray = floatArrayOf(
            -1.0f, .0f, .0f, .0f, 255.0f,
            .0f, -1.0f, .0f, .0f, 255.0f,
            .0f, .0f, -1.0f, .0f, 255.0f,
            .0f, .0f, .0f, 1.0f, .0f
        )
        val negativeMatrix = ColorMatrix(negativeArray)

        val destinationColor = Color.parseColor("#FF2A2A2A")
        val lr = (255.0f - Color.red(destinationColor)) / 255.0f
        val lg = (255.0f - Color.green(destinationColor)) / 255.0f
        val lb = (255.0f - Color.blue(destinationColor)) / 255.0f
        val grayScaleArray = floatArrayOf(
            lr, lg, lb, 0f, 0f, //
            lr, lg, lb, 0f, 0f, //
            lr, lg, lb, 0f, 0f, //
            0f, 0f, 0f, 0f, 255f //
        )
        val grayScaleMatrix = ColorMatrix(grayScaleArray)
        grayScaleMatrix.preConcat(negativeMatrix)

        val dr = Color.red(destinationColor)
        val dg = Color.green(destinationColor)
        val db = Color.blue(destinationColor)
        val drf = dr / 255f
        val dgf = dg / 255f
        val dbf = db / 255f
        val tintArray = floatArrayOf(
            drf, 0f, 0f, 0f, 0f, //
            0f, dgf, 0f, 0f, 0f, //
            0f, 0f, dbf, 0f, 0f, //
            0f, 0f, 0f, 1f, 0f //
        )
        val tintMatrix = ColorMatrix(tintArray)
        tintMatrix.preConcat(grayScaleMatrix)

        val lDest = drf * lr + dgf * lg + dbf * lb
        val scale = 1f - lDest
        val translate = 1 - scale * 0.5f

        val scaleArray = floatArrayOf(
            scale, 0f, 0f, 0f, dr * translate, //
            0f, scale, 0f, 0f, dg * translate, //
            0f, 0f, scale, 0f, db * translate, //
            0f, 0f, 0f, 1f, 0f
        )
        val scaleMatrix = ColorMatrix(scaleArray)
        scaleMatrix.preConcat(tintMatrix)

        val filter = ColorMatrixColorFilter(scaleMatrix)

        binding.mapView.overlayManager.tilesOverlay.setColorFilter(filter)
    }

    private fun drawPosition(position: GeoPoint) {
        val satPosition = Polygon(binding.mapView)
        val trackPoints = mutableListOf<GeoPoint>()

        val lat = Math.toDegrees(position.latitude)
        var lon = Math.toDegrees(position.longitude)
        if (lon > 180.0) lon -= 360.0
        trackPoints.add(GeoPoint(lat, lon))

        val startMarker = Marker(binding.mapView)
        startMarker.position = GeoPoint(lat, lon)
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        startMarker.title = "Sat Position"
        binding.mapView.overlays.add(startMarker)

        binding.mapView.overlays.add(satPosition)
        satPosition.points = trackPoints
        binding.mapView.invalidate()
    }

    override fun onPause() {
        binding.mapView.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }
}