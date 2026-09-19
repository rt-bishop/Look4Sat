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
import com.rtbishop.look4sat.core.domain.predict.GeoPos
import com.rtbishop.look4sat.core.domain.predict.OrbitalObject
import com.rtbishop.look4sat.core.domain.repository.IContainerProvider
import com.rtbishop.look4sat.core.presentation.IconCard
import com.rtbishop.look4sat.core.presentation.NextPassRow
import com.rtbishop.look4sat.core.presentation.R
import com.rtbishop.look4sat.core.presentation.TimerRow
import com.rtbishop.look4sat.core.presentation.TopBar
import com.rtbishop.look4sat.core.presentation.isVerticalLayout
import com.rtbishop.look4sat.core.presentation.layoutPadding
import org.osmdroid.events.DelayedMapListener
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
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
private const val OVERLAY_TERMINATOR = 4
private const val OVERLAY_SUN = 5
private const val OVERLAY_MOON = 6
private const val OVERLAY_COUNT = 7

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
private val sunIconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    colorFilter =
        android.graphics.PorterDuffColorFilter("#FFE082".toColorInt(), android.graphics.PorterDuff.Mode.SRC_IN)
}
private val moonIconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    colorFilter =
        android.graphics.PorterDuffColorFilter("#E0E0E0".toColorInt(), android.graphics.PorterDuff.Mode.SRC_IN)
}

/** Accent used for the station marker; recolored per theme so it stays visible on a light map. */
private var mapAccentColor = "#FFE082".toColorInt()

/** The dark-map filter (grayscale + invert) is only applied when NOT in light theme; cached to avoid rebuilds. */
private val darkTileFilter by lazy { createColorFilter() }

/**
 * Recolor the shared overlay paints for the current theme. The light OSM tiles are white, so the
 * dark-theme amber/grey overlays would vanish; light theme swaps them for dark, high-contrast colors.
 * Must run before the set*() overlay builders on each frame, as several icons cache the paint color.
 */
private fun applyMapColors(isLightUi: Boolean) {
    if (isLightUi) {
        mapAccentColor = "#715C0C".toColorInt() // dark amber (matches app lightScheme primary)
        footprintPaint.color = mapAccentColor
        textPaint.color = "#1E1B13".toColorInt()
        textPaint.setShadowLayer(3f, 3f, 3f, Color.WHITE)
        sunIconPaint.colorFilter =
            android.graphics.PorterDuffColorFilter(mapAccentColor, android.graphics.PorterDuff.Mode.SRC_IN)
        moonIconPaint.colorFilter =
            android.graphics.PorterDuffColorFilter("#4C4639".toColorInt(), android.graphics.PorterDuff.Mode.SRC_IN)
    } else {
        mapAccentColor = "#FFE082".toColorInt()
        footprintPaint.color = mapAccentColor
        textPaint.color = mapAccentColor
        textPaint.setShadowLayer(3f, 3f, 3f, Color.BLACK)
        sunIconPaint.colorFilter =
            android.graphics.PorterDuffColorFilter(mapAccentColor, android.graphics.PorterDuff.Mode.SRC_IN)
        moonIconPaint.colorFilter =
            android.graphics.PorterDuffColorFilter("#E0E0E0".toColorInt(), android.graphics.PorterDuff.Mode.SRC_IN)
    }
}

@Composable
fun MapDestination() {
    val context = LocalContext.current
    val container = (context.applicationContext as IContainerProvider).getMainContainer()
    val viewModel: MapViewModel = viewModel(factory = MapViewModel.factory(container))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mapView = rememberMapViewWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.onAction(MapAction.SetVisible(true))
                Lifecycle.Event.ON_STOP -> viewModel.onAction(MapAction.SetVisible(false))
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            viewModel.onAction(MapAction.SetVisible(false))
        }
    }
    MapScreen(uiState, viewModel::onAction, mapView)
}

