package com.rtbishop.look4sat.presentation.map

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
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
import androidx.compose.runtime.State
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.domain.predict.GeoPos
import com.rtbishop.look4sat.domain.predict.OrbitalObject
import com.rtbishop.look4sat.domain.predict.OrbitalPos
import com.rtbishop.look4sat.presentation.Screen
import com.rtbishop.look4sat.presentation.common.IconCard
import com.rtbishop.look4sat.presentation.common.NextPassRow
import com.rtbishop.look4sat.presentation.common.TimerRow
import com.rtbishop.look4sat.presentation.common.TopBar
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import androidx.core.graphics.toColorInt
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import com.rtbishop.look4sat.presentation.common.isVerticalLayout
import com.rtbishop.look4sat.presentation.common.layoutPadding

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
private val labelRect = Rect()

fun NavGraphBuilder.mapDestination() {
    composable(Screen.Map.route) {
        val viewModel = viewModel(MapViewModel::class.java, factory = MapViewModel.Factory)
        val uiState = viewModel.uiState.collectAsStateWithLifecycle()
        val mapView = rememberMapViewWithLifecycle()
        MapScreen(uiState, mapView)
    }
}

@Composable
private fun MapScreen(uiState: State<MapState>, mapView: MapView) {
    val onItemClick = { item: OrbitalObject -> uiState.value.sendAction(MapAction.SelectItem(item)) }
    val selectPrev = { uiState.value.sendAction(MapAction.SelectPrev) }
    val selectNext = { uiState.value.sendAction(MapAction.SelectNext) }
    val rotateMod = Modifier.rotate(180f)
    val timeString = uiState.value.mapData?.aosTime ?: "00:00:00"
    val isTimeAos = uiState.value.mapData?.isTimeAos ?: true
    LaunchedEffect(uiState.value.track) {
        val latitude = uiState.value.track?.get(0)?.get(0)?.latitude ?: 0.0
        val longitude = uiState.value.track?.get(0)?.get(0)?.longitude ?: 0.0
        mapView.controller.animateTo(GeoPoint(latitude, longitude))
    }
    Column(modifier = Modifier.layoutPadding(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (isVerticalLayout()) {
            TopBar {
                IconCard(action = selectPrev, resId = R.drawable.ic_arrow, modifier = rotateMod)
                TimerRow(timeString = timeString, isTimeAos = isTimeAos)
                IconCard(action = selectNext, resId = R.drawable.ic_arrow)
            }
            TopBar { NextPassRow(pass = uiState.value.orbitalPass) }
        } else {
            TopBar {
                IconCard(action = selectPrev, resId = R.drawable.ic_arrow, modifier = rotateMod)
                TimerRow(timeString = timeString, isTimeAos = isTimeAos)
                NextPassRow(pass = uiState.value.orbitalPass, modifier = Modifier.weight(1f))
                IconCard(action = selectNext, resId = R.drawable.ic_arrow)
            }
        }
        ElevatedCard(modifier = Modifier.weight(1f)) {
            Box(contentAlignment = Alignment.BottomCenter) {
                AndroidView({ mapView }) { mapView ->
                    uiState.value.stationPosition?.let { setStationPosition(it, mapView) }
                    uiState.value.positions?.let { setPositions(it, mapView, onItemClick) }
                    uiState.value.track?.let { setSatelliteTrack(it, mapView) }
                    uiState.value.footprint?.let { setFootprint(it, mapView) }
                }
                uiState.value.mapData?.let { mapData ->
                    if (isVerticalLayout()) MapDataCard(mapData) else MapDataCards(mapData)
                }
            }
        }
    }
}

@Composable
private fun MapDataCard(data: MapData) {
    val textColor = MaterialTheme.colorScheme.primary
    val cardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
        Text(text = "© OpenStreetMap contributors", fontSize = 14.sp)
        Card(colors = cardColors) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.map_azimuth, data.azimuth), color = textColor)
                    Text(text = stringResource(R.string.map_elevation, data.elevation), color = textColor)
                }
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.map_altitude, data.altitude))
                    Text(text = stringResource(R.string.map_distance, data.range))
                }
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.map_latitude, data.osmPos.latitude), color = textColor)
                    Text(text = stringResource(R.string.map_longitude, data.osmPos.longitude), color = textColor)
                }
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.map_qth, data.qthLoc))
                    Text(text = stringResource(R.string.map_phase, data.phase))
                }
            }
        }
    }
}

@Composable
private fun MapDataCards(data: MapData) {
    val cardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    val paddingMod = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).width(160.dp)
    val osmText = "© OpenStreetMap contributors"
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
        Text(text = osmText, fontSize = 14.sp, modifier = Modifier.align(Alignment.BottomCenter))
        Card(colors = cardColors, modifier = Modifier.align(Alignment.BottomEnd)) {
            Column(horizontalAlignment = Alignment.End, modifier = paddingMod) {
                Text(text = stringResource(R.string.map_latitude, data.osmPos.latitude), color = textColor)
                Text(text = stringResource(R.string.map_longitude, data.osmPos.longitude))
            }
        }
    }
}

