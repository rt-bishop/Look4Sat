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
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.databinding.FragmentRadarBinding
import com.rtbishop.look4sat.domain.predict.SatPass
import com.rtbishop.look4sat.domain.predict.SatPos
import com.rtbishop.look4sat.framework.SettingsProvider
import com.rtbishop.look4sat.presentation.navigateSafe
import com.rtbishop.look4sat.presentation.toTimerString
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RadarFragment : Fragment(R.layout.fragment_radar) {

    @Inject
    lateinit var preferences: SettingsProvider
    private val viewModel: RadarViewModel by viewModels()
    private var radarView: RadarView? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupComponents(view)
    }

    private fun setupComponents(view: View) {
        val context = requireContext()
        val adapter = TransmittersAdapter()
        val layoutManager = LinearLayoutManager(context)
        val itemDecoration = DividerItemDecoration(context, layoutManager.orientation)
        FragmentRadarBinding.bind(view).apply {
            radarRecycler.apply {
                setHasFixedSize(true)
                this.adapter = adapter
                this.layoutManager = LinearLayoutManager(context)
                addItemDecoration(itemDecoration)
                (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
            }
            setupObservers(adapter, this)
        }
    }

    private fun setupObservers(
        transmittersAdapter: TransmittersAdapter,
        binding: FragmentRadarBinding
    ) {
        val catNum = requireArguments().getInt("catNum")
        val aosTime = requireArguments().getLong("aosTime")
        viewModel.getPass(catNum, aosTime).observe(viewLifecycleOwner) { pass ->
            radarView = RadarView(requireContext()).apply {
                setShowAim(preferences.getUseCompass())
                setScanning(preferences.getShowSweep())
            }
            binding.radarCard.addView(radarView)
            viewModel.radarData.observe(viewLifecycleOwner) { passData ->
                radarView?.setPosition(passData.satPos)
                radarView?.setPositions(passData.satTrack)
                setPassText(pass, passData.satPos, binding)
            }
            viewModel.transmitters.observe(viewLifecycleOwner) { list ->
                if (list.isNotEmpty()) {
                    transmittersAdapter.submitList(list)
                    binding.radarRecyclerMsg.text = getString(R.string.trans_data)
                } else {
                    binding.radarRecyclerMsg.text = getString(R.string.trans_no_data)
                }
                radarView?.invalidate()
            }
            viewModel.orientation.observe(viewLifecycleOwner) { orientation ->
                radarView?.setOrientation(
                    orientation.first,
                    orientation.second,
                    orientation.third
                )
            }
            binding.radarMap.setOnClickListener {
                val bundle = bundleOf("catNum" to pass.catNum)
                findNavController().navigateSafe(R.id.action_radar_to_map, bundle)
            }
        }
    }

    private fun setPassText(satPass: SatPass, satPos: SatPos, binding: FragmentRadarBinding) {
        val timeNow = System.currentTimeMillis()
        val polarAz = getString(R.string.pat_azimuth)
        val polarEl = getString(R.string.pat_elevation)
        val polarRng = getString(R.string.pat_distance)
        val polarAlt = getString(R.string.pat_altitude)
        binding.radarAz.text = String.format(polarAz, Math.toDegrees(satPos.azimuth))
        binding.radarEl.text = String.format(polarEl, Math.toDegrees(satPos.elevation))
        binding.radarDst.text = String.format(polarRng, satPos.range)
        binding.radarAlt.text = String.format(polarAlt, satPos.altitude)
        binding.radarName.text = satPass.name

        if (!satPass.isDeepSpace) {
            if (timeNow < satPass.aosTime) {
                val millisBeforeStart = satPass.aosTime.minus(timeNow)
                binding.radarTimer.text = millisBeforeStart.toTimerString()
            } else {
                val millisBeforeEnd = satPass.losTime.minus(timeNow)
                binding.radarTimer.text = millisBeforeEnd.toTimerString()
                if (timeNow > satPass.losTime) {
                    binding.radarTimer.text = 0L.toTimerString()
                    findNavController().navigateUp()
                }
            }
        } else {
            binding.radarTimer.text = 0L.toTimerString()
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
}
