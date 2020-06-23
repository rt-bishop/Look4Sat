package com.rtbishop.look4sat.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
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
import com.rtbishop.look4sat.utility.GeneralUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.mapsforge.map.rendertheme.XmlRenderTheme
import org.mapsforge.map.rendertheme.XmlRenderThemeMenuCallback
import org.osmdroid.mapsforge.MapsForgeTileProvider
import org.osmdroid.mapsforge.MapsForgeTileSource
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import java.io.File
import java.io.InputStream
import java.util.*
import javax.inject.Inject

class MapOsmFragment : Fragment(R.layout.fragment_map_osm) {

    @Inject
    lateinit var modelFactory: ViewModelFactory
    private lateinit var mainActivity: MainActivity
    private lateinit var viewModel: SharedViewModel
    private lateinit var binding: FragmentMapOsmBinding
    private var satPassList = emptyList<SatPass>()
    private var gsp = GroundStationPosition(0.0, 0.0, 0.0)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMapOsmBinding.bind(view)
        mainActivity = requireActivity() as MainActivity
        (mainActivity.application as Look4SatApp).appComponent.inject(this)
        viewModel = ViewModelProvider(mainActivity, modelFactory).get(SharedViewModel::class.java)

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.getGSP().observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is Result.Success -> gsp = result.data
            }
        })

        viewModel.getPassList().observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is Result.Success -> {
                    satPassList = result.data
                    setupMapWithAdapter()
                }
            }
        })
    }

    inner class MyRenderTheme : XmlRenderTheme {

        override fun getRenderThemeAsStream(): InputStream {
            return resources.openRawResource(R.raw.customrender)
        }

        override fun getMenuCallback(): XmlRenderThemeMenuCallback? {
            return null
        }

        override fun getRelativePathPrefix(): String {
            return ""
        }

        override fun setMenuCallback(menuCallback: XmlRenderThemeMenuCallback) {}

    }

    private fun setupMapWithAdapter() {
        val mapsArray = arrayOf(File(Environment.getExternalStorageDirectory(), "world.map"))
        val tileSource = MapsForgeTileSource.createFromFiles(mapsArray, MyRenderTheme(), null)
        val registerReceiver = SimpleRegisterReceiver(mainActivity.applicationContext)
        val tileProvider = MapsForgeTileProvider(registerReceiver, tileSource, null)

        binding.mapView.tileProvider = tileProvider

        binding.mapView.apply {
            overlayManager.tilesOverlay.loadingBackgroundColor = Color.TRANSPARENT
            overlayManager.tilesOverlay.loadingLineColor = Color.TRANSPARENT
            minZoomLevel = 3.00
            maxZoomLevel = 8.00
            controller.setZoom(5.0)

            val startPoint = GeoPoint(gsp.latitude, gsp.longitude)
            controller.setCenter(startPoint)

            val startMarker = Marker(binding.mapView)
            startMarker.position = startPoint
            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            startMarker.title = "Ground Station Position"
            overlays.add(startMarker)

            var dateNow = Date(System.currentTimeMillis())
            val selectedSat = satPassList[3]
            val orbitalPeriod = (24 * 60 / selectedSat.tle.meanmo).toInt()
            var positions = selectedSat.predictor.getPositions(dateNow, 60, 0, orbitalPeriod * 3)

            drawGroundTrack(positions)
            Log.d("myTag", selectedSat.tle.name)

            val compassOverlay = CompassOverlay(
                context,
                InternalCompassOrientationProvider(context),
                binding.mapView
            )
            compassOverlay.enableCompass()
            overlays.add(compassOverlay)

            setMultiTouchControls(true)
            setScrollableAreaLimitLatitude(85.0, -85.0, 0)

            lifecycleScope.launch {
                while (true) {
                    delay(3000)
                    dateNow = Date(System.currentTimeMillis())
                    positions =
                        selectedSat.predictor.getPositions(dateNow, 60, 0, orbitalPeriod * 3)
                    drawPosition(GeoPoint(positions[0].latitude, positions[0].longitude))
                    binding.mapView.invalidate()
                }
            }
        }
    }

    private fun drawPosition(position: GeoPoint) {
        val satPosition = Polygon(binding.mapView)
        val trackPoints = mutableListOf<GeoPoint>()

        Log.d("myTag", "${position.latitude}, ${position.longitude}")

        val lat = GeneralUtils.rad2Deg(position.latitude)
        var lon = GeneralUtils.rad2Deg(position.longitude)
        if (lon > 180.0) lon -= 360.0
        trackPoints.add(GeoPoint(lat, lon))

        val startMarker = Marker(binding.mapView)
        startMarker.position = GeoPoint(lat, lon)
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        startMarker.title = "Sat Position"
        binding.mapView.overlays.add(startMarker)

        Log.d("myTag", "$lat, $lon")

        binding.mapView.overlays.add(satPosition)
        satPosition.points = trackPoints
        binding.mapView.invalidate()
    }

    private fun drawGroundTrack(list: List<SatPos>) {
        val groundTrack = Polyline(binding.mapView)
        val trackPoints = mutableListOf<GeoPoint>()
        var lat = 0.0
        var lon = 0.0

        Log.d("myTag", "${list[1].latitude}, ${list[1].longitude}")

        list.forEach {
            lat = GeneralUtils.rad2Deg(it.latitude) * -1
            lon = GeneralUtils.rad2Deg(it.longitude)
            if (lon > 180.0) lon -= 360.0
            trackPoints.add(GeoPoint(lat, lon))
        }

        Log.d("myTag", "$lat, $lon")

        groundTrack.setPoints(trackPoints)
        groundTrack.setDensityMultiplier(0.5f)
        binding.mapView.overlays.add(groundTrack)
        binding.mapView.invalidate()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }
}