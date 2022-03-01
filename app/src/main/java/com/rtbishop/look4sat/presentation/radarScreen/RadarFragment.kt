/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2021 Arty Bishop (bishop.arty@gmail.com)
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
package com.rtbishop.look4sat.presentation.radarScreen

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.databinding.FragmentRadarBinding
import com.rtbishop.look4sat.domain.predict.SatPass
import com.rtbishop.look4sat.domain.predict.SatPos
import com.rtbishop.look4sat.domain.toTimerString
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RadarFragment : Fragment(R.layout.fragment_radar) {

    private val viewModel: RadarViewModel by viewModels()
    private val navArgs: RadarFragmentArgs by navArgs()
    private val radioAdapter = RadioAdapter()
    private var binding: FragmentRadarBinding? = null
    private var radarView: RadarView? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentRadarBinding.bind(view).apply {
            radarRecycler.apply {
                setHasFixedSize(true)
                this.adapter = radioAdapter
                this.layoutManager = LinearLayoutManager(requireContext())
                (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
                addItemDecoration(DividerItemDecoration(requireContext(), 1))
            }
            setupObservers()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.enableSensor()
    }

    override fun onPause() {
        super.onPause()
        viewModel.disableSensor()
    }

    override fun onDestroyView() {
        binding?.radarRecycler?.adapter = null
        radarView = null
        binding = null
        super.onDestroyView()
    }

    private fun setupObservers() {
        viewModel.getPass(navArgs.catNum, navArgs.aosTime).observe(viewLifecycleOwner) { pass ->
            binding?.run {
                radarView = RadarView(requireContext()).apply {
                    setShowAim(viewModel.getUseCompass())
                    setScanning(viewModel.getShowSweep())
                }
                radarCard.addView(radarView)
                viewModel.radarData.observe(viewLifecycleOwner) { passData ->
                    radarView?.setPosition(passData.satPos)
                    radarView?.setPositions(passData.satTrack)
                    setPassText(pass, passData.satPos)
                }
                viewModel.transmitters.observe(viewLifecycleOwner) { list ->
                    if (list.isNotEmpty()) {
                        radioAdapter.submitList(list)
                        radarProgress.visibility = View.INVISIBLE
                    } else {
                        radarProgress.visibility = View.INVISIBLE
                        radarEmptyLayout.visibility = View.VISIBLE
                    }
                    radarView?.invalidate()
                }
                viewModel.orientation.observe(viewLifecycleOwner) { value ->
                    radarView?.setOrientation(value.first, value.second, value.third)
                }
                radarBtnBack.setOnClickListener { findNavController().navigateUp() }
                radarBtnMap.setOnClickListener {
                    val direction = RadarFragmentDirections.globalToMap(pass.catNum)
                    findNavController().navigate(direction)
                }
                radarBtnNotify.isEnabled = false
                radarBtnSettings.setOnClickListener {
                    val direction = RadarFragmentDirections.globalToSettings()
                    findNavController().navigate(direction)
                }
            }
        }
    }

    private fun setPassText(satPass: SatPass, satPos: SatPos) {
        binding?.run {
            val timeNow = System.currentTimeMillis()
            val radarAzim = getString(R.string.radar_az_value)
            val radarElev = getString(R.string.radar_el_value)
            val radarAlt = getString(R.string.radar_alt_value)
            val radarDist = getString(R.string.radar_dist_value)
            val radarId = getString(R.string.radar_sat_id)
            radarAzValue.text = String.format(radarAzim, Math.toDegrees(satPos.azimuth))
            radarElValue.text = String.format(radarElev, Math.toDegrees(satPos.elevation))
            radarAltValue.text = String.format(radarAlt, satPos.altitude)
            radarDstValue.text = String.format(radarDist, satPos.distance)
            radarSatId.text = String.format(radarId, satPass.catNum)
            if (!satPass.isDeepSpace) {
                if (timeNow < satPass.aosTime) {
                    val millisBeforeStart = satPass.aosTime.minus(timeNow)
                    radarTimer.text = millisBeforeStart.toTimerString()
                } else {
                    val millisBeforeEnd = satPass.losTime.minus(timeNow)
                    radarTimer.text = millisBeforeEnd.toTimerString()
                    if (timeNow > satPass.losTime) {
                        radarTimer.text = 0L.toTimerString()
                        findNavController().navigateUp()
                    }
                }
            } else radarTimer.text = 0L.toTimerString()
        }
    }
}
