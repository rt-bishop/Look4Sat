package com.rtbishop.look4sat.ui.fragments

import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.os.Bundle
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
import com.rtbishop.look4sat.data.SatOverlayItem
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
            val layers = listOf(FolderOverlay(), FolderOverlay(), FolderOverlay(), FolderOverlay())
            overlays.addAll(layers)
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
                    setupPosOverlay(stationPosition.data)
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

    private fun setupPosOverlay(gsp: GroundStationPosition) {
        Marker(binding.mapView).apply {
            setInfoWindow(null)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = resources.getDrawable(R.drawable.ic_map_pos, mainActivity.theme)
            position = GeoPoint(gsp.latitude, gsp.longitude)
            binding.mapView.overlays[0] = this
            binding.mapView.invalidate()
        }
    }

    private fun setupSatOverlay(passList: List<SatPass>) {
        lifecycleScope.launch {
            while (true) {
                val dateNow = Date(System.currentTimeMillis())
                binding.mapView.overlays[3] = getSatOverlay(passList, dateNow)
                binding.mapView.invalidate()
                delay(3000)
            }
        }
    }

    private suspend fun getSatOverlay(passList: List<SatPass>, dateNow: Date): Overlay =
        withContext(Dispatchers.Default) {
            val satIcon = resources.getDrawable(R.drawable.ic_map_sat, mainActivity.theme)
            val overlayItems = mutableListOf<SatOverlayItem>()

            passList.forEach {
                val satPos = it.predictor.getSatPos(dateNow)
                var lat = Math.toDegrees(satPos.latitude)
                var lon = Math.toDegrees(satPos.longitude)

                if (lat > 85.05) lat = 85.05
                else if (lat < -85.05) lat = -85.05
                if (lon > 180.0) lon -= 360.0

                val satItem = SatOverlayItem(it.tle.name, it.tle.name, GeoPoint(lat, lon), it)
                satItem.markerHotspot = OverlayItem.HotspotPlace.CENTER
                overlayItems.add(satItem)
            }

            val listener = object : ItemizedIconOverlay.OnItemGestureListener<SatOverlayItem> {
                override fun onItemLongPress(index: Int, item: SatOverlayItem): Boolean {
                    return true
                }

                override fun onItemSingleTapUp(index: Int, item: SatOverlayItem): Boolean {
                    setSatDetails(item.pass, dateNow)
                    return true
                }
            }

            return@withContext ItemizedIconOverlay<SatOverlayItem>(
                overlayItems,
                satIcon,
                listener,
                mainActivity
            )
        }

    private fun setSatDetails(pass: SatPass, dateNow: Date) {
        binding.mapView.overlays[1] = getSatTrack(pass, dateNow)
        binding.mapView.overlays[2] = getSatFootprint(pass, dateNow)
//        showSatInfo(pass)
        binding.mapView.invalidate()
    }

    private fun getSatTrack(pass: SatPass, dateNow: Date): Overlay {
        val period = (24 * 60 / pass.tle.meanmo).toInt()
        val positions = pass.predictor.getPositions(dateNow, 20, 0, period * 3)
        val trackOverlay = FolderOverlay()
        val trackPoints = mutableListOf<GeoPoint>()
        var oldLon = 0.0

        positions.forEach {
            val newLat = Math.toDegrees(it.latitude)
            var newLon = Math.toDegrees(it.longitude)
            if (newLon > 180.0) newLon -= 360.0
            if (oldLon < -170.0 && newLon > 170.0 || oldLon > 170.0 && newLon < -170.0) {
                val currentPoints = mutableListOf<GeoPoint>()
                currentPoints.addAll(trackPoints)
                Polyline().apply {
                    outlinePaint.set(trackPaint)
                    setPoints(currentPoints)
                    trackOverlay.add(this)
                }
                trackPoints.clear()
            }
            oldLon = newLon
            trackPoints.add(GeoPoint(newLat, newLon))
        }

        Polyline().apply {
            outlinePaint.set(trackPaint)
            setPoints(trackPoints)
            trackOverlay.add(this)
        }

        return trackOverlay
    }

    private fun getSatFootprint(pass: SatPass, dateNow: Date): Overlay {
        val rangePoints = mutableListOf<GeoPoint>()
        val rangeCircle = pass.predictor.getSatPos(dateNow).rangeCircle
        var zeroPoint = GeoPoint(0.0, 0.0)
        val satRange = Polygon().apply {
            fillPaint.set(rangePaint)
            outlinePaint.set(rangePaint)
        }
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