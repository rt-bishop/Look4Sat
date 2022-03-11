/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2022 Arty Bishop (bishop.arty@gmail.com)
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

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.databinding.FragmentMapBinding
import com.rtbishop.look4sat.domain.predict.GeoPos
import com.rtbishop.look4sat.domain.predict.SatPos
import com.rtbishop.look4sat.domain.predict.Satellite
import com.rtbishop.look4sat.presentation.clickWithDebounce
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@AndroidEntryPoint
class MapFragment : Fragment(R.layout.fragment_map) {

    private val viewModel: MapViewModel by viewModels()
    private val navArgs: MapFragmentArgs by navArgs()
    private val minLat = MapView.getTileSystem().minLatitude
    private val maxLat = MapView.getTileSystem().maxLatitude
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 3f
        style = Paint.Style.STROKE
        color = Color.RED
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val footprintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 3f
        style = Paint.Style.FILL_AND_STROKE
        color = Color.parseColor("#FFE082")
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 36f
        style = Paint.Style.FILL
        color = Color.parseColor("#CCFFFFFF")
        setShadowLayer(3f, 3f, 3f, Color.BLACK)
    }
    private val labelRect = Rect()
    private lateinit var binding: FragmentMapBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMapBinding.bind(view)
        setupComponents()
        setupObservers()
    }

    private fun setupComponents() {
        val context = requireContext()
        val preferences = context.getSharedPreferences("default", Context.MODE_PRIVATE)
        Configuration.getInstance().load(context, preferences)
        binding.run {
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
            mapBtnBack.clickWithDebounce { findNavController().navigateUp() }
            mapBtnPrev.setOnClickListener { viewModel.scrollSelection(true) }
            mapBtnNext.setOnClickListener { viewModel.scrollSelection(false) }
        }
    }

    private fun setupObservers() {
        viewModel.selectDefaultSatellite(navArgs.catNum)
        viewModel.stationPos.observe(viewLifecycleOwner) { handleStationPos(it) }
        viewModel.positions.observe(viewLifecycleOwner) { handlePositions(it) }
        viewModel.track.observe(viewLifecycleOwner) { handleTrack(it) }
        viewModel.footprint.observe(viewLifecycleOwner) { handleFootprint(it) }
        viewModel.mapData.observe(viewLifecycleOwner) { handleMapData(it) }
    }

    private fun handleStationPos(stationPos: GeoPos) {
        binding.apply {
            Marker(mapView).apply {
                setInfoWindow(null)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_position)
                position = GeoPoint(stationPos.lat, stationPos.lon)
                mapView.overlays[0] = this
                mapView.invalidate()
            }
        }
    }

    private fun handlePositions(posMap: Map<Satellite, GeoPos>) {
        binding.apply {
            val markers = FolderOverlay()
            posMap.entries.forEach {
                Marker(mapView).apply {
                    setInfoWindow(null)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = getCustomTextIcon(it.key.data.name)
                    try {
                        position = GeoPoint(it.value.lat, it.value.lon)
                    } catch (exception: IllegalArgumentException) {
                        println(exception.stackTraceToString())
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

    private fun getCustomTextIcon(textLabel: String): Drawable {
        textPaint.getTextBounds(textLabel, 0, textLabel.length, labelRect)
        val iconSize = 8f
        val width = labelRect.width() + iconSize * 2f
        val height = textPaint.textSize * 3f + iconSize * 2f
        val bitmap = Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888)
        Canvas(bitmap).run {
            drawCircle(width / 2f, height / 2f, iconSize, textPaint)
            drawText(textLabel, iconSize / 2f, height - iconSize, textPaint)
        }
        return BitmapDrawable(binding.mapView.context.resources, bitmap)
    }

    private fun handleTrack(satTrack: List<List<GeoPos>>) {
        val center = GeoPoint(satTrack[0][0].lat, satTrack[0][0].lon)
        val trackOverlay = FolderOverlay()
        satTrack.forEach { track ->
            val trackPoints = track.map { GeoPoint(it.lat, it.lon) }
            Polyline().apply {
                try {
                    setPoints(trackPoints)
                    outlinePaint.set(trackPaint)
                    trackOverlay.add(this)
                } catch (exception: IllegalArgumentException) {
                    println(exception.stackTraceToString())
                }
            }
        }
        binding.mapView.overlays[1] = trackOverlay
        binding.mapView.controller.animateTo(center)
    }

    private fun handleFootprint(satPos: SatPos) {
        val footprintPoints = satPos.getRangeCircle().map { GeoPoint(it.lat, it.lon) }
        val footprintOverlay = Polyline().apply {
            outlinePaint.set(footprintPaint)
            try {
                setPoints(footprintPoints)
            } catch (exception: IllegalArgumentException) {
                println(exception.stackTraceToString())
            }
        }
        binding.mapView.overlays[2] = footprintOverlay
    }

    private fun handleMapData(mapData: MapData) {
        binding.apply {
            mapTimer.text = mapData.aosTime
            mapDataPeriod.text = String.format(getString(R.string.map_period, mapData.period))
            mapDataPhase.text = String.format(getString(R.string.map_phase), mapData.phase)
            mapAzimuth.text = String.format(getString(R.string.map_azimuth), mapData.azimuth)
            mapElevation.text = String.format(getString(R.string.map_elevation), mapData.elevation)
            mapDataAlt.text = String.format(getString(R.string.map_altitude), mapData.altitude)
            mapDataDst.text = String.format(getString(R.string.map_distance), mapData.range)
            mapDataLat.text =
                String.format(getString(R.string.map_latitude), mapData.osmPos.lat)
            mapDataLon.text =
                String.format(getString(R.string.map_longitude), mapData.osmPos.lon)
            mapDataQth.text = String.format(getString(R.string.map_qth), mapData.qthLoc)
            mapDataVel.text = String.format(getString(R.string.map_velocity), mapData.velocity)
            if (mapData.eclipsed) {
                mapDataVisibility.text = getString(R.string.map_eclipsed)
            } else {
                mapDataVisibility.text = getString(R.string.map_visible)
            }
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
