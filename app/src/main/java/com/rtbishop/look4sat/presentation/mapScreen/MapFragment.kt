/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2021 Arty Bishop (bishop.arty@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.rtbishop.look4sat.presentation.mapScreen

import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.databinding.FragmentMapBinding
import com.rtbishop.look4sat.domain.predict4kotlin.Position
import com.rtbishop.look4sat.domain.predict4kotlin.Satellite
import com.rtbishop.look4sat.framework.model.SelectedSat
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.*
import timber.log.Timber
import java.util.*

@AndroidEntryPoint
class MapFragment : Fragment(R.layout.fragment_map) {

    private val mapViewModel: MapViewModel by viewModels()
    private val minLat = MapView.getTileSystem().minLatitude
    private val maxLat = MapView.getTileSystem().maxLatitude

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Configuration.getInstance().load(requireContext(), mapViewModel.getPreferences())
        val binding = FragmentMapBinding.bind(view).apply {
            mapView.apply {
                setMultiTouchControls(true)
                setTileSource(TileSourceFactory.WIKIMEDIA)
                val minZoom = getMinZoom(resources.displayMetrics.heightPixels)
                minZoomLevel = minZoom
                maxZoomLevel = 6.0
                controller.setZoom(minZoom + 0.5)
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                overlayManager.tilesOverlay.loadingBackgroundColor = Color.TRANSPARENT
                overlayManager.tilesOverlay.loadingLineColor = Color.TRANSPARENT
                overlayManager.tilesOverlay.setColorFilter(getColorFilter())
                setScrollableAreaLimitLatitude(maxLat, minLat, 0)
                // add overlays: 0 - GSP, 1 - SatTrack, 2 - SatFootprint, 3 - SatIcons
                overlays.addAll(Array(4) { FolderOverlay() })
            }
        }
        binding.fabPrev.setOnClickListener { mapViewModel.scrollSelection(true) }
        binding.fabNext.setOnClickListener { mapViewModel.scrollSelection(false) }
        setupObservers(binding)
    }

    private fun setupObservers(binding: FragmentMapBinding) {
        mapViewModel.stationPosition.observe(viewLifecycleOwner, { setupPosOverlay(it, binding) })
        mapViewModel.getSelectedSat().observe(viewLifecycleOwner, { setSatDetails(it, binding) })
        mapViewModel.getSatMarkers().observe(viewLifecycleOwner, { setMarkers(it, binding) })
    }

    private fun setupPosOverlay(osmPos: Position, binding: FragmentMapBinding) {
        binding.apply {
            Marker(mapView).apply {
                setInfoWindow(null)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_map_pos)
                position = GeoPoint(osmPos.latitude, osmPos.longitude)
                mapView.overlays[0] = this
                mapView.invalidate()
            }
        }
    }

    private fun setSatDetails(sat: SelectedSat, binding: FragmentMapBinding) {
        binding.apply {
            idName.text = String.format(getString(R.string.pat_osm_idName), sat.catNum, sat.name)
            qthLocator.text = String.format(getString(R.string.map_qth), sat.qthLoc)
            altitude.text = String.format(getString(R.string.pat_altitude), sat.altitude)
            distance.text = String.format(getString(R.string.pat_distance), sat.range)
            velocity.text = String.format(getString(R.string.pat_osm_vel), sat.velocity)
            mapLat.text = String.format(getString(R.string.pat_osm_lat), sat.osmPos.latitude)
            mapLon.text = String.format(getString(R.string.pat_osm_lon), sat.osmPos.longitude)
            mapView.overlays[1] = sat.groundTrack
            mapView.overlays[2] = sat.footprint
            mapView.invalidate()
        }
    }

    private fun setMarkers(map: Map<Satellite, Position>, binding: FragmentMapBinding) {
        binding.apply {
            val markers = FolderOverlay()
            map.entries.forEach {
                if (mapViewModel.shouldUseTextLabels()) {
                    Marker(mapView).apply {
                        setInfoWindow(null)
                        textLabelFontSize = 24
                        textLabelBackgroundColor = Color.TRANSPARENT
                        textLabelForegroundColor =
                            ContextCompat.getColor(requireContext(), R.color.themeLight)
                        setTextIcon(it.key.tle.name)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        try {
                            position = GeoPoint(it.value.latitude, it.value.longitude)
                        } catch (e: IllegalArgumentException) {
                            Timber.d("Position: $position")
                        }
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
                        try {
                            position = GeoPoint(it.value.latitude, it.value.longitude)
                        } catch (e: IllegalArgumentException) {
                            Timber.d("Position: $position")
                        }
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
        val grayScaleMatrix = ColorMatrix().apply { setSaturation(0f) }
        val negativeMatrix = ColorMatrix(
            floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        negativeMatrix.preConcat(grayScaleMatrix)
        return ColorMatrixColorFilter(negativeMatrix)
    }

    private fun getMinZoom(screenHeight: Int): Double {
        return MapView.getTileSystem().getLatitudeZoom(maxLat, minLat, screenHeight)
    }
}
