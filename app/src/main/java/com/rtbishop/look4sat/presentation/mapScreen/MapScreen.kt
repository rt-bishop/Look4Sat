package com.rtbishop.look4sat.presentation.mapScreen

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.domain.predict.GeoPos
import com.rtbishop.look4sat.domain.predict.SatPos
import com.rtbishop.look4sat.domain.predict.Satellite
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import timber.log.Timber

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

@Composable
fun MapScreen(viewModel: MapViewModel = hiltViewModel()) {
    val context = LocalContext.current
    Configuration.getInstance().load(context, viewModel.getPreferences())
    viewModel.selectDefaultSatellite(-1)
    val stationPos = viewModel.stationPos.observeAsState()
    val positions = viewModel.positions.observeAsState()
    val satTrack = viewModel.track.observeAsState()
    val footprint = viewModel.footprint.observeAsState()
    val positionClick = { satellite: Satellite -> viewModel.selectSatellite(satellite) }
    Timber.d("MapScreen recomposition")
    Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ElevatedCard(modifier = Modifier.fillMaxSize()) {
            MapView(modifier = Modifier.fillMaxSize()) { mapView ->
                Timber.d("MapView recomposition")
                stationPos.value?.let { setStationPosition(it, mapView) }
                positions.value?.let { setPositions(it, mapView, positionClick) }
                satTrack.value?.let { setSatelliteTrack(it, mapView) }
                footprint.value?.let { setFootprint(it, mapView) }
            }
        }
    }
}

private fun setStationPosition(stationPos: GeoPos, mapView: MapView) {
    Marker(mapView).apply {
        setInfoWindow(null)
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        icon = ContextCompat.getDrawable(mapView.context, R.drawable.ic_position)
        position = GeoPoint(stationPos.lat, stationPos.lon)
        mapView.overlays[0] = this
        mapView.invalidate()
    }
}

private fun setPositions(
    posMap: Map<Satellite, GeoPos>, mapView: MapView, action: (Satellite) -> Unit
) {
    val markers = FolderOverlay()
    posMap.entries.forEach {
        Marker(mapView).apply {
            setInfoWindow(null)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = getCustomTextIcon(it.key.data.name, mapView)
            try {
                position = GeoPoint(it.value.lat, it.value.lon)
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
}

private fun getCustomTextIcon(textLabel: String, mapView: MapView): Drawable {
    textPaint.getTextBounds(textLabel, 0, textLabel.length, labelRect)
    val iconSize = 8f
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
    mapView.overlays[1] = trackOverlay
//    mapView.controller.animateTo(center)
}

private fun setFootprint(satPos: SatPos, mapView: MapView) {
    val footprintPoints = satPos.getRangeCircle().map { GeoPoint(it.lat, it.lon) }
    val footprintOverlay = Polyline().apply {
        outlinePaint.set(footprintPaint)
        try {
            setPoints(footprintPoints)
        } catch (exception: IllegalArgumentException) {
            println(exception.stackTraceToString())
        }
    }
    mapView.overlays[2] = footprintOverlay
}

//private fun handleMapData(mapData: MapData) {
//    binding.apply {
//        mapTimer.text = mapData.aosTime
//        mapDataPeriod.text = String.format(getString(R.string.map_period, mapData.period))
//        mapDataPhase.text = String.format(getString(R.string.map_phase), mapData.phase)
//        mapAzimuth.text = String.format(getString(R.string.map_azimuth), mapData.azimuth)
//        mapElevation.text = String.format(getString(R.string.map_elevation), mapData.elevation)
//        mapDataAlt.text = String.format(getString(R.string.map_altitude), mapData.altitude)
//        mapDataDst.text = String.format(getString(R.string.map_distance), mapData.range)
//        mapDataLat.text =
//            String.format(getString(R.string.map_latitude), mapData.osmPos.lat)
//        mapDataLon.text =
//            String.format(getString(R.string.map_longitude), mapData.osmPos.lon)
//        mapDataQth.text = String.format(getString(R.string.map_qth), mapData.qthLoc)
//        mapDataVel.text = String.format(getString(R.string.map_velocity), mapData.velocity)
//        if (mapData.eclipsed) {
//            mapDataVisibility.text = getString(R.string.map_eclipsed)
//        } else {
//            mapDataVisibility.text = getString(R.string.map_visible)
//        }
//    }
//    binding.mapView.invalidate()
//}

@Composable
private fun MapView(modifier: Modifier = Modifier, update: ((map: MapView) -> Unit)? = null) {
    val mapView = rememberMapViewWithLifecycle()
    AndroidView({ mapView }, modifier) { update?.invoke(it) }
}

@Composable
private fun rememberMapViewWithLifecycle(context: Context = LocalContext.current): MapView {
    // add clipToOutline = true if needed
    val mapView = remember { MapView(context).apply { id = R.id.osm_map_view } }.apply {
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

private fun getColorFilter(): ColorMatrixColorFilter {
    val grayScaleMatrix = ColorMatrix().apply { setSaturation(0f) }
    val negativeMatrix = ColorMatrix(
        floatArrayOf(
            -1f, 0f, 0f, 0f, 255f, 0f, -1f, 0f, 0f, 255f, 0f, 0f, -1f, 0f, 255f, 0f, 0f, 0f, 1f, 0f
        )
    )
    negativeMatrix.preConcat(grayScaleMatrix)
    return ColorMatrixColorFilter(negativeMatrix)
}

private fun getMinZoom(screenHeight: Int): Double {
    return MapView.getTileSystem().getLatitudeZoom(maxLat, minLat, screenHeight)
}
