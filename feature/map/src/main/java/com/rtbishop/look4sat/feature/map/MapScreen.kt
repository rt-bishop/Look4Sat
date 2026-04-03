/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2026 Arty Bishop and contributors.
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
package com.rtbishop.look4sat.feature.map

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.collection.LruCache
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.rtbishop.look4sat.core.domain.predict.GeoPos
import com.rtbishop.look4sat.core.domain.predict.OrbitalObject
import com.rtbishop.look4sat.core.domain.predict.OrbitalPos
import com.rtbishop.look4sat.core.presentation.IconCard
import com.rtbishop.look4sat.core.presentation.NextPassRow
import com.rtbishop.look4sat.core.presentation.R
import com.rtbishop.look4sat.core.presentation.Screen
import com.rtbishop.look4sat.core.presentation.TimerRow
import com.rtbishop.look4sat.core.presentation.TopBar
import com.rtbishop.look4sat.core.presentation.isVerticalLayout
import com.rtbishop.look4sat.core.presentation.layoutPadding
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

// Overlay indices
private const val OVERLAY_STATION = 0
private const val OVERLAY_TRACK = 1
private const val OVERLAY_FOOTPRINT = 2
private const val OVERLAY_POSITIONS = 3
private const val OVERLAY_COUNT = 4

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
    color = "#FFE082".toColorInt()
}
private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    textSize = 36f
    style = Paint.Style.FILL
    color = "#FFE082".toColorInt()
    setShadowLayer(3f, 3f, 3f, Color.BLACK)
}
private val iconCache = LruCache<String, Drawable>(128)

fun NavGraphBuilder.mapDestination() {
    composable(Screen.Map.route) {
        val viewModel = viewModel(MapViewModel::class.java, factory = MapViewModel.Factory)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val mapView = rememberMapViewWithLifecycle()
        MapScreen(uiState, mapView)
    }
}

@Composable
private fun MapScreen(uiState: MapState, mapView: MapView) {
    val onItemClick = { item: OrbitalObject -> uiState.sendAction(MapAction.SelectItem(item)) }
    val selectPrev = { uiState.sendAction(MapAction.SelectPrev) }
    val selectNext = { uiState.sendAction(MapAction.SelectNext) }
    val rotateMod = Modifier.rotate(180f)
    val timeString = uiState.mapData?.aosTime ?: "00:00:00"
    val isTimeAos = uiState.mapData?.isTimeAos ?: true

    LaunchedEffect(uiState.track) {
        val firstPos = uiState.track?.firstOrNull()?.firstOrNull() ?: return@LaunchedEffect
        mapView.controller.animateTo(GeoPoint(firstPos.latitude, firstPos.longitude))
    }
    Column(modifier = Modifier.layoutPadding(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val isVertical = isVerticalLayout()
        if (isVertical) {
            TopBar {
                IconCard(action = selectPrev, resId = R.drawable.ic_arrow, modifier = rotateMod)
                TimerRow(timeString = timeString, isTimeAos = isTimeAos)
                IconCard(action = selectNext, resId = R.drawable.ic_arrow)
            }
            TopBar { NextPassRow(pass = uiState.orbitalPass) }
        } else {
            TopBar {
                IconCard(action = selectPrev, resId = R.drawable.ic_arrow, modifier = rotateMod)
                TimerRow(timeString = timeString, isTimeAos = isTimeAos)
                NextPassRow(pass = uiState.orbitalPass, modifier = Modifier.weight(1f))
                IconCard(action = selectNext, resId = R.drawable.ic_arrow)
            }
        }
        ElevatedCard(modifier = Modifier.weight(1f)) {
            Box(contentAlignment = Alignment.BottomCenter) {
                AndroidView({ mapView }) { view ->
                    uiState.stationPosition?.let { setStationPosition(it, view) }
                    uiState.track?.let { setSatelliteTrack(it, view) }
                    uiState.footprint?.let { setFootprint(it, view) }
                    uiState.positions?.let { setPositions(it, view, onItemClick) }
                    view.invalidate()
                }
                uiState.mapData?.let { mapData ->
                    if (isVertical) MapDataCard(mapData) else MapDataCards(mapData)
                }
            }
        }
    }
}

// region Map data composables
@Composable
private fun MapDataCard(data: MapData) {
    val textColor = MaterialTheme.colorScheme.primary
    val cardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = stringResource(R.string.map_copyright), fontSize = 14.sp)
        Card(colors = cardColors) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                MapDataRow(
                    stringResource(R.string.map_azimuth, data.azimuth) to textColor,
                    stringResource(R.string.map_elevation, data.elevation) to textColor
                )
                MapDataRow(
                    stringResource(R.string.map_altitude, data.altitude) to null,
                    stringResource(R.string.map_distance, data.range) to null
                )
                MapDataRow(
                    stringResource(R.string.map_latitude, data.osmPos.latitude) to textColor,
                    stringResource(R.string.map_longitude, data.osmPos.longitude) to textColor
                )
                MapDataRow(
                    stringResource(R.string.map_qth, data.qthLoc) to null,
                    stringResource(R.string.map_phase, data.phase) to null
                )
            }
        }
    }
}

