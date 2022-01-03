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

import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.databinding.FragmentMapBinding
import com.rtbishop.look4sat.domain.predict.GeoPos
import com.rtbishop.look4sat.domain.predict.Satellite
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.*
import timber.log.Timber
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class MapFragment : Fragment(R.layout.fragment_map) {

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    private val viewModel: MapViewModel by viewModels()
    private val minLat = MapView.getTileSystem().minLatitude
    private val maxLat = MapView.getTileSystem().maxLatitude
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 2f
        style = Paint.Style.STROKE
        color = Color.RED
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val footprintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL_AND_STROKE
        color = Color.parseColor("#26FFE082")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Configuration.getInstance().load(requireContext(), sharedPreferences)
        val binding = FragmentMapBinding.bind(view).apply {
            mapView.apply {
                setMultiTouchControls(true)
                setTileSource(TileSourceFactory.WIKIMEDIA)
                minZoomLevel = getMinZoom(resources.displayMetrics.heightPixels) + 0.25
                maxZoomLevel = 5.75
                controller.setZoom(minZoomLevel + 0.25)
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                overlayManager.tilesOverlay.loadingBackgroundColor = Color.TRANSPARENT
                overlayManager.tilesOverlay.loadingLineColor = Color.TRANSPARENT
                overlayManager.tilesOverlay.setColorFilter(getColorFilter())
                setScrollableAreaLimitLatitude(maxLat, minLat, 0)
                // add overlays: 0 - GSP, 1 - SatTrack, 2 - SatFootprint, 3 - SatIcons
                overlays.addAll(Array(4) { FolderOverlay() })
            }
        }
        binding.mapBtnPrev.setOnClickListener { viewModel.scrollSelection(true) }
        binding.mapBtnNext.setOnClickListener { viewModel.scrollSelection(false) }
        setupObservers(binding)
    }

    private fun setupObservers(binding: FragmentMapBinding) {
        viewModel.selectDefaultSatellite(arguments?.getInt("catNum"))
        viewModel.stationPosLiveData.observe(viewLifecycleOwner, { renderStationPos(it, binding) })
        viewModel.satPositions.observe(viewLifecycleOwner, { renderSatPositions(it, binding) })
        viewModel.satTrack.observe(viewLifecycleOwner, { renderSatTrack(it, binding) })
        viewModel.satFootprint.observe(viewLifecycleOwner, { renderSatFootprint(it, binding) })
        viewModel.mapData.observe(viewLifecycleOwner, { renderSatData(it, binding) })
    }

    private fun renderStationPos(stationPos: GeoPos, binding: FragmentMapBinding) {
        binding.apply {
            Marker(mapView).apply {
                setInfoWindow(null)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_pos)
                position = GeoPoint(stationPos.latitude, stationPos.longitude)
                mapView.overlays[0] = this
                mapView.invalidate()
            }
        }
    }

    private fun renderSatPositions(posMap: Map<Satellite, GeoPos>, binding: FragmentMapBinding) {
        binding.apply {
            val markers = FolderOverlay()
            posMap.entries.forEach {
                Marker(mapView).apply {
                    setInfoWindow(null)
                    textLabelFontSize = 24
                    textLabelBackgroundColor = Color.TRANSPARENT
                    textLabelForegroundColor =
                        ContextCompat.getColor(requireContext(), R.color.themeAccent)
                    setTextIcon(it.key.params.name)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    try {
                        position = GeoPoint(it.value.latitude, it.value.longitude)
                    } catch (exception: IllegalArgumentException) {
                        Timber.d(exception)
                    }
                    setOnMarkerClickListener { _, _ ->
                        viewModel.selectSatellite(it.key)
                        return@setOnMarkerClickListener true
                    }
                    markers.add(this)
                }
            }
            mapView.overlays[3] = markers
            mapView.invalidate()
        }
    }

    private fun renderSatTrack(satTrack: List<List<GeoPos>>, binding: FragmentMapBinding) {
        val trackOverlay = FolderOverlay()
        satTrack.forEach { track ->
            val trackPoints = track.map { GeoPoint(it.latitude, it.longitude) }
            Polyline().apply {
                try {
                    setPoints(trackPoints)
                    outlinePaint.set(trackPaint)
                    trackOverlay.add(this)
                } catch (exception: IllegalArgumentException) {
                    Timber.d(exception)
                }
            }
        }
        binding.mapView.overlays[1] = trackOverlay
    }

    private fun renderSatFootprint(satFootprint: List<GeoPos>, binding: FragmentMapBinding) {
        val footprintPoints = satFootprint.map { GeoPoint(it.latitude, it.longitude) }
        val footprintOverlay = Polygon().apply {
            fillPaint.set(footprintPaint)
            outlinePaint.set(footprintPaint)
            try {
                this.points = footprintPoints
            } catch (exception: IllegalArgumentException) {
                Timber.d(exception)
            }
        }
        binding.mapView.overlays[2] = footprintOverlay
    }

    private fun renderSatData(mapData: MapData, binding: FragmentMapBinding) {
        binding.apply {
            mapDataName.text =
                String.format(getString(R.string.pat_osm_idName), mapData.catNum, mapData.name)
            mapDataQth.text = String.format(getString(R.string.map_qth), mapData.qthLoc)
            mapDataAlt.text = String.format(getString(R.string.pat_altitude), mapData.altitude)
            mapDataDst.text = String.format(getString(R.string.pat_distance), mapData.range)
            mapDataVel.text = String.format(getString(R.string.pat_osm_vel), mapData.velocity)
            mapDataLat.text =
                String.format(getString(R.string.pat_osm_lat), mapData.osmPos.latitude)
            mapDataLon.text =
                String.format(getString(R.string.pat_osm_lon), mapData.osmPos.longitude)
        }
        binding.mapView.invalidate()
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
