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

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.SharedViewModel
import com.rtbishop.look4sat.data.Result
import com.rtbishop.look4sat.data.SatPass
import com.rtbishop.look4sat.databinding.FragmentPassesBinding
import com.rtbishop.look4sat.ui.adapters.PassesAdapter
import com.rtbishop.look4sat.utility.PrefsManager
import com.rtbishop.look4sat.utility.Utilities
import com.rtbishop.look4sat.utility.Utilities.getRotationAnimator
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class PassesFragment : Fragment(R.layout.fragment_passes) {

    @Inject
    lateinit var prefsManager: PrefsManager

    private lateinit var binding: FragmentPassesBinding
    private lateinit var animator: ObjectAnimator
    private lateinit var passesAdapter: PassesAdapter
    private val viewModel: SharedViewModel by activityViewModels()
    private var passes = mutableListOf<SatPass>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentPassesBinding.bind(view)
        setupComponents()
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.getPasses().observe(viewLifecycleOwner, { result ->
            when (result) {
                is Result.Success -> {
                    animator.cancel()
                    passes = result.data
                    passesAdapter.setList(passes)
                }
                is Result.InProgress -> animator.start()
            }
        })
        viewModel.getCurrentTimeMillis().observe(viewLifecycleOwner, { currentTime ->
            tickMainTimer(currentTime)
            passesAdapter.tickPasses(currentTime)
        })
    }

    private fun setupComponents() {
        passesAdapter = PassesAdapter(requireContext(), prefsManager.shouldUseUTC())
        binding.apply {
            passesRecycler.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = passesAdapter
                isVerticalScrollBarEnabled = false
                (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
                setHasFixedSize(true)
            }
            animator = passesFab.getRotationAnimator()
            passesFab.setOnClickListener {
                if (animator.isRunning) {
                    animator.cancel()
                } else {
                    viewModel.calculatePasses()
                }
            }
        }
        viewModel.triggerCalculation()
    }

    private fun tickMainTimer(timeNow: Long) {
        if (passes.isNotEmpty()) {
            try {
                val nextPass = passes.first { it.pass.startTime.time.minus(timeNow) > 0 }
                val millisBeforeStart = nextPass.pass.startTime.time.minus(timeNow)
                binding.passesTimer.text = Utilities.formatForTimer(millisBeforeStart)
            } catch (e: NoSuchElementException) {
                val lastPass = passes.last()
                val millisBeforeEnd = lastPass.pass.endTime.time.minus(timeNow)
                binding.passesTimer.text = Utilities.formatForTimer(millisBeforeEnd)
            }
        } else {
            binding.passesTimer.text = Utilities.formatForTimer(0L)
        }
    }
}
