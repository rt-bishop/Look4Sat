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
import com.github.amsacode.predict4java.SatPos
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
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourcePolicy
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.*
import java.util.*
import javax.inject.Inject

class MapOsmFragment : Fragment(R.layout.fragment_map_osm) {

    @Inject
    lateinit var modelFactory: ViewModelFactory
    private lateinit var mainActivity: MainActivity
    private lateinit var viewModel: SharedViewModel
    private lateinit var binding: FragmentMapOsmBinding

    private var gspOverlay = FolderOverlay()
    private var satTrackOverlay = FolderOverlay()
    private var satRangeOverlay = FolderOverlay()
    private var satNameOverlay = FolderOverlay()

    private val trackPaint = Paint().apply {
        strokeWidth = 2f
        style = Paint.Style.STROKE
        color = Color.parseColor("#D50000")
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    private val rangePaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = Color.parseColor("#26FFE082")
        isAntiAlias = true
    }

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
            setTileSource(getWikimediaTileSource())
            minZoomLevel = 2.5
            maxZoomLevel = 6.0
            controller.setZoom(minZoomLevel)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false
            overlayManager.tilesOverlay.loadingBackgroundColor = Color.TRANSPARENT
            overlayManager.tilesOverlay.loadingLineColor = Color.TRANSPARENT
            setScrollableAreaLimitDouble(BoundingBox(85.05, 180.0, -85.05, -180.0))

            // apply filter
            val tilesFilter = getColorFilter(Color.parseColor("#D50000"))
            overlayManager.tilesOverlay.setColorFilter(tilesFilter)

