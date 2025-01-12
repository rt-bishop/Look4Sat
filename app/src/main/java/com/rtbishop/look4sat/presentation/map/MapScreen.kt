package com.rtbishop.look4sat.presentation.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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
import com.rtbishop.look4sat.presentation.components.CardIcon
import com.rtbishop.look4sat.presentation.components.NextPassRow
import com.rtbishop.look4sat.presentation.components.TimerBar
import com.rtbishop.look4sat.presentation.components.TimerRow
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

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
    color = Color.parseColor("#FFE082")
    setShadowLayer(3f, 3f, 3f, Color.BLACK)
}
private val labelRect = Rect()

fun NavGraphBuilder.mapDestination() {
    composable(Screen.Map.route) {
        val viewModel = viewModel(MapViewModel::class.java, factory = MapViewModel.Factory)
        val uiState = viewModel.uiState.collectAsStateWithLifecycle()
        MapScreen(uiState)
    }
}

@Composable
private fun MapScreen(uiState: State<MapState>) {
    val onItemClick = { item: OrbitalObject -> uiState.value.sendAction(MapAction.SelectItem(item)) }
    val selectPrev = { uiState.value.sendAction(MapAction.SelectPrev) }
    val selectNext = { uiState.value.sendAction(MapAction.SelectNext) }
    val rotateMod = Modifier.rotate(180f)
    val timeString = uiState.value.mapData?.aosTime ?: "00:00:00"
    val isTimeAos = uiState.value.mapData?.isTimeAos ?: true
    val osmInfo = "Wikimedia maps | Map data © OpenStreetMap contributors"
    Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TimerRow {
            CardIcon(onClick = selectPrev, iconId = R.drawable.ic_arrow, modifier = rotateMod)
            TimerBar(timeString = timeString, isTimeAos = isTimeAos)
            CardIcon(onClick = selectNext, iconId = R.drawable.ic_arrow)
        }
        NextPassRow(pass = uiState.value.orbitalPass)
        ElevatedCard(modifier = Modifier.weight(1f)) {
            Box(contentAlignment = Alignment.BottomCenter) {
                MapView(isLightUi = uiState.value.isLightUi) { mapView ->
                    uiState.value.stationPosition?.let { setStationPosition(it, mapView) }
                    uiState.value.positions?.let { setPositions(it, mapView, onItemClick) }
                    uiState.value.track?.let { setSatelliteTrack(it, mapView) }
                    uiState.value.footprint?.let { setFootprint(it, mapView) }
                }
                Text(text = osmInfo, textAlign = TextAlign.Center, fontSize = 14.sp)
            }
        }
        uiState.value.mapData?.let { mapData ->
            ElevatedCard {
                MapDataCard(mapData)
            }
        }
    }
}

@Composable
private fun MapDataCard(mapData: MapData) {
    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.map_period, mapData.period), color = MaterialTheme.colorScheme.primary)
            Text(text = stringResource(R.string.map_phase, mapData.phase), color = MaterialTheme.colorScheme.primary)
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.map_azimuth, mapData.azimuth))
            Text(text = stringResource(R.string.map_elevation, mapData.elevation))
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.map_altitude, mapData.altitude), color = MaterialTheme.colorScheme.primary)
            Text(text = stringResource(R.string.map_distance, mapData.range), color = MaterialTheme.colorScheme.primary)
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.map_latitude, mapData.osmPos.latitude))
            Text(text = stringResource(R.string.map_longitude, mapData.osmPos.longitude))
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.map_qth, mapData.qthLoc), color = MaterialTheme.colorScheme.primary)
            Text(text = stringResource(R.string.map_velocity, mapData.velocity), color = MaterialTheme.colorScheme.primary)
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
    val bitmap = Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888)
    Canvas(bitmap).run {
        drawCircle(width / 2f, height / 2f, iconSize, textPaint)
        drawText(textLabel, iconSize / 2f, height - iconSize, textPaint)
    }
    return BitmapDrawable(mapView.context.resources, bitmap)
}

private fun setSatelliteTrack(satTrack: List<List<GeoPos>>, mapView: MapView) {
//    val center = GeoPoint(satTrack[0][0].lat, satTrack[0][0].lon)
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
//    mapView.controller.animateTo(center)
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
private fun MapView(
    modifier: Modifier = Modifier,
    isLightUi: Boolean,
    update: ((map: MapView) -> Unit)? = null
) {
    val mapView = rememberMapViewWithLifecycle(isLightUi)
    AndroidView({ mapView }, modifier) { update?.invoke(it) }
}

@Composable
private fun rememberMapViewWithLifecycle(isLightTheme: Boolean): MapView {
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
        overlayManager.tilesOverlay.setColorFilter(getColorFilter(isLightTheme))
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
private fun getColorFilter(isLightTheme: Boolean): ColorMatrixColorFilter {
    val grayScale = ColorMatrix().apply { setSaturation(0f) }
    val negative = ColorMatrix(
        floatArrayOf(-1f, 0f, 0f, 0f, 260f, 0f, -1f, 0f, 0f, 260f, 0f, 0f, -1f, 0f, 260f, 0f, 0f, 0f, 1f, 0f)
    )
    negative.preConcat(grayScale)
    return if (isLightTheme) ColorMatrixColorFilter(grayScale) else ColorMatrixColorFilter(negative)
}

private fun getMinZoom(screenHeight: Int): Double {
    return MapView.getTileSystem().getLatitudeZoom(maxLat, minLat, screenHeight)
}
