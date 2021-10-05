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
package com.rtbishop.look4sat.presentation.passesScreen

import android.Manifest
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.databinding.FragmentPassesBinding
import com.rtbishop.look4sat.predict4kotlin.SatPass
import com.rtbishop.look4sat.common.DataState
import com.rtbishop.look4sat.presentation.ItemDivider
import com.rtbishop.look4sat.utility.navigateSafe
import com.rtbishop.look4sat.utility.toTimerString
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class PassesFragment : Fragment(R.layout.fragment_passes), PassesAdapter.PassesClickListener {

    private val passesViewModel: PassesViewModel by viewModels()
    private val permRequest = ActivityResultContracts.RequestPermission()
    private val permRequestLauncher = registerForActivityResult(permRequest) {
        passesViewModel.triggerInitialSetup()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupComponents(view)
    }

    private fun setupComponents(view: View) {
        val passesAdapter = PassesAdapter(passesViewModel.shouldUseUTC(), this)
        val binding = FragmentPassesBinding.bind(view).apply {
            passesRecycler.apply {
                setHasFixedSize(true)
                adapter = passesAdapter
                isVerticalScrollBarEnabled = false
                layoutManager = LinearLayoutManager(context)
                (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
                addItemDecoration(ItemDivider(R.drawable.rec_divider_dark))
            }
            passesRefresh.setOnClickListener { passesViewModel.forceCalculation() }
        }
        passesViewModel.passes.observe(viewLifecycleOwner, { passesResult ->
            handleNewPasses(passesResult, passesAdapter, binding)
        })
        passesViewModel.isFirstLaunchDone.observe(viewLifecycleOwner, { setupDone ->
            if (!setupDone) permRequestLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        })
    }

    private fun handleNewPasses(
        dataState: DataState<List<SatPass>>,
        passesAdapter: PassesAdapter,
        binding: FragmentPassesBinding
    ) {
        when (dataState) {
            is DataState.Success -> {
                passesAdapter.submitList(dataState.data)
                binding.apply {
                    passesError.visibility = View.INVISIBLE
                    passesProgress.visibility = View.INVISIBLE
                    passesRecycler.visibility = View.VISIBLE
                }
                tickMainTimer(dataState.data, binding)
            }
            is DataState.Loading -> {
                binding.apply {
                    passesTimer.text = 0L.toTimerString()
                    passesError.visibility = View.INVISIBLE
                    passesRecycler.visibility = View.INVISIBLE
                    passesProgress.visibility = View.VISIBLE
                }
            }
            is DataState.Error -> {
                binding.apply {
                    passesTimer.text = 0L.toTimerString()
                    passesProgress.visibility = View.INVISIBLE
                    passesRecycler.visibility = View.INVISIBLE
                    passesError.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun tickMainTimer(passes: List<SatPass>, binding: FragmentPassesBinding) {
        if (passes.isNotEmpty()) {
            val timeNow = System.currentTimeMillis()
            try {
                val nextPass = passes.first { it.aosTime.minus(timeNow) > 0 }
                val millisBeforeStart = nextPass.aosTime.minus(timeNow)
                binding.passesTimer.text = millisBeforeStart.toTimerString()
            } catch (e: NoSuchElementException) {
                val lastPass = passes.last()
                val millisBeforeEnd = lastPass.losTime.minus(timeNow)
                binding.passesTimer.text = millisBeforeEnd.toTimerString()
            }
        } else {
            binding.passesTimer.text = 0L.toTimerString()
        }
    }

    override fun navigateToPass(satPass: SatPass) {
        if (satPass.progress < 100) {
            val bundle = bundleOf("catNum" to satPass.catNum, "aosTime" to satPass.aosTime)
            findNavController().navigateSafe(R.id.action_passes_to_radar, bundle)
        }
    }
}
