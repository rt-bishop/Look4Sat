package com.rtbishop.look4sat.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.databinding.FragmentMapOsmBinding
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.OverlayItem

class MapOsmFragment : Fragment(R.layout.fragment_map_osm) {

    private lateinit var binding: FragmentMapOsmBinding
    private lateinit var mapView: MapView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val appContext = requireActivity().applicationContext

        binding = FragmentMapOsmBinding.bind(view)
        binding.mapView.apply {
            controller.animateTo(mapCenter)
            controller.setZoom(2.5)
            setMultiTouchControls(true)
            maxZoomLevel = 7.5
            minZoomLevel = 2.5
//            isClickable = true
//            setScrollableAreaLimitLatitude(-90.0, 90.0, 0)
//            setScrollableAreaLimitLongitude(-180.0, 180.0, 0)
        }

        Configuration
            .getInstance()
            .load(appContext, PreferenceManager.getDefaultSharedPreferences(appContext))

        init()
    }

    private fun init() {
        mapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        val gPt = GeoPoint(28.6337874, 77.35767599999997)
        val gPt2 = GeoPoint(28.638472, 77.360667)
        mapView.controller.setCenter(gPt)

        /*place icon on map*/
        val items = ArrayList<OverlayItem>()
        items.add(OverlayItem("Current Location", "Amrapali Village, Indirapuram", gPt))
        items.add(OverlayItem("Destination", "Aditya Mall, Indirapuram", gPt2))
    }

    override fun onResume() {
        super.onResume()
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        mapView.onResume() //needed for compass, my location overlays, v6.0.0 and up
    }

    override fun onPause() {
        super.onPause()
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        mapView.onPause()  //needed for compass, my location overlays, v6.0.0 and up
    }
}