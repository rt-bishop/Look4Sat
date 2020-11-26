package com.rtbishop.look4sat.ui.mainScreen

import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.github.amsacode.predict4java.GroundStationPosition
import com.github.amsacode.predict4java.Position
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.SharedViewModel
import com.rtbishop.look4sat.data.Result
import com.rtbishop.look4sat.data.SatPass
import com.rtbishop.look4sat.databinding.FragmentMapBinding
import dagger.hilt.android.AndroidEntryPoint
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

@AndroidEntryPoint
class MapFragment : Fragment(R.layout.fragment_map) {

    @Inject
    lateinit var preferences: SharedPreferences
    private lateinit var trackPaint: Paint
    private lateinit var footprintPaint: Paint
    private lateinit var nameFormat: String
    private lateinit var altFormat: String
    private lateinit var distFormat: String
    private lateinit var velFormat: String
    private lateinit var latLonFormat: String
    private lateinit var selectedSat: SatPass
    private val dateNow = Date()
    private val viewModel: MapViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var binding: FragmentMapBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupComponents(view)
        setupMapView()
        setupObservers()
    }

    private fun setupComponents(view: View) {
        Configuration.getInstance().load(requireContext().applicationContext, preferences)
        binding = FragmentMapBinding.bind(view)
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
        nameFormat = getString(R.string.pat_osm_idName)
        altFormat = getString(R.string.pat_altitude)
        distFormat = getString(R.string.pat_distance)
        velFormat = getString(R.string.pat_osm_vel)
        latLonFormat = getString(R.string.pat_osm_latLon)
    }

    private fun setupMapView() {
        binding?.apply {
            mapView.apply {
                setMultiTouchControls(true)
                setTileSource(getTileSource())
                minZoomLevel = 2.5
                maxZoomLevel = 6.0
                controller.setZoom(3.0)
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                overlayManager.tilesOverlay.loadingBackgroundColor = Color.TRANSPARENT
                overlayManager.tilesOverlay.loadingLineColor = Color.TRANSPARENT
                overlayManager.tilesOverlay.setColorFilter(getColorFilter())
                setScrollableAreaLimitDouble(BoundingBox(85.05, 180.0, -85.05, -180.0))
                // add overlays: 0 - GSP, 1 - SatTrack, 2 - SatFootprint, 3 - SatIcons
                overlays.addAll(Array(4) { FolderOverlay() })
            }
        }
    }

    private fun setupObservers() {
        viewModel.getGSP().observe(viewLifecycleOwner, { setupPosOverlay(it) })
        sharedViewModel.getPasses().observe(viewLifecycleOwner, {
            if (it is Result.Success && it.data.isNotEmpty()) {
                viewModel.setPasses(it.data)
                binding?.fabPrev?.setOnClickListener { viewModel.changeSelection(true) }
                binding?.fabNext?.setOnClickListener { viewModel.changeSelection(false) }
            }
        })
        viewModel.getSelectedPass().observe(viewLifecycleOwner, { satPass ->
            satPass?.let {
                selectedSat = satPass
                setSelectedSatDetails(satPass)
                scrollToSat(satPass)
            }
        })
        viewModel.getSatMarkers().observe(viewLifecycleOwner, {
            setMarkers(it)
            setSelectedSatDetails(selectedSat)
        })
    }

    private fun setupPosOverlay(gsp: GroundStationPosition) {
        binding?.apply {
            Marker(mapView).apply {
                setInfoWindow(null)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_map_pos)
                position = GeoPoint(gsp.latitude, gsp.longitude)
                mapView.overlays[0] = this
                mapView.invalidate()
            }
        }
    }

    private fun setSelectedSatDetails(satPass: SatPass) {
        val satPos = satPass.predictor.getSatPos(dateNow)
        val osmPos = viewModel.getOsmPosition(satPos.latitude, satPos.longitude, true)
        binding?.apply {
            idName.text = String.format(nameFormat, satPass.tle.catnum, satPass.tle.name)
            altitude.text = String.format(altFormat, satPos.altitude)
            distance.text = String.format(distFormat, satPos.range)
            velocity.text = String.format(velFormat, viewModel.getSatVelocity(satPos.altitude))
            latLon.text = String.format(latLonFormat, osmPos.lat, osmPos.lon)
            mapView.overlays[1] = getSatTrack(satPass)
            mapView.overlays[2] = getSatFootprint(satPass)
            mapView.invalidate()
        }
    }

    private fun scrollToSat(satPass: SatPass) {
        val satPos = satPass.predictor.getSatPos(dateNow)
        val osmPos = viewModel.getOsmPosition(satPos.latitude, satPos.longitude, true)
        binding?.mapView?.controller?.animateTo(GeoPoint(osmPos.lat, osmPos.lon))
    }

    private fun setMarkers(map: Map<SatPass, Position>) {
        dateNow.time = System.currentTimeMillis()
        binding?.apply {
            val markers = FolderOverlay()
            map.entries.forEach {
                Marker(mapView).apply {
                    setInfoWindow(null)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_map_sat)
                    position = GeoPoint(it.value.lat, it.value.lon)
                    setOnMarkerClickListener { _, _ -> viewModel.setSelectedSat(it.key) }
                    markers.add(this)
                }
            }
            mapView.overlays[3] = markers
            mapView.invalidate()
        }
    }

    private fun getSatTrack(pass: SatPass): Overlay {
        val period = (24 * 60 / pass.tle.meanmo).toInt()
        val positions = pass.predictor.getPositions(dateNow, 20, 0, period * 3)
        val trackOverlay = FolderOverlay()
        val trackPoints = mutableListOf<GeoPoint>()
        var oldLon = 0.0

        positions.forEach {
            val osmPos = viewModel.getOsmPosition(it.latitude, it.longitude, true)
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
            val osmPos = viewModel.getOsmPosition(it.value.lat, it.value.lon, false)
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

    private fun getColorFilter(): ColorMatrixColorFilter {
        val targetColor = ContextCompat.getColor(requireContext(), R.color.satTrack)
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

    private fun getTileSource(): ITileSource {
        val copyright = resources.getString(R.string.map_copyright)
        val sources = arrayOf("https://maps.wikimedia.org/osm-intl/")
        val policy = TileSourcePolicy(
            1, TileSourcePolicy.FLAG_NO_BULK and TileSourcePolicy.FLAG_NO_PREVENTIVE and
                    TileSourcePolicy.FLAG_USER_AGENT_MEANINGFUL and TileSourcePolicy.FLAG_USER_AGENT_NORMALIZED
        )
        return XYTileSource("wikimedia", 2, 6, 256, ".png", sources, copyright, policy)
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}