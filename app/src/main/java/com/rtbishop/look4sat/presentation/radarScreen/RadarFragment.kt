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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.databinding.FragmentRadarBinding
import com.rtbishop.look4sat.framework.PreferencesSource
import com.rtbishop.look4sat.predict4kotlin.SatPass
import com.rtbishop.look4sat.predict4kotlin.SatPos
import com.rtbishop.look4sat.presentation.ItemDivider
import com.rtbishop.look4sat.utility.navigateSafe
import com.rtbishop.look4sat.utility.toTimerString
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RadarFragment : Fragment(R.layout.fragment_radar) {

    @Inject
    lateinit var preferences: PreferencesSource
    private val viewModel: RadarViewModel by viewModels()
    private var radarView: RadarView? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        FragmentRadarBinding.bind(view).apply {
            val transAdapter = TransmittersAdapter()
            radarRecycler.apply {
                setHasFixedSize(true)
                adapter = transAdapter
                isVerticalScrollBarEnabled = false
                layoutManager = LinearLayoutManager(context)
                (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
                addItemDecoration(ItemDivider(R.drawable.rec_divider_dark))
            }
            setupObservers(transAdapter, this)
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

    private fun setupObservers(
        transmittersAdapter: TransmittersAdapter,
        binding: FragmentRadarBinding
    ) {
        val catNum = requireArguments().getInt("catNum")
        val aosTime = requireArguments().getLong("aosTime")
        viewModel.getPass(catNum, aosTime).observe(viewLifecycleOwner) { pass ->
            radarView = RadarView(requireContext()).apply {
                setShowAim(preferences.shouldUseCompass())
                setScanning(preferences.shouldShowSweep())
            }
            binding.radarFrame.addView(radarView)
            viewModel.radarData.observe(viewLifecycleOwner, { passData ->
                radarView?.setPosition(passData.satPos)
                radarView?.setPositions(passData.satTrack)
                setPassText(pass, passData.satPos, binding)
            })
            viewModel.transmitters.observe(viewLifecycleOwner, { list ->
                if (list.isNotEmpty()) {
                    transmittersAdapter.submitList(list)
                    binding.radarRecycler.visibility = View.VISIBLE
                    binding.radarRecyclerMsg.visibility = View.INVISIBLE
                } else {
                    binding.radarRecycler.visibility = View.INVISIBLE
                    binding.radarRecyclerMsg.visibility = View.VISIBLE
                }
                radarView?.invalidate()
            })
            viewModel.orientation.observe(viewLifecycleOwner, { orientation ->
                radarView?.setOrientation(
                    orientation.first,
                    orientation.second,
                    orientation.third
                )
            })
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
                    findNavController().navigateSafe(R.id.action_radar_to_passes)
                }
            }
        } else {
            binding.radarTimer.text = 0L.toTimerString()
        }
    }
}
