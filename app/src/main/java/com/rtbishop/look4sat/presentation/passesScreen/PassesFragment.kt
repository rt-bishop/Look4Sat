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

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.framework.model.Result
import com.rtbishop.look4sat.databinding.FragmentPassesBinding
import com.rtbishop.look4sat.domain.predict4kotlin.SatPass
import com.rtbishop.look4sat.utility.RecyclerDivider
import com.rtbishop.look4sat.utility.formatForTimer
import com.rtbishop.look4sat.utility.navigateSafe
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class PassesFragment : Fragment(R.layout.fragment_passes), PassesAdapter.PassesClickListener {

    private val viewModel: PassesViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupComponents(view)
    }

    private fun setupComponents(view: View) {
        val passesAdapter = PassesAdapter(viewModel.shouldUseUTC(), this)
        val binding = FragmentPassesBinding.bind(view).apply {
            passesRecycler.apply {
                setHasFixedSize(true)
                adapter = passesAdapter
                isVerticalScrollBarEnabled = false
                layoutManager = LinearLayoutManager(context)
                (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
                addItemDecoration(RecyclerDivider(R.drawable.rec_divider_dark))
            }
            passesRefresh.setOnClickListener { viewModel.forceCalculation() }
        }
        viewModel.passes.observe(viewLifecycleOwner, { passesResult ->
            handleNewPasses(passesResult, passesAdapter, binding)
        })
    }

    private fun handleNewPasses(
        result: Result<List<SatPass>>,
        passesAdapter: PassesAdapter,
        binding: FragmentPassesBinding
    ) {
        when (result) {
            is Result.Success -> {
                passesAdapter.submitList(result.data)
                binding.apply {
                    passesError.visibility = View.INVISIBLE
                    passesProgress.visibility = View.INVISIBLE
                    passesRecycler.visibility = View.VISIBLE
                }
                tickMainTimer(result.data, binding)
            }
            is Result.InProgress -> {
                binding.apply {
                    passesTimer.text = 0L.formatForTimer()
                    passesError.visibility = View.INVISIBLE
                    passesRecycler.visibility = View.INVISIBLE
                    passesProgress.visibility = View.VISIBLE
                }
            }
            is Result.Error -> {
                binding.apply {
                    passesTimer.text = 0L.formatForTimer()
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
                val nextPass = passes.first { it.aosDate.time.minus(timeNow) > 0 }
                val millisBeforeStart = nextPass.aosDate.time.minus(timeNow)
                binding.passesTimer.text = millisBeforeStart.formatForTimer()
            } catch (e: NoSuchElementException) {
                val lastPass = passes.last()
                val millisBeforeEnd = lastPass.losDate.time.minus(timeNow)
                binding.passesTimer.text = millisBeforeEnd.formatForTimer()
            }
        } else {
            binding.passesTimer.text = 0L.formatForTimer()
        }
    }

    override fun navigateToPass(satPass: SatPass) {
        if (satPass.progress < 100) {
            val bundle = bundleOf("catNum" to satPass.catNum, "aosTime" to satPass.aosDate.time)
            findNavController().navigateSafe(R.id.action_passes_to_polar, bundle)
        }
    }
}
