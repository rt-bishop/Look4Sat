package com.rtbishop.look4sat.ui.mainScreen

import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.github.amsacode.predict4java.Position
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.SharedViewModel
import com.rtbishop.look4sat.data.Result
import com.rtbishop.look4sat.data.SatPass
import com.rtbishop.look4sat.databinding.FragmentMapBinding
import com.rtbishop.look4sat.utility.PrefsManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.ITileSource
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

@AndroidEntryPoint
class MapFragment : Fragment(R.layout.fragment_map) {

    @Inject
    lateinit var prefsManager: PrefsManager

    private lateinit var binding: FragmentMapBinding
    private lateinit var trackPaint: Paint
    private lateinit var footprintPaint: Paint
    private lateinit var selectedPass: SatPass
    private lateinit var allPasses: List<SatPass>
    private val dateNow = Date()
    private val viewModel: SharedViewModel by activityViewModels()
    private var iconPos: Drawable? = null
    private var iconSat: Drawable? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMapBinding.bind(view)
        setupMapView()
        setupPosOverlay()
        setupObservers()
    }

    private fun setupMapView() {
        Configuration.getInstance()
            .load(requireContext().applicationContext, prefsManager.preferences)
        val filter = getColorFilter(ContextCompat.getColor(requireContext(), R.color.satTrack))
        binding.mapView.apply {
            setMultiTouchControls(true)
            setTileSource(getWikimediaTileSource())
            minZoomLevel = 2.5
            maxZoomLevel = 6.0
            controller.setZoom(3.0)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            overlayManager.tilesOverlay.loadingBackgroundColor = Color.TRANSPARENT
            overlayManager.tilesOverlay.loadingLineColor = Color.TRANSPARENT
            overlayManager.tilesOverlay.setColorFilter(filter)
            setScrollableAreaLimitDouble(BoundingBox(85.05, 180.0, -85.05, -180.0))

            // add overlays: 0 - GSP, 1 - SatTrack, 2 - SatFootprint, 3 - SatIcons
            val layers = listOf(FolderOverlay(), FolderOverlay(), FolderOverlay(), FolderOverlay())
            overlays.addAll(layers)
        }
        trackPaint = Paint().apply {
            strokeWidth = 2f
            style = Paint.Style.STROKE
            color = ContextCompat.getColor(requireContext(), R.color.satTrack)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }
        footprintPaint = Paint().apply {
            style = Paint.Style.FILL_AND_STROKE
            color = ContextCompat.getColor(requireContext(), R.color.satFootprint)
            isAntiAlias = true
        }
        iconPos = ContextCompat.getDrawable(requireContext(), R.drawable.ic_map_pos)
        iconSat = ContextCompat.getDrawable(requireContext(), R.drawable.ic_map_sat)
    }

    private fun getWikimediaTileSource(): ITileSource {
        val copyright = resources.getString(R.string.map_copyright)
        val sources = arrayOf("https://maps.wikimedia.org/osm-intl/")
        val policy = TileSourcePolicy(
            1, TileSourcePolicy.FLAG_NO_BULK and TileSourcePolicy.FLAG_NO_PREVENTIVE and
                    TileSourcePolicy.FLAG_USER_AGENT_MEANINGFUL and TileSourcePolicy.FLAG_USER_AGENT_NORMALIZED
        )
        return XYTileSource("wikimedia", 2, 6, 256, ".png", sources, copyright, policy)
    }

    private fun setupPosOverlay() {
        val gsp = prefsManager.getStationPosition()
        Marker(binding.mapView).apply {
            setInfoWindow(null)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = iconPos
            position = GeoPoint(gsp.latitude, gsp.longitude)
            binding.mapView.overlays[0] = this
            binding.mapView.invalidate()
        }
    }

    private fun setupObservers() {
        viewModel.getPasses().observe(viewLifecycleOwner, { satPasses ->
            if (satPasses is Result.Success) {
                if (satPasses.data.isNotEmpty()) {
                    allPasses = satPasses.data.distinctBy { it.tle }
                    selectedPass = allPasses[0]
                    binding.fabPrev.setOnClickListener { changeSelection(true) }
                    binding.fabNext.setOnClickListener { changeSelection(false) }
                    setupSatOverlay(allPasses)
                }
            }
        })
    }

    private fun setupSatOverlay(passList: List<SatPass>) {
        lifecycleScope.launch {
            while (true) {
                dateNow.time = System.currentTimeMillis()
                setSatDetails(selectedPass)
                binding.mapView.overlays[3] = getSatMarkers(passList)
                binding.mapView.invalidate()
                delay(2000)
            }
        }
    }

    private fun changeSelection(decrement: Boolean) {
        val index = allPasses.indexOf(selectedPass)
        selectedPass = if (decrement) {
            if (index > 0) allPasses[index - 1] else allPasses[allPasses.size - 1]
        } else {
            if (index < allPasses.size - 1) allPasses[index + 1] else allPasses[0]
        }
        val satPos = selectedPass.predictor.getSatPos(dateNow)
        val osmPos = getOsmPosition(satPos.latitude, satPos.longitude, true)
        setSatDetails(selectedPass)
        binding.mapView.controller.animateTo(GeoPoint(osmPos.lat, osmPos.lon))
    }

    private suspend fun getSatMarkers(passList: List<SatPass>): Overlay =
        withContext(Dispatchers.Default) {
            val items = FolderOverlay()
            passList.forEach { satPass ->
                val satPos = satPass.predictor.getSatPos(dateNow)
                val osmPos = getOsmPosition(satPos.latitude, satPos.longitude, true)
                Marker(binding.mapView).apply {
                    setInfoWindow(null)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = iconSat
                    position = GeoPoint(osmPos.lat, osmPos.lon)
                    setOnMarkerClickListener { _, _ ->
                        selectedPass = satPass
                        setSatDetails(satPass)
                        true
                    }
                    items.add(this)
                }
            }
            return@withContext items
        }

    private fun setSatDetails(satPass: SatPass) {
        setSatInfo(satPass)
        binding.mapView.overlays[1] = getSatTrack(satPass)
        binding.mapView.overlays[2] = getSatFootprint(satPass)
    }

    private fun setSatInfo(satPass: SatPass) {
        val satPos = satPass.predictor.getSatPos(dateNow)
        val osmPos = getOsmPosition(satPos.latitude, satPos.longitude, true)
        val catNum = satPass.tle.catnum
        val name = satPass.tle.name
        val satVelocity = getSatVelocity(satPos.altitude)
        binding.apply {
            idName.text = String.format(getString(R.string.pat_osm_idName), catNum, name)
            altitude.text = String.format(getString(R.string.pat_altitude), satPos.altitude)
            distance.text = String.format(getString(R.string.pat_distance), satPos.range)
            velocity.text = String.format(getString(R.string.pat_osm_vel), satVelocity)
            latLon.text = String.format(getString(R.string.pat_osm_latLon), osmPos.lat, osmPos.lon)
        }
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
            val osmPos = getOsmPosition(it.latitude, it.longitude, true)
            if (oldLon < -170.0 && osmPos.lon > 170.0 || oldLon > 170.0 && osmPos.lon < -170.0) {
                val currentPoints = mutableListOf<GeoPoint>()
                currentPoints.addAll(trackPoints)
                Polyline().apply {
                    outlinePaint.set(trackPaint)
                    setPoints(currentPoints)
                    trackOverlay.add(this)
                }
                trackPoints.clear()
            }
            oldLon = osmPos.lon
            trackPoints.add(GeoPoint(osmPos.lat, osmPos.lon))
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
            val osmPos = getOsmPosition(it.value.lat, it.value.lon, false)
            if (it.index == 0) zeroPoint = GeoPoint(osmPos.lat, osmPos.lon)
            rangePoints.add(GeoPoint(osmPos.lat, osmPos.lon))
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

    private fun getOsmPosition(lat: Double, lon: Double, inRadians: Boolean): Position {
        return if (inRadians) {
            var osmLat = Math.toDegrees(lat)
            var osmLon = Math.toDegrees(lon)
            if (osmLat > 85.05) osmLat = 85.05 else if (osmLat < -85.05) osmLat = -85.05
            if (osmLon > 180f) osmLon -= 360f
            Position(osmLat, osmLon)
        } else {
            val osmLat = if (lat > 85.05) 85.05 else if (lat < -85.05) -85.05 else lat
            val osmLon = if (lon > 180.0) lon - 360.0 else lon
            Position(osmLat, osmLon)
        }
    }
}