private fun setStationPosition(stationPos: GeoPos, mapView: MapView) {
    try {
        Marker(mapView).apply {
            setInfoWindow(null)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ContextCompat.getDrawable(mapView.context, R.drawable.ic_position)
            position = GeoPoint(stationPos.latitude, stationPos.longitude)
            mapView.overlays[0] = this
            mapView.invalidate()
        }
    } catch (exception: Exception) {
        println(exception)
    }
}

private fun setPositions(
    posMap: Map<OrbitalObject, GeoPos>,
    mapView: MapView,
    action: (OrbitalObject) -> Unit
) {
    val markers = FolderOverlay()
    try {
        posMap.entries.forEach {
            Marker(mapView).apply {
                setInfoWindow(null)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = getCustomTextIcon(it.key.data.name, mapView)
                try {
                    position = GeoPoint(it.value.latitude, it.value.longitude)
                } catch (exception: IllegalArgumentException) {
                    println(exception.stackTraceToString())
                }
                setOnMarkerClickListener { _, _ ->
                    action(it.key)
                    return@setOnMarkerClickListener true
                }
                markers.add(this)
            }
        }
        mapView.overlays[3] = markers
        mapView.invalidate()
    } catch (exception: Exception) {
        println(exception)
    }
}

private fun getCustomTextIcon(textLabel: String, mapView: MapView): Drawable {
    textPaint.getTextBounds(textLabel, 0, textLabel.length, labelRect)
    val iconSize = 10f
    val width = labelRect.width() + iconSize * 2f
    val height = textPaint.textSize * 3f + iconSize * 2f
    val bitmap = createBitmap(width.toInt(), height.toInt())
    Canvas(bitmap).run {
        drawCircle(width / 2f, height / 2f, iconSize, textPaint)
        drawText(textLabel, iconSize / 2f, height - iconSize, textPaint)
    }
    return bitmap.toDrawable(mapView.context.resources)
}

private fun setSatelliteTrack(satTrack: List<List<GeoPos>>, mapView: MapView) {
    val trackOverlay = FolderOverlay()
    try {
        satTrack.forEach { track ->
            val trackPoints = track.map { GeoPoint(it.latitude, it.longitude) }
            Polyline().apply {
                setPoints(trackPoints)
                outlinePaint.set(trackPaint)
                trackOverlay.add(this)
            }
        }
        mapView.overlays[1] = trackOverlay
    } catch (exception: Exception) {
        println(exception)
    }
}

private fun setFootprint(orbitalPos: OrbitalPos, mapView: MapView) {
    val footprintPoints = orbitalPos.getRangeCircle().map { GeoPoint(it.latitude, it.longitude) }
    try {
        val footprintOverlay = Polyline().apply {
            outlinePaint.set(footprintPaint)
            setPoints(footprintPoints)
        }
        mapView.overlays[2] = footprintOverlay
    } catch (exception: Exception) {
        println(exception)
    }
}

@Composable
private fun rememberMapViewWithLifecycle(): MapView {
    val tileSource = XYTileSource("tiles", 0, 6, 256, ".webp", emptyArray<String>())
    val context = LocalContext.current
    val mapView = remember { MapView(context) }.apply {
        setMultiTouchControls(true)
        setUseDataConnection(false)
        setTileSource(tileSource)
        minZoomLevel = getMinZoom(resources.displayMetrics.heightPixels)
        maxZoomLevel = 7.0
        controller.setCenter(GeoPoint(48.8575, 6.3514))
        controller.setZoom(minZoomLevel + 2)
        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        overlayManager.tilesOverlay.loadingBackgroundColor = Color.TRANSPARENT
        overlayManager.tilesOverlay.loadingLineColor = Color.TRANSPARENT
        overlayManager.tilesOverlay.setColorFilter(getColorFilter())
        setScrollableAreaLimitLatitude(maxLat, minLat, 0)
        // add overlays: 0 - GSP, 1 - SatTrack, 2 - SatFootprint, 3 - SatIcons
        overlays.addAll(Array(4) { FolderOverlay() })
    }
    // Makes MapView follow the lifecycle of this composable
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

@Composable
private fun getColorFilter(): ColorMatrixColorFilter {
    val grayScale = ColorMatrix().apply { setSaturation(0f) }
    val negative = ColorMatrix(
        floatArrayOf(-1f, 0f, 0f, 0f, 260f, 0f, -1f, 0f, 0f, 260f, 0f, 0f, -1f, 0f, 260f, 0f, 0f, 0f, 1f, 0f)
    )
    negative.preConcat(grayScale)
    return ColorMatrixColorFilter(negative)
}

@Composable
private fun getMinZoom(screenHeight: Int): Double {
    if (!isVerticalLayout()) return 3.5
    return MapView.getTileSystem().getLatitudeZoom(maxLat, minLat, screenHeight)
}
