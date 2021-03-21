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
package com.rtbishop.look4sat.ui.passesScreen

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.data.model.Result
import com.rtbishop.look4sat.data.model.SatPass
import com.rtbishop.look4sat.databinding.FragmentPassesBinding
import com.rtbishop.look4sat.utility.RecyclerDivider
import com.rtbishop.look4sat.utility.formatForTimer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import java.util.*

@FlowPreview
@AndroidEntryPoint
class PassesFragment : Fragment(R.layout.fragment_passes) {

    private val viewModel: PassesViewModel by viewModels()
    private var passes = mutableListOf<SatPass>()
    private var binding: FragmentPassesBinding? = null
    private var passesAdapter: PassesAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentPassesBinding.bind(view)
        setupComponents()
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.passes.observe(viewLifecycleOwner, { result ->
            if (result is Result.Success) {
                if (result.data.isEmpty()) {
                    binding?.apply {
                        passesProgress.visibility = View.INVISIBLE
                        passesRecycler.visibility = View.INVISIBLE
                        passesError.visibility = View.VISIBLE
                    }
                } else {
                    passes = result.data
                    passesAdapter?.setList(passes)
                    binding?.apply {
                        passesError.visibility = View.INVISIBLE
                        passesProgress.visibility = View.INVISIBLE
                        passesRecycler.visibility = View.VISIBLE
                    }
                }
            } else if (result is Result.InProgress) {
                passes.clear()
                binding?.apply {
                    passesTimer.text = 0L.formatForTimer()
                    passesError.visibility = View.INVISIBLE
                    passesRecycler.visibility = View.INVISIBLE
                    passesProgress.visibility = View.VISIBLE
                }
            }
        })
        viewModel.getAppTimer().observe(viewLifecycleOwner, { currentTime ->
            tickMainTimer(currentTime)
            passesAdapter?.tickPasses(currentTime)
        })
    }

    private fun setupComponents() {
        passesAdapter = PassesAdapter(viewModel.shouldUseUTC())
        binding?.apply {
            passesRecycler.apply {
                setHasFixedSize(true)
                adapter = passesAdapter
                isVerticalScrollBarEnabled = false
                layoutManager = LinearLayoutManager(context)
                (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
                addItemDecoration(RecyclerDivider(R.drawable.rec_divider_dark))
            }
        }
    }

    private fun tickMainTimer(timeNow: Long) {
        if (passes.isNotEmpty()) {
            try {
                val nextPass = passes.first { it.pass.startTime.time.minus(timeNow) > 0 }
                val millisBeforeStart = nextPass.pass.startTime.time.minus(timeNow)
                binding?.passesTimer?.text = millisBeforeStart.formatForTimer()
            } catch (e: NoSuchElementException) {
                val lastPass = passes.last()
                val millisBeforeEnd = lastPass.pass.endTime.time.minus(timeNow)
                binding?.passesTimer?.text = millisBeforeEnd.formatForTimer()
            }
        } else {
            binding?.passesTimer?.text = 0L.formatForTimer()
        }
    }

    override fun onDestroyView() {
        passesAdapter = null
        binding = null
        super.onDestroyView()
    }
}
