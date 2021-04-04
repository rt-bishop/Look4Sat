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
package com.rtbishop.look4sat.ui.polarScreen

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.data.model.SatPass
import com.rtbishop.look4sat.databinding.FragmentPolarBinding
import com.rtbishop.look4sat.utility.RecyclerDivider
import com.rtbishop.look4sat.utility.formatForTimer
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class PolarFragment : Fragment(R.layout.fragment_polar) {

    private val viewModel: PolarViewModel by viewModels()
    private var polarView: PolarView? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentPolarBinding.bind(view)
        val catNum = requireArguments().getInt("catNum")
        val aosTime = requireArguments().getLong("aosTime")
        viewModel.getPass(catNum, aosTime).observe(viewLifecycleOwner) { pass ->
            polarView = PolarView(requireContext()).apply { setPass(pass) }
            binding.frame.addView(polarView)
            observeTransmitters(pass, binding)
        }
        viewModel.azimuth.observe(viewLifecycleOwner, { trueNorthAzimuth ->
            polarView?.rotation = -trueNorthAzimuth
        })
    }

    override fun onResume() {
        super.onResume()
        viewModel.enableSensor()
    }

    override fun onPause() {
        super.onPause()
        viewModel.disableSensor()
    }

    private fun observeTransmitters(satPass: SatPass, binding: FragmentPolarBinding) {
        viewModel.getSatTransmitters(satPass.catNum).observe(viewLifecycleOwner, { list ->
            val transmitterAdapter = TransAdapter(satPass)
            if (list.isNotEmpty()) {
                transmitterAdapter.setData(list)
                binding.recycler.apply {
                    setHasFixedSize(true)
                    adapter = transmitterAdapter
                    isVerticalScrollBarEnabled = false
                    layoutManager = LinearLayoutManager(context)
                    (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
                    addItemDecoration(RecyclerDivider(R.drawable.rec_divider_dark))
                    visibility = View.VISIBLE
                }
                binding.noTransMsg.visibility = View.INVISIBLE
            } else {
                binding.recycler.visibility = View.INVISIBLE
                binding.noTransMsg.visibility = View.VISIBLE
            }
            viewModel.getAppTimer().observe(viewLifecycleOwner, {
                setPassText(it, satPass, binding)
                polarView?.invalidate()
                transmitterAdapter.tickTransmitters()
            })
        })
    }

    private fun setPassText(timeNow: Long, satPass: SatPass, binding: FragmentPolarBinding) {
        val dateNow = Date(timeNow)
        val satPos = satPass.predictor.getSatPos(dateNow)
        val polarAz = getString(R.string.pat_azimuth)
        val polarEl = getString(R.string.pat_elevation)
        val polarRng = getString(R.string.pat_distance)
        val polarAlt = getString(R.string.pat_altitude)
        binding.azimuth.text = String.format(polarAz, Math.toDegrees(satPos.azimuth))
        binding.elevation.text = String.format(polarEl, Math.toDegrees(satPos.elevation))
        binding.distance.text = String.format(polarRng, satPos.range)
        binding.altitude.text = String.format(polarAlt, satPos.altitude)

        if (!satPass.isDeepSpace) {
            if (dateNow.before(satPass.aosDate)) {
                val millisBeforeStart = satPass.aosDate.time.minus(timeNow)
                binding.polarTimer.text = millisBeforeStart.formatForTimer()
            } else {
                val millisBeforeEnd = satPass.losDate.time.minus(timeNow)
                binding.polarTimer.text = millisBeforeEnd.formatForTimer()
                if (dateNow.after(satPass.losDate)) {
                    binding.polarTimer.text = 0L.formatForTimer()
                    findNavController().popBackStack()
                }
            }
        } else {
            binding.polarTimer.text = 0L.formatForTimer()
        }
    }
}