            // fill overlays
            overlays.add(0, gspOverlay)
            overlays.add(1, satTrackOverlay)
            overlays.add(2, satRangeOverlay)
            overlays.add(3, satNameOverlay)
        }
    }

    private fun getWikimediaTileSource(): OnlineTileSourceBase {
        val wikimediaSourceArray = arrayOf("https://maps.wikimedia.org/osm-intl/")
        val wikimediaCopyright = resources.getString(R.string.osmCopyright)
        val wikimediaSourcePolicy = TileSourcePolicy(
            1, TileSourcePolicy.FLAG_NO_BULK and TileSourcePolicy.FLAG_NO_PREVENTIVE and
                    TileSourcePolicy.FLAG_USER_AGENT_MEANINGFUL and TileSourcePolicy.FLAG_USER_AGENT_NORMALIZED
        )
        return XYTileSource(
            "wikimedia",
            2,
            6,
            256,
            ".png",
            wikimediaSourceArray,
            wikimediaCopyright,
            wikimediaSourcePolicy
        )
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
            textLabelForegroundColor = Color.YELLOW
            textLabelFontSize = 24
            setTextIcon("GSP")
            setInfoWindow(null)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        binding.mapView.overlays[0] = positionMarker
        binding.mapView.invalidate()
    }

    private fun setupSatOverlay(passList: List<SatPass>) {
        lifecycleScope.launch {
            while (true) {
                val satRanges = getSatRanges(passList)
                satRanges.forEach { satRangeOverlay.add(it) }
                binding.mapView.overlays[2] = satRangeOverlay
                satRangeOverlay = FolderOverlay()

                val satMarkers = getSatMarkers(passList)
                satMarkers.forEach { satNameOverlay.add(it) }
                binding.mapView.overlays[3] = satNameOverlay
                satNameOverlay = FolderOverlay()

                binding.mapView.invalidate()
                delay(3000)
            }
        }
    }

    private suspend fun getSatRanges(passList: List<SatPass>): List<Overlay> =
        withContext(Dispatchers.Default) {
            val satRanges = mutableListOf<Overlay>()
            val dateNow = Date(System.currentTimeMillis())
            passList.forEach {
                val satRange = getRangeForPass(it, dateNow)
                satRanges.add(satRange)
            }
            return@withContext satRanges
        }

    private fun getRangeForPass(it: SatPass, dateNow: Date): Overlay {
        val satRange = Polygon().apply {
            fillPaint.set(rangePaint)
            outlinePaint.set(rangePaint)
        }
        val rangePoints = mutableListOf<GeoPoint>()
        val rangeCircle = it.predictor.getSatPos(dateNow).rangeCircle

        var zeroPoint = GeoPoint(0.0, 0.0)
        rangeCircle.withIndex().forEach {
            var lat = it.value.lat
            var lon = it.value.lon

            if (lat > 85.05) lat = 85.05
            else if (lat < -85.05) lat = -85.05
            if (lon > 180.0) lon -= 360.0

            if (it.index == 0) zeroPoint = GeoPoint(lat, lon)
            rangePoints.add(GeoPoint(lat, lon))
        }
        rangePoints.add(zeroPoint)
        satRange.points = rangePoints

        return satRange
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
        var lat = Math.toDegrees(satPos.latitude)
        var lon = Math.toDegrees(satPos.longitude)

        if (lat > 85.05) lat = 85.05
        else if (lat < -85.05) lat = -85.05
        if (lon > 180.0) lon -= 360.0

        return Marker(binding.mapView).apply {
            position = GeoPoint(lat, lon)
            textLabelBackgroundColor = Color.TRANSPARENT
            textLabelForegroundColor = Color.YELLOW
            textLabelFontSize = 24
            setTextIcon(pass.tle.name)
            setInfoWindow(null)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            setOnMarkerClickListener { _, _ ->
                val period = (24 * 60 / pass.tle.meanmo).toInt()
                val positions = pass.predictor.getPositions(date, 20, 0, period * 3)
                showSatGroundTrack(positions)
                return@setOnMarkerClickListener true
            }
        }
    }

    private fun showSatGroundTrack(positions: List<SatPos>) {
        val trackPoints = mutableListOf<GeoPoint>()
        var oldLon = 0.0
        positions.forEach {
            val newLat = Math.toDegrees(it.latitude)
            var newLon = Math.toDegrees(it.longitude)
            if (newLon > 180.0) newLon -= 360.0

            if (oldLon < -170.0 && newLon > 170.0 || oldLon > 170.0 && newLon < -170.0) {
                val currentPoints = mutableListOf<GeoPoint>()
                currentPoints.addAll(trackPoints)
                val completeTrack = Polyline()
                completeTrack.outlinePaint.set(trackPaint)
                completeTrack.setPoints(currentPoints)
                satTrackOverlay.add(completeTrack)
                trackPoints.clear()
            }

            oldLon = newLon
            trackPoints.add(GeoPoint(newLat, newLon))
        }

        val completeTrack = Polyline()
        completeTrack.outlinePaint.set(trackPaint)
        completeTrack.setPoints(trackPoints)
        satTrackOverlay.add(completeTrack)
        binding.mapView.overlays[1] = satTrackOverlay
        satTrackOverlay = FolderOverlay()
        binding.mapView.invalidate()
    }

    private fun getItemizedOverlay(): Overlay {
        val items = mutableListOf<OverlayItem>()
        items.add(OverlayItem("Title", "Description", GeoPoint(0.0, 0.0)))

        val listener = object : ItemizedIconOverlay.OnItemGestureListener<OverlayItem> {
            override fun onItemLongPress(index: Int, item: OverlayItem): Boolean {
                return true
            }

            override fun onItemSingleTapUp(index: Int, item: OverlayItem): Boolean {
                Log.d("myTag", item.title)
                return true
            }
        }

        return ItemizedIconOverlay<OverlayItem>(mainActivity, items, listener)
    }

    private fun getColorFilter(targetColor: Int): ColorMatrixColorFilter {
        val newR = Color.red(targetColor) / 255f
        val newG = Color.green(targetColor) / 255f
        val newB = Color.blue(targetColor) / 255f

        val negativeMatrix = ColorMatrix(
            floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            )
        )

        val tintedMatrix = ColorMatrix(
            floatArrayOf(
                newR, newG, newB, 0f, 0f,
                newR, newG, newB, 0f, 0f,
                newR, newG, newB, 0f, 0f,
                0f, 0f, 0f, 0f, 255f
            )
        )

        tintedMatrix.preConcat(negativeMatrix)
        return ColorMatrixColorFilter(tintedMatrix)
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