@Composable
private fun MapScreen(uiState: MapState, onAction: (MapAction) -> Unit, mapView: MapView) {
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
                IconCard(action = { onAction(MapAction.SelectPrev) }, resId = R.drawable.ic_arrow, modifier = rotateMod)
                TimerRow(timeString = timeString, isTimeAos = isTimeAos)
                IconCard(action = { onAction(MapAction.SelectNext) }, resId = R.drawable.ic_arrow)
            }
            TopBar { NextPassRow(pass = uiState.orbitalPass, isUtc = uiState.isUtc) }
        } else {
            TopBar {
                IconCard(action = { onAction(MapAction.SelectPrev) }, resId = R.drawable.ic_arrow, modifier = rotateMod)
                TimerRow(timeString = timeString, isTimeAos = isTimeAos)
                NextPassRow(pass = uiState.orbitalPass, modifier = Modifier.weight(1f), isUtc = uiState.isUtc)
                IconCard(action = { onAction(MapAction.SelectNext) }, resId = R.drawable.ic_arrow)
            }
        }
        ElevatedCard(modifier = Modifier.weight(1f)) {
            Box(contentAlignment = Alignment.BottomCenter) {
                AndroidView({ mapView }) { view ->
                    applyMapColors(uiState.isLightUi)
                    view.overlayManager.tilesOverlay.setColorFilter(if (uiState.isLightUi) null else darkTileFilter)
                    uiState.stationPosition?.let { setStationPosition(it, view) }
                    uiState.track?.let { setSatelliteTrack(it, view) }
                    uiState.footprint?.let { setFootprint(it, view) }
                    uiState.positions?.let { setPositions(it, view) { item -> onAction(MapAction.SelectItem(item)) } }
                    setTerminator(uiState.sunLatDeg, uiState.sunLonDeg, view)
                    setSubSolarPoint(uiState.sunLatDeg, uiState.sunLonDeg, view)
                    setMoonPosition(uiState.moonLatDeg, uiState.moonLonDeg, view)
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
                icon = ContextCompat.getDrawable(mapView.context, R.drawable.ic_position)?.apply {
                    setTint(mapAccentColor)
                }
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

/**
 * Above this many satellites inside the viewport labels are dropped in favor of a single
 * shared dot icon. Per-satellite label bitmaps cost ~90KB each, so drawing thousands of them
 * exhausts memory and stalls the UI thread — and overlapping labels are unreadable anyway.
 */
private const val LABEL_LIMIT = 128

/** Degrees of space around the viewport so markers don't pop in at the edges */
private const val VIEWPORT_MARGIN = 8.0

/** Debounce for viewport-driven marker refreshes, in milliseconds */
private const val MAP_LISTENER_DELAY = 128L

/** Shared dot icon used when too many satellites are visible to label them */
private var dotIcon: Drawable? = null

/** Scratch list reused every frame to avoid per-tick allocation */
private val visibleSats = ArrayList<Pair<OrbitalObject, GeoPos>>()

/** Last emitted positions, replayed on scroll/zoom so culled markers appear without waiting for a tick */
private var lastPositions: Map<OrbitalObject, GeoPos>? = null
private var lastAction: ((OrbitalObject) -> Unit)? = null

private fun setPositions(
    posMap: Map<OrbitalObject, GeoPos>,
    mapView: MapView,
    action: (OrbitalObject) -> Unit
) {
    try {
        lastPositions = posMap
        lastAction = action
        // Clear caches when the MapView instance changes (e.g. config change)
        if (lastMapView !== mapView) {
            lastMapView = mapView
            markerPool.clear()
            iconCache.evictAll()
            dotIcon = null
            footprintPolyline = null
            footprintPoints = null
            mapView.addMapListener(DelayedMapListener(object : MapListener {
                override fun onScroll(event: ScrollEvent?) = refreshPositions(mapView)
                override fun onZoom(event: ZoomEvent?) = refreshPositions(mapView)
            }, MAP_LISTENER_DELAY))
        }
        // Reuse the existing FolderOverlay — creating a new one and replacing it
        // causes osmdroid to detach shared Marker objects, making them invisible.
        val folder = mapView.overlays[OVERLAY_POSITIONS] as? FolderOverlay ?: FolderOverlay().also {
            mapView.overlays[OVERLAY_POSITIONS] = it
        }
        folder.items.clear()

        // Cull satellites outside the viewport: only meaningful once zoomed in, but that is
        // exactly when marker labels are shown and drawing is most expensive.
        visibleSats.clear()
        if (mapView.width > 0 && mapView.height > 0) {
            val box = mapView.boundingBox
            val latNorth = box.latNorth + VIEWPORT_MARGIN
            val latSouth = box.latSouth - VIEWPORT_MARGIN
            val lonWest = box.lonWest - VIEWPORT_MARGIN
            val lonEast = box.lonEast + VIEWPORT_MARGIN
            val wrapsDateLine = box.lonWest > box.lonEast
            for ((satellite, geoPos) in posMap) {
                val lat = geoPos.latitude
                if (lat !in latSouth..latNorth) continue
                val lon = geoPos.longitude
                val isLonVisible = if (wrapsDateLine) lon >= lonWest || lon <= lonEast
                else lon in lonWest..lonEast
                if (isLonVisible) visibleSats.add(satellite to geoPos)
            }
        } else {
            for (entry in posMap) visibleSats.add(entry.key to entry.value)
        }

        val showLabels = visibleSats.size <= LABEL_LIMIT
        val activeNames = HashSet<String>(visibleSats.size)
        for ((satellite, geoPos) in visibleSats) {
            val name = satellite.data.name
            activeNames.add(name)
            val marker = markerPool.getOrPut(name) {
                Marker(mapView).apply {
                    setInfoWindow(null)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    // Resolve the satellite via relatedObject so the listener is allocated
                    // once per marker instead of once per satellite per tick
                    setOnMarkerClickListener { clicked, _ ->
                        (clicked.relatedObject as? OrbitalObject)?.let(action)
                        true
                    }
                }
            }
            marker.relatedObject = satellite
            val icon = if (showLabels) getCachedTextIcon(name, mapView) else getDotIcon(mapView)
            if (marker.icon !== icon) marker.icon = icon
            // Update position in-place — reuse existing GeoPoint if available
            val pos = marker.position
            if (pos != null) {
                pos.latitude = geoPos.latitude
                pos.longitude = geoPos.longitude
            } else {
                marker.position = GeoPoint(geoPos.latitude, geoPos.longitude)
            }
            folder.add(marker)
        }
        // Evict markers that are no longer tracked or no longer visible
        markerPool.keys.retainAll(activeNames)
        visibleSats.clear()
    } catch (e: Exception) {
        println(e)
    }
}

/** Re-applies the last known positions against the new viewport after a pan or zoom */
private fun refreshPositions(mapView: MapView): Boolean {
    val posMap = lastPositions ?: return false
    val action = lastAction ?: return false
    setPositions(posMap, mapView, action)
    mapView.invalidate()
    return true
}

private fun getDotIcon(mapView: MapView): Drawable = dotIcon ?: run {
    val size = 20
    val bitmap = createBitmap(size, size)
    Canvas(bitmap).drawCircle(size / 2f, size / 2f, size / 2f - 2f, textPaint)
    bitmap.toDrawable(mapView.context.resources).also { dotIcon = it }
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

private fun setFootprint(rangeCircle: List<GeoPos>, mapView: MapView) {
    try {
        var pts = footprintPoints
        if (pts == null || pts.size != rangeCircle.size) {
            pts = ArrayList(rangeCircle.size)
            for (gp in rangeCircle) pts.add(GeoPoint(gp.latitude, gp.longitude))
            footprintPoints = pts
        } else {
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

/**
 * Update the NightOverlay with the current sub-solar position.
 * The overlay is created once and kept in OVERLAY_TERMINATOR; only its
 * sunLatDeg/sunLonDeg fields are updated each tick so osmdroid redraws it.
 */
private fun setTerminator(sunLatDeg: Double, sunLonDeg: Double, mapView: MapView) {
    try {
        val overlay = mapView.overlays[OVERLAY_TERMINATOR]
        if (overlay is MapNightOverlay) {
            overlay.sunLatDeg = sunLatDeg
            overlay.sunLonDeg = sunLonDeg
        } else {
            mapView.overlays[OVERLAY_TERMINATOR] = MapNightOverlay().apply {
                this.sunLatDeg = sunLatDeg
                this.sunLonDeg = sunLonDeg
            }
        }
    } catch (e: Exception) {
        println(e)
    }
}

/** Place an ic_sun icon marker at the sub-solar point. */
private fun setSubSolarPoint(sunLatDeg: Double, sunLonDeg: Double, mapView: MapView) {
    try {
        val overlay = mapView.overlays[OVERLAY_SUN]
        val sunPos = GeoPoint(sunLatDeg, sunLonDeg)
        if (overlay is Marker) {
            overlay.position = sunPos
        } else {
            val iconSize = 48
            val bmp = createBitmap(iconSize, iconSize)
            ContextCompat.getDrawable(mapView.context, R.drawable.ic_sun)?.apply {
                setBounds(0, 0, iconSize, iconSize)
                colorFilter = sunIconPaint.colorFilter
                draw(Canvas(bmp))
            }
            mapView.overlays[OVERLAY_SUN] = Marker(mapView).apply {
                setInfoWindow(null)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = bmp.toDrawable(mapView.context.resources)
                position = sunPos
            }
        }
    } catch (e: Exception) {
        println(e)
    }
}

/** Place an ic_moon icon marker at the sub-lunar point. */
private fun setMoonPosition(moonLatDeg: Double, moonLonDeg: Double, mapView: MapView) {
    try {
        val overlay = mapView.overlays[OVERLAY_MOON]
        val moonPos = GeoPoint(moonLatDeg, moonLonDeg)
        if (overlay is Marker) {
            overlay.position = moonPos
        } else {
            val iconSize = 48
            val bmp = createBitmap(iconSize, iconSize)
            val c = Canvas(bmp)
            ContextCompat.getDrawable(mapView.context, R.drawable.ic_moon)?.apply {
                setBounds(0, 0, iconSize, iconSize)
                colorFilter = moonIconPaint.colorFilter
                draw(c)
            }
            mapView.overlays[OVERLAY_MOON] = Marker(mapView).apply {
                setInfoWindow(null)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = bmp.toDrawable(mapView.context.resources)
                position = moonPos
            }
        }
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
            // Tile color filter (dark inversion vs light) is applied per-theme in the AndroidView update block.
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
    // The overlay caches below are file-level (shared across MapView instances), so they must be
    // released with the MapView or they keep the Activity and its bitmaps alive after disposal.
    DisposableEffect(mapView) {
        onDispose {
            clearMapCaches()
            mapView.onDetach()
        }
    }
    return mapView
}

private fun clearMapCaches() {
    markerPool.clear()
    iconCache.evictAll()
    dotIcon = null
    footprintPolyline = null
    footprintPoints = null
    lastPositions = null
    lastAction = null
    lastMapView = null
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
