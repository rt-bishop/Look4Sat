/*
 * Look4Sat. Amateur radio and weather satellite tracker and passes predictor for Android.
 * Copyright (C) 2019, 2020 Arty Bishop (bishop.arty@gmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.rtbishop.look4sat.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.github.amsacode.predict4java.GroundStationPosition
import com.github.amsacode.predict4java.TLE
import com.rtbishop.look4sat.Look4SatApp
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.dagger.ViewModelFactory
import com.rtbishop.look4sat.data.Result
import com.rtbishop.look4sat.data.SatPass
import com.rtbishop.look4sat.databinding.FragmentMapViewBinding
import com.rtbishop.look4sat.ui.MainActivity
import com.rtbishop.look4sat.ui.SharedViewModel
import com.rtbishop.look4sat.ui.views.MapView
import com.rtbishop.look4sat.utility.GeneralUtils
import com.rtbishop.look4sat.utility.GeneralUtils.toast
import com.rtbishop.look4sat.utility.PassPredictor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

class MapViewFragment : Fragment(R.layout.fragment_map_view) {

    @Inject
    lateinit var modelFactory: ViewModelFactory
    private lateinit var mainActivity: MainActivity
    private lateinit var viewModel: SharedViewModel
    private lateinit var mapView: MapView
    private lateinit var predictor: PassPredictor
    private lateinit var selectedSat: TLE
    private lateinit var binding: FragmentMapViewBinding
    private var gsp = GroundStationPosition(0.0, 0.0, 0.0)
    private var satPassList = emptyList<SatPass>()
    private var checkedItem = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMapViewBinding.bind(view)
        mainActivity = activity as MainActivity
        (mainActivity.application as Look4SatApp).appComponent.inject(this)
        viewModel = ViewModelProvider(mainActivity, modelFactory).get(SharedViewModel::class.java)
        setupObservers(binding)
    }

    private fun setupObservers(binding: FragmentMapViewBinding) {
        viewModel.getGSP().observe(viewLifecycleOwner, androidx.lifecycle.Observer { result ->
            when (result) {
                is Result.Success -> gsp = result.data
            }
        })

        viewModel.getPassList().observe(viewLifecycleOwner, androidx.lifecycle.Observer { result ->
            when (result) {
                is Result.Success -> {
                    satPassList = result.data
                    setupMapView(binding)
                }
            }
        })
    }

    private fun setupMapView(binding: FragmentMapViewBinding) {
        if (satPassList.isNotEmpty()) {
            satPassList = satPassList.distinctBy { it.tle }
            satPassList = satPassList.sortedBy { it.tle.name }
            binding.fabMap.setOnClickListener { showSelectSatDialog(satPassList) }
            selectedSat = satPassList.first().tle
            predictor = satPassList.first().predictor
            mapView = MapView(mainActivity, gsp)
            mapView.setList(satPassList)
            mapView.setChecked(checkedItem)
            binding.frameMap.addView(mapView)
            refreshView()
        } else {
            binding.fabMap.setOnClickListener {
                getString(R.string.err_no_sat_selected).toast(mainActivity)
            }
        }
    }

    private fun refreshView() {
        lifecycleScope.launch {
            while (true) {
                mapView.invalidate()
                setTextViewsToSelectedSatPos()
                delay(viewModel.getRefreshRate())
            }
        }
    }

    private fun showSelectSatDialog(list: List<SatPass>) {
        val tleArray = arrayOfNulls<String>(list.size).apply {
            list.withIndex().forEach {
                this[it.index] = it.value.tle.name
            }
        }

        val builder = AlertDialog.Builder(mainActivity)
        builder.setTitle(getString(R.string.dialog_show_track))
            .setSingleChoiceItems(tleArray, checkedItem) { dialog, which ->
                checkedItem = which
                selectedSat = list[which].tle
                predictor = list[which].predictor
                mapView.setChecked(checkedItem)
                mapView.invalidate()
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun setTextViewsToSelectedSatPos() {
        val currentTime = GeneralUtils.getDateFor(System.currentTimeMillis())
        val orbitalPeriod = (24 * 60 / selectedSat.meanmo).toInt()
        val positions = predictor.getPositions(currentTime, 60, 0, orbitalPeriod * 3)
        var lon = GeneralUtils.rad2Deg(positions[0].longitude).toFloat()
        val lat = GeneralUtils.rad2Deg(positions[0].latitude).toFloat()
        val rng = positions[0].range

        if (lon > 180f) lon -= 360f

        binding.tvMapLat.text = String.format(mainActivity.getString(R.string.pat_latitude), lat)
        binding.tvMapLon.text = String.format(mainActivity.getString(R.string.pat_longitude), lon)
        binding.tvMapRng.text = String.format(mainActivity.getString(R.string.pat_distance), rng)
    }
}
