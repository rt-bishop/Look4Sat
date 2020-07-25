package com.rtbishop.look4sat.ui.fragments

import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.github.amsacode.predict4java.GroundStationPosition
import com.github.amsacode.predict4java.SatPos
import com.rtbishop.look4sat.Look4SatApp
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.dagger.ViewModelFactory
import com.rtbishop.look4sat.data.Result
import com.rtbishop.look4sat.data.SatPass
import com.rtbishop.look4sat.databinding.FragmentMapOsmBinding
import com.rtbishop.look4sat.ui.MainActivity
import com.rtbishop.look4sat.ui.SharedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.*
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.gridlines.LatLonGridlineOverlay2
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.*
import javax.inject.Inject

class MapOsmFragment : Fragment(R.layout.fragment_map_osm) {

    @Inject
    lateinit var modelFactory: ViewModelFactory
    private lateinit var mainActivity: MainActivity
    private lateinit var viewModel: SharedViewModel
    private lateinit var binding: FragmentMapOsmBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMapOsmBinding.bind(view)
        mainActivity = requireActivity() as MainActivity
        (mainActivity.application as Look4SatApp).appComponent.inject(this)
        viewModel = ViewModelProvider(mainActivity, modelFactory).get(SharedViewModel::class.java)

        val prefs = PreferenceManager.getDefaultSharedPreferences(mainActivity.applicationContext)
        Configuration.getInstance().load(mainActivity.applicationContext, prefs)

        setupMapView()
        setupObservers()
    }

    private fun setupMapView() {
        binding.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)

            // General settings
//            isHorizontalMapRepetitionEnabled = false
//            isVerticalMapRepetitionEnabled = false
            overlayManager.tilesOverlay.loadingBackgroundColor = Color.TRANSPARENT
            overlayManager.tilesOverlay.loadingLineColor = Color.TRANSPARENT


            // Zoom settings
            minZoomLevel = 2.5
            maxZoomLevel = 6.5
            controller.setZoom(minZoomLevel)
//            controller.setCenter(GeoPoint(0.0, 0.0))
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

            // Scroll limits