@Composable
private fun MapDataRow(
    left: Pair<String, androidx.compose.ui.graphics.Color?>,
    right: Pair<String, androidx.compose.ui.graphics.Color?>
) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        if (left.second != null) Text(text = left.first, color = left.second!!)
        else Text(text = left.first)
        if (right.second != null) Text(text = right.first, color = right.second!!)
        else Text(text = right.first)
    }
}

@Composable
private fun MapDataCards(data: MapData) {
    val cardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    val paddingMod = Modifier
        .padding(horizontal = 8.dp, vertical = 4.dp)
        .width(160.dp)
    val textColor = MaterialTheme.colorScheme.primary
    Box(modifier = Modifier.fillMaxSize()) {
        Card(colors = cardColors, modifier = Modifier.align(Alignment.TopStart)) {
            Column(horizontalAlignment = Alignment.Start, modifier = paddingMod) {
                Text(text = stringResource(R.string.map_azimuth, data.azimuth), color = textColor)
                Text(text = stringResource(R.string.map_elevation, data.elevation))
            }
        }
        Card(colors = cardColors, modifier = Modifier.align(Alignment.TopEnd)) {
            Column(horizontalAlignment = Alignment.End, modifier = paddingMod) {
                Text(text = stringResource(R.string.map_altitude, data.altitude), color = textColor)
                Text(text = stringResource(R.string.map_distance, data.range))
            }
        }
        Card(colors = cardColors, modifier = Modifier.align(Alignment.BottomStart)) {
            Column(horizontalAlignment = Alignment.Start, modifier = paddingMod) {
                Text(text = stringResource(R.string.map_phase, data.phase), color = textColor)
                Text(text = stringResource(R.string.map_qth, data.qthLoc))
            }
        }
        Text(
            text = stringResource(R.string.map_copyright),
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        Card(colors = cardColors, modifier = Modifier.align(Alignment.BottomEnd)) {
            Column(horizontalAlignment = Alignment.End, modifier = paddingMod) {
                Text(text = stringResource(R.string.map_latitude, data.osmPos.latitude), color = textColor)
                Text(text = stringResource(R.string.map_longitude, data.osmPos.longitude))
            }
        }
    }
}
// endregion

// region Map overlay helpers
private fun setStationPosition(stationPos: GeoPos, mapView: MapView) {
    try {
        val overlay = mapView.overlays[OVERLAY_STATION]
        if (overlay is Marker) {
            overlay.position = GeoPoint(stationPos.latitude, stationPos.longitude)
        } else {
            mapView.overlays[OVERLAY_STATION] = Marker(mapView).apply {
                setInfoWindow(null)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = ContextCompat.getDrawable(mapView.context, R.drawable.ic_position)
                position = GeoPoint(stationPos.latitude, stationPos.longitude)
            }
        }
    } catch (e: Exception) {
        println(e)
    }
}

/** Pool of reusable Marker objects keyed by satellite name, to avoid re-creation every frame */
private val markerPool = HashMap<String, Marker>()
private var lastMapView: MapView? = null

private fun setPositions(
    posMap: Map<OrbitalObject, GeoPos>,
    mapView: MapView,
    action: (OrbitalObject) -> Unit
) {
    try {
        // Clear caches when the MapView instance changes (e.g. config change)
        if (lastMapView !== mapView) {
            lastMapView = mapView
            markerPool.clear()
            iconCache.evictAll()
            footprintPolyline = null
            footprintPoints = null
        }
        // Reuse the existing FolderOverlay — creating a new one and replacing it
        // causes osmdroid to detach shared Marker objects, making them invisible.
        val folder = mapView.overlays[OVERLAY_POSITIONS] as? FolderOverlay ?: FolderOverlay().also {
            mapView.overlays[OVERLAY_POSITIONS] = it
        }
        folder.items.clear()

        val activeNames = HashSet<String>(posMap.size)
        posMap.forEach { (satellite, geoPos) ->
            val name = satellite.data.name
            activeNames.add(name)
            val marker = markerPool.getOrPut(name) {
                Marker(mapView).apply {
                    setInfoWindow(null)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = getCachedTextIcon(name, mapView)
                }
            }
            // Update position in-place — reuse existing GeoPoint if available
            val pos = marker.position
            if (pos != null) {
                pos.latitude = geoPos.latitude
                pos.longitude = geoPos.longitude
            } else {
                marker.position = GeoPoint(geoPos.latitude, geoPos.longitude)
            }
            marker.setOnMarkerClickListener { _, _ ->
                action(satellite)
                true
            }
            folder.add(marker)
        }
        // Evict markers for satellites no longer tracked
        val iter = markerPool.keys.iterator()
        while (iter.hasNext()) {
            if (iter.next() !in activeNames) iter.remove()
        }
    } catch (e: Exception) {
        println(e)
    }
}

private fun getCachedTextIcon(name: String, mapView: MapView): Drawable {
    iconCache[name]?.let { return it }
    val labelRect = Rect()
    textPaint.getTextBounds(name, 0, name.length, labelRect)
    val iconSize = 10f
    val width = labelRect.width() + iconSize * 2f
    val height = textPaint.textSize * 3f + iconSize * 2f
    val bitmap = createBitmap(width.toInt(), height.toInt())
    Canvas(bitmap).run {
        drawCircle(width / 2f, height / 2f, iconSize, textPaint)
        drawText(name, iconSize / 2f, height - iconSize, textPaint)
    }
    val drawable = bitmap.toDrawable(mapView.context.resources)
    iconCache.put(name, drawable)
    return drawable
}

private fun setSatelliteTrack(satTrack: List<List<GeoPos>>, mapView: MapView) {
    val trackOverlay = FolderOverlay()
    try {
        satTrack.forEach { track ->
            Polyline().apply {
                setPoints(track.map { GeoPoint(it.latitude, it.longitude) })
                outlinePaint.set(trackPaint)
                trackOverlay.add(this)
            }
        }
        mapView.overlays[OVERLAY_TRACK] = trackOverlay
    } catch (e: Exception) {
        println(e)
    }
}

/** Reusable footprint Polyline — created once, points updated in-place each frame */
private var footprintPolyline: Polyline? = null
private var footprintPoints: ArrayList<GeoPoint>? = null

private fun setFootprint(orbitalPos: OrbitalPos, mapView: MapView) {
    try {
        val rangeCircle = orbitalPos.getRangeCircle()
        // Lazily initialise the reusable point list and polyline
        var pts = footprintPoints
        if (pts == null || pts.size != rangeCircle.size) {
            pts = ArrayList(rangeCircle.size)
            for (gp in rangeCircle) pts.add(GeoPoint(gp.latitude, gp.longitude))
            footprintPoints = pts
        } else {
            // Update coordinates in-place — zero allocations
            for (i in rangeCircle.indices) {
                pts[i].latitude = rangeCircle[i].latitude
                pts[i].longitude = rangeCircle[i].longitude
            }
        }
        val polyline = footprintPolyline ?: Polyline().apply {
            outlinePaint.set(footprintPaint)
            footprintPolyline = this
        }
        polyline.setPoints(pts)
        mapView.overlays[OVERLAY_FOOTPRINT] = polyline
    } catch (e: Exception) {
        println(e)
    }
}
// endregion

// region MapView lifecycle
@Composable
private fun rememberMapViewWithLifecycle(): MapView {
    val tileSource = XYTileSource("tiles", 0, 6, 256, ".webp", emptyArray<String>())
    val context = LocalContext.current
    val isVertical = isVerticalLayout()
    val mapView = remember {
        MapView(context).apply {
            setMultiTouchControls(true)
            setUseDataConnection(false)
            setTileSource(tileSource)
            minZoomLevel = getMinZoom(resources.displayMetrics.heightPixels, isVertical)
            maxZoomLevel = 7.0
            controller.setCenter(GeoPoint(48.8575, 6.3514))
            controller.setZoom(minZoomLevel + 2)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            overlayManager.tilesOverlay.loadingBackgroundColor = Color.TRANSPARENT
            overlayManager.tilesOverlay.loadingLineColor = Color.TRANSPARENT
            overlayManager.tilesOverlay.setColorFilter(createColorFilter())
            setScrollableAreaLimitLatitude(maxLat, minLat, 0)
            overlays.addAll(Array(OVERLAY_COUNT) { FolderOverlay() })
        }
    }
    val lifecycleObserver = rememberMapViewLifecycleObserver(mapView)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        lifecycle.addObserver(lifecycleObserver)
        onDispose { lifecycle.removeObserver(lifecycleObserver) }
    }
    return mapView
}

@Composable
private fun rememberMapViewLifecycleObserver(mapView: MapView) = remember(mapView) {
    LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> mapView.onResume()
            Lifecycle.Event.ON_PAUSE -> mapView.onPause()
            else -> {}
        }
    }
}

private fun createColorFilter(): ColorMatrixColorFilter {
    val grayScale = ColorMatrix().apply { setSaturation(0f) }
    val negative = ColorMatrix(
        floatArrayOf(-1f, 0f, 0f, 0f, 260f, 0f, -1f, 0f, 0f, 260f, 0f, 0f, -1f, 0f, 260f, 0f, 0f, 0f, 1f, 0f)
    )
    negative.preConcat(grayScale)
    return ColorMatrixColorFilter(negative)
}

private fun getMinZoom(screenHeight: Int, isVertical: Boolean): Double {
    if (!isVertical) return 3.5
    return MapView.getTileSystem().getLatitudeZoom(maxLat, minLat, screenHeight)
}
// endregion
