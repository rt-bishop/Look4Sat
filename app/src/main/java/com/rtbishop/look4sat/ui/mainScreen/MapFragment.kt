package com.rtbishop.look4sat.ui.mainScreen

import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.github.amsacode.predict4java.Position
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.SharedViewModel
import com.rtbishop.look4sat.data.Result
import com.rtbishop.look4sat.data.SatPass
import com.rtbishop.look4sat.data.SelectedSat
import com.rtbishop.look4sat.databinding.FragmentMapBinding
import com.rtbishop.look4sat.utility.PrefsManager
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
    lateinit var prefsManager: PrefsManager
    private lateinit var binding: FragmentMapBinding
    private val mapViewModel: MapViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Configuration.getInstance().load(requireContext(), prefsManager.preferences)
        binding = FragmentMapBinding.bind(view).apply {
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
        setupObservers()
    }

    private fun setupObservers() {
        mapViewModel.getGSP().observe(viewLifecycleOwner, { setupPosOverlay(it) })
        sharedViewModel.getPasses().observe(viewLifecycleOwner, {
            if (it is Result.Success && it.data.isNotEmpty()) {
                mapViewModel.setPasses(it.data)
                binding.fabPrev.setOnClickListener { mapViewModel.scrollSelection(true) }
                binding.fabNext.setOnClickListener { mapViewModel.scrollSelection(false) }
            }
        })
        mapViewModel.getSelectedSat().observe(viewLifecycleOwner, { setSelectedSatDetails(it) })
        mapViewModel.getSatMarkers().observe(viewLifecycleOwner, { setMarkers(it) })
    }

    private fun setupPosOverlay(osmPos: Position) {
        binding.apply {
            Marker(mapView).apply {
                setInfoWindow(null)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_map_pos)
                position = GeoPoint(osmPos.lat, osmPos.lon)
                mapView.overlays[0] = this
                mapView.invalidate()
            }
        }
    }

    private fun setSelectedSatDetails(sat: SelectedSat) {
        binding.apply {
            idName.text = String.format(getString(R.string.pat_osm_idName), sat.catNum, sat.name)
            qthLocator.text = String.format(getString(R.string.map_qth), sat.qthLoc)
            altitude.text = String.format(getString(R.string.pat_altitude), sat.altitude)
            distance.text = String.format(getString(R.string.pat_distance), sat.range)
            velocity.text = String.format(getString(R.string.pat_osm_vel), sat.velocity)
            latLon.text =
                String.format(getString(R.string.pat_osm_latLon), sat.osmPos.lat, sat.osmPos.lon)
            mapCoverage.text = String.format(getString(R.string.map_coverage), sat.coverage)
            mapView.overlays[1] = sat.groundTrack
            mapView.overlays[2] = sat.footprint
            mapView.invalidate()
        }
    }

    private fun setMarkers(map: Map<SatPass, Position>) {
        binding.apply {
            val markers = FolderOverlay()
            map.entries.forEach {
                if (prefsManager.shouldUseTextLabels()) {
                    Marker(mapView).apply {
                        setInfoWindow(null)
                        textLabelFontSize = 24
                        textLabelBackgroundColor = Color.TRANSPARENT
                        textLabelForegroundColor =
                            ContextCompat.getColor(requireContext(), R.color.themeLight)
                        setTextIcon(it.key.tle.name)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        position = GeoPoint(it.value.lat, it.value.lon)
                        setOnMarkerClickListener { _, _ ->
                            mapViewModel.selectSatellite(it.key)
                            return@setOnMarkerClickListener true
                        }
                        markers.add(this)
                    }
                } else {
                    Marker(mapView).apply {
                        setInfoWindow(null)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_map_sat)
                        position = GeoPoint(it.value.lat, it.value.lon)
                        setOnMarkerClickListener { _, _ ->
                            mapViewModel.selectSatellite(it.key)
                            return@setOnMarkerClickListener true
                        }
                        markers.add(this)
                    }
                }
            }
            mapView.overlays[3] = markers
            mapView.invalidate()
        }
    }

    private fun getColorFilter(): ColorMatrixColorFilter {
        val targetColor = Color.RED
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
}