//            setScrollableAreaLimitLatitude(85.0, -85.0, 0)
//            setScrollableAreaLimitLongitude(-180.0, 180.0, 0)
            setScrollableAreaLimitDouble(BoundingBox(85.0, 180.0, -85.0, -180.0))

            addLocationOverlay()
            addMapScaleBarOverlay()
            addSomeIcons()
            addColorFilter()
        }
    }

    private fun setupObservers() {
        viewModel.getGSP().observe(viewLifecycleOwner, Observer { stationPosition ->
            when (stationPosition) {
                is Result.Success -> {
//                    setUserLocation(stationPosition.data)
                }
            }
        })

        viewModel.getPassList().observe(viewLifecycleOwner, Observer { satPasses ->
            when (satPasses) {
                is Result.Success -> {
                    val filteredPasses = satPasses.data.distinctBy { it.tle }
//                    setupSatOverlay(filteredPasses)
                }
            }
        })
    }

    private fun setUserLocation(position: GroundStationPosition) {
        val startPoint = GeoPoint(position.latitude, position.longitude)
        Marker(binding.mapView).apply {
            this.position = startPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Ground Station Position"
        }
    }

    private fun addCompassOverlay() {
        val overlay =
            CompassOverlay(context, InternalCompassOrientationProvider(context), binding.mapView)
        overlay.enableCompass()
        binding.mapView.overlays.add(overlay)

    }

    private fun addLocationOverlay() {
        val overlay = MyLocationNewOverlay(GpsMyLocationProvider(mainActivity), binding.mapView)
        overlay.enableMyLocation()
        binding.mapView.overlays.add(overlay)
    }

    private fun addGridLineOverlay() {
        val overlay = LatLonGridlineOverlay2()
        binding.mapView.overlays.add(overlay)
    }

    private fun addMapScaleBarOverlay() {
        val dm = mainActivity.resources.displayMetrics
        val overlay = ScaleBarOverlay(binding.mapView).apply {
            setCentred(true)
            setScaleBarOffset(dm.widthPixels / 2, 10)
        }
        binding.mapView.overlays.add(overlay)
    }

    private fun addMiniMap() {
        val dm = mainActivity.resources.displayMetrics
        val overlay =
            MinimapOverlay(mainActivity, binding.mapView.tileRequestCompleteHandler).apply {
                width = dm.widthPixels / 5
                height = dm.heightPixels / 5
            }
        binding.mapView.overlays.add(overlay)
    }

    private fun addSomeIcons() {
        val items = mutableListOf<OverlayItem>()
        items.add(OverlayItem("Title", "Description", GeoPoint(0.0, 0.0)))

        val listener = object : ItemizedIconOverlay.OnItemGestureListener<OverlayItem> {
            override fun onItemLongPress(index: Int, item: OverlayItem?): Boolean {
                return true
            }

            override fun onItemSingleTapUp(index: Int, item: OverlayItem?): Boolean {
                Log.d("myTag", "${item?.title}")
                return true
            }
        }

        val overlay = ItemizedIconOverlay(mainActivity, items, listener)
        binding.mapView.overlays.add(overlay)
    }

    private fun addColorFilter() {

        val negativeArray = floatArrayOf(
            -1.0f, .0f, .0f, .0f, 255.0f,
            .0f, -1.0f, .0f, .0f, 255.0f,
            .0f, .0f, -1.0f, .0f, 255.0f,
            .0f, .0f, .0f, 1.0f, .0f
        )
        val negativeMatrix = ColorMatrix(negativeArray)

        val destinationColor = Color.parseColor("#FF2A2A2A")
        val lr = (255.0f - Color.red(destinationColor)) / 255.0f
        val lg = (255.0f - Color.green(destinationColor)) / 255.0f
        val lb = (255.0f - Color.blue(destinationColor)) / 255.0f
        val grayScaleArray = floatArrayOf(
            lr, lg, lb, 0f, 0f, //
            lr, lg, lb, 0f, 0f, //
            lr, lg, lb, 0f, 0f, //
            0f, 0f, 0f, 0f, 255f //
        )
        val grayScaleMatrix = ColorMatrix(grayScaleArray)
        grayScaleMatrix.preConcat(negativeMatrix)

        val dr = Color.red(destinationColor)
        val dg = Color.green(destinationColor)
        val db = Color.blue(destinationColor)
        val drf = dr / 255f
        val dgf = dg / 255f
        val dbf = db / 255f
        val tintArray = floatArrayOf(
            drf, 0f, 0f, 0f, 0f, //
            0f, dgf, 0f, 0f, 0f, //
            0f, 0f, dbf, 0f, 0f, //
            0f, 0f, 0f, 1f, 0f //
        )
        val tintMatrix = ColorMatrix(tintArray)
        tintMatrix.preConcat(grayScaleMatrix)

        val lDest = drf * lr + dgf * lg + dbf * lb
        val scale = 1f - lDest
        val translate = 1 - scale * 0.5f

        val scaleArray = floatArrayOf(
            scale, 0f, 0f, 0f, dr * translate, //
            0f, scale, 0f, 0f, dg * translate, //
            0f, 0f, scale, 0f, db * translate, //
            0f, 0f, 0f, 1f, 0f
        )
        val scaleMatrix = ColorMatrix(scaleArray)
        scaleMatrix.preConcat(tintMatrix)

        val filter = ColorMatrixColorFilter(scaleMatrix)

        binding.mapView.overlayManager.tilesOverlay.setColorFilter(filter)
    }

    private fun setupSatOverlay(passList: List<SatPass>) {
        lifecycleScope.launch {
            while (true) {
//                val markers = showSatIcons(data)
//                satLayer.removeAllItems()
//                satLayer.addItems(markers)
                delay(1000)
            }
        }
    }

    private suspend fun drawGroundTrack(data: List<SatPass>) {
        withContext(Dispatchers.IO) {
            data[1].apply {
                val dateNow = Date(System.currentTimeMillis())
                val period = (24 * 60 / this.tle.meanmo).toInt()
                val positions = this.predictor.getPositions(dateNow, 15, 0, period)
                positions.forEach {
                    val lat = Math.toDegrees(it.latitude)
                    var lon = Math.toDegrees(it.longitude)
//                    Log.d("myTag", "$lat, $lon")
                    if (lon > 180.0) {
//                        path.addPoint(GeoPoint(lat, 180.0))
                        lon -= 360.0
                    }
                    Log.d("myTag", "$lat, $lon")
                }
            }
//            binding.mapView.map().layers().add(path)
//            binding.mapView.map().render()
        }
    }

    private fun drawGroundTrack(list: List<SatPos>) {
        val groundTrack = Polyline(binding.mapView)
        val trackPoints = mutableListOf<GeoPoint>()

        list.forEach {
            val lat = Math.toDegrees(it.latitude) * -1
            var lon = Math.toDegrees(it.longitude)
            if (lon > 180.0) lon -= 360.0
            trackPoints.add(GeoPoint(lat, lon))
        }

        groundTrack.setPoints(trackPoints)
        groundTrack.setDensityMultiplier(0.5f)
        binding.mapView.overlays.add(groundTrack)
        binding.mapView.invalidate()
    }

    private fun drawPosition(position: GeoPoint) {
        val satPosition = Polygon(binding.mapView)
        val trackPoints = mutableListOf<GeoPoint>()

        val lat = Math.toDegrees(position.latitude)
        var lon = Math.toDegrees(position.longitude)
        if (lon > 180.0) lon -= 360.0
        trackPoints.add(GeoPoint(lat, lon))

        val startMarker = Marker(binding.mapView)
        startMarker.position = GeoPoint(lat, lon)
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        startMarker.title = "Sat Position"
        binding.mapView.overlays.add(startMarker)

        binding.mapView.overlays.add(satPosition)
        satPosition.points = trackPoints
        binding.mapView.invalidate()
    }

    private suspend fun showSatIcons(data: List<SatPass>) {
        withContext(Dispatchers.Default) {
            val dateNow = Date(System.currentTimeMillis())
            data.forEach {
                val satPos = it.predictor.getSatPos(dateNow)
                val lat = Math.toDegrees(satPos.latitude)
                var lon = Math.toDegrees(satPos.longitude)
                if (lon > 180) lon -= 360
                Log.d("myTag", "${it.tle.name}, $lat, $lon")
            }
        }
    }

    override fun onPause() {
        binding.mapView.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }
}