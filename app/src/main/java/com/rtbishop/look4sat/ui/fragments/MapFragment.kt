package com.rtbishop.look4sat.ui.fragments

import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.github.amsacode.predict4java.GroundStationPosition
import com.rtbishop.look4sat.Look4SatApp
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.dagger.ViewModelFactory
import com.rtbishop.look4sat.data.Result
import com.rtbishop.look4sat.data.SatItem
import com.rtbishop.look4sat.data.SatPass
import com.rtbishop.look4sat.databinding.FragmentMapBinding
import com.rtbishop.look4sat.ui.SharedViewModel
import com.rtbishop.look4sat.utility.PrefsManager
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
import kotlin.math.pow
import kotlin.math.sqrt

class MapFragment : Fragment(R.layout.fragment_map) {

    @Inject
    lateinit var factory: ViewModelFactory

    @Inject
    lateinit var prefsManager: PrefsManager

    private lateinit var mainActivity: FragmentActivity
    private lateinit var binding: FragmentMapBinding
    private lateinit var trackPaint: Paint
    private lateinit var footprintPaint: Paint
    private lateinit var selectedPass: SatPass
    private val dateNow = Date(System.currentTimeMillis())
    private val viewModel: SharedViewModel by activityViewModels { factory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMapBinding.bind(view)
        mainActivity = requireActivity()
        (mainActivity.application as Look4SatApp).appComponent.inject(this)

        val prefs = PreferenceManager.getDefaultSharedPreferences(mainActivity.applicationContext)
        Configuration.getInstance().load(mainActivity.applicationContext, prefs)

        trackPaint = Paint().apply {
            strokeWidth = 2f
            style = Paint.Style.STROKE
            color = ContextCompat.getColor(mainActivity, R.color.satTrack)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }

        footprintPaint = Paint().apply {
            style = Paint.Style.FILL_AND_STROKE
            color = ContextCompat.getColor(mainActivity, R.color.satOsmTemp)
            isAntiAlias = true
        }

        setupMapView()
        setupPosOverlay(prefsManager.getStationPosition())
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

            // add overlays: 0 - GSP, 1 - SatTrack, 2 - SatFootprint, 3 - SatIcons
            val layers = listOf(FolderOverlay(), FolderOverlay(), FolderOverlay(), FolderOverlay())
            overlays.addAll(layers)
        }
    }

    private fun getWikimediaTileSource(): OnlineTileSourceBase {
        return XYTileSource(
            "wikimedia",
            2,
            6,
            256,
            ".png",
            arrayOf("https://maps.wikimedia.org/osm-intl/"),
            resources.getString(R.string.osmCopyright),
            TileSourcePolicy(
                1, TileSourcePolicy.FLAG_NO_BULK and TileSourcePolicy.FLAG_NO_PREVENTIVE and
                        TileSourcePolicy.FLAG_USER_AGENT_MEANINGFUL and TileSourcePolicy.FLAG_USER_AGENT_NORMALIZED
            )
        )
    }

    private fun setupObservers() {
        viewModel.getPasses().observe(viewLifecycleOwner, { satPasses ->
            when (satPasses) {
                is Result.Success -> {
                    if (satPasses.data.isNotEmpty()) {
                        val filteredPasses = satPasses.data.distinctBy { it.tle }
                        selectedPass = filteredPasses[0]
                        setupSatOverlay(filteredPasses)
                    }
                }
            }
        })
    }

    private fun setupPosOverlay(gsp: GroundStationPosition) {
        Marker(binding.mapView).apply {
            setInfoWindow(null)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ResourcesCompat.getDrawable(resources, R.drawable.ic_map_pos, mainActivity.theme)
            position = GeoPoint(gsp.latitude, gsp.longitude)
            binding.mapView.overlays[0] = this
            binding.mapView.invalidate()
        }
    }

    private fun setupSatOverlay(passList: List<SatPass>) {
        lifecycleScope.launch {
            while (true) {
                dateNow.time = System.currentTimeMillis()
                binding.mapView.overlays[3] = getSatIcons(passList)
                binding.mapView.overlays[2] = getSatFootprint(selectedPass)
                setSatInfo(selectedPass)
                binding.mapView.invalidate()
                delay(3000)
            }
        }
    }

    private suspend fun getSatIcons(passList: List<SatPass>): Overlay =
        withContext(Dispatchers.Default) {
            val icon =
                ResourcesCompat.getDrawable(resources, R.drawable.ic_map_sat, mainActivity.theme)
            val items = mutableListOf<SatItem>()

            passList.forEach {
                val satPos = it.predictor.getSatPos(dateNow)
                var lat = Math.toDegrees(satPos.latitude)
                var lon = Math.toDegrees(satPos.longitude)

                if (lat > 85.05) lat = 85.05
                else if (lat < -85.05) lat = -85.05
                if (lon > 180.0) lon -= 360.0

                SatItem(it.tle.name, it.tle.name, GeoPoint(lat, lon), it).apply {
                    markerHotspot = OverlayItem.HotspotPlace.CENTER
                    items.add(this)
                }
            }

            val listener = object : ItemizedIconOverlay.OnItemGestureListener<SatItem> {
                override fun onItemLongPress(index: Int, item: SatItem): Boolean {
                    return true
                }

                override fun onItemSingleTapUp(index: Int, item: SatItem): Boolean {
                    selectedPass = item.pass
                    setSatDetails(item.pass)
                    return true
                }
            }

            return@withContext ItemizedIconOverlay(items, icon, listener, mainActivity)
        }

    private fun setSatDetails(satPass: SatPass) {
        setSatInfo(satPass)
        binding.mapView.overlays[1] = getSatTrack(satPass)
        binding.mapView.overlays[2] = getSatFootprint(satPass)
        binding.mapView.invalidate()
    }

    private fun setSatInfo(satPass: SatPass) {
        val satPos = satPass.predictor.getSatPos(dateNow)
        val satLat = Math.toDegrees(satPos.latitude).toFloat()
        var satLon = Math.toDegrees(satPos.longitude).toFloat()
        if (satLon > 180f) satLon -= 360f

        binding.idName.text =
            String.format(
                mainActivity.getString(R.string.pat_osm_idName),
                satPass.tle.catnum,
                satPass.tle.name
            )
        binding.altitude.text =
            String.format(mainActivity.getString(R.string.pat_altitude), satPos.altitude)
        binding.distance.text =
            String.format(mainActivity.getString(R.string.pat_distance), satPos.range)
        binding.velocity.text =
            String.format(
                mainActivity.getString(R.string.pat_osm_vel),
                getSatVelocity(satPos.altitude)
            )
        binding.latLon.text =
            String.format(mainActivity.getString(R.string.pat_osm_latLon), satLat, satLon)
    }

    private fun getSatVelocity(satAlt: Double): Double {
        val earthG = 6.674 * 10.0.pow(-11)
        val earthM = 5.98 * 10.0.pow(24)
        val radius = 6.37 * 10.0.pow(6) + satAlt * 10.0.pow(3)
        return sqrt(earthG * earthM / radius) / 1000
    }

    private fun getSatTrack(pass: SatPass): Overlay {
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

    private fun getSatFootprint(pass: SatPass): Overlay {
        val rangeCircle = pass.predictor.getSatPos(dateNow).rangeCircle
        val rangePoints = mutableListOf<GeoPoint>()
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

        return Polygon().apply {
            fillPaint.set(footprintPaint)
            outlinePaint.set(footprintPaint)
            points = rangePoints
        }
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