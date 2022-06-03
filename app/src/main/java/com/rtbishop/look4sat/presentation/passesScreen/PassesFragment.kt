/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2022 Arty Bishop (bishop.arty@gmail.com)
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
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.databinding.FragmentPassesBinding
import com.rtbishop.look4sat.domain.model.DataState
import com.rtbishop.look4sat.domain.predict.SatPass
import com.rtbishop.look4sat.presentation.clickWithDebounce
import com.rtbishop.look4sat.presentation.getNavResult
import com.rtbishop.look4sat.utility.toTimerString
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PassesFragment : Fragment(R.layout.fragment_passes), PassesAdapter.PassesClickListener {

    private val viewModel: PassesViewModel by viewModels()
    private val passesAdapter = PassesAdapter(this)
    private var binding: FragmentPassesBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentPassesBinding.bind(view).apply {
            passesAdapter.setUTC(viewModel.shouldUseUTC())
            passesRecycler.apply {
                setHasFixedSize(true)
                adapter = passesAdapter
                layoutManager = LinearLayoutManager(requireContext())
                (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
                addItemDecoration(DividerItemDecoration(requireContext(), 1))
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        if (dy > 0 && passesFab.visibility == View.VISIBLE) passesFab.hide()
                        else if (dy < 0 && passesFab.visibility != View.VISIBLE) passesFab.show()
                    }
                })
            }
            passesBtnRefresh.clickWithDebounce { viewModel.calculatePasses() }
            passesBtnMap.clickWithDebounce {
                val dir = PassesFragmentDirections.globalToMap()
                findNavController().navigate(dir)
            }
            passesBtnFilter.clickWithDebounce {
                val dir = PassesFragmentDirections.passesToFilter()
                findNavController().navigate(dir)
            }
            passesBtnSettings.clickWithDebounce {
                val dir = PassesFragmentDirections.globalToSettings()
                findNavController().navigate(dir)
            }
            setupObservers()
        }
    }

    override fun navigateToPass(satPass: SatPass) {
        if (satPass.progress < 100) satPass.run {
            val dir = PassesFragmentDirections.globalToRadar(this.catNum, this.aosTime)
            findNavController().navigate(dir)
        }
    }

    override fun onDestroyView() {
        binding?.passesRecycler?.adapter = null
        binding = null
        super.onDestroyView()
    }

    private fun setupObservers() {
        viewModel.entriesTotal.observe(viewLifecycleOwner) { number ->
            handleEntriesTotal(number)
        }
        viewModel.passes.observe(viewLifecycleOwner) { passesResult ->
            handleNewPasses(passesResult)
        }
        getNavResult<Pair<Int, Double>>(R.id.nav_passes, "prefs") { prefs ->
            viewModel.calculatePasses(prefs.first, prefs.second)
        }
        getNavResult<List<Int>>(R.id.nav_passes, "selection") { items ->
            viewModel.calculatePasses(selection = items)
        }
    }

    private fun handleEntriesTotal(number: Int) {
        binding?.run {
            if (number > 0) {
                passesFab.clickWithDebounce {
                    val direction = PassesFragmentDirections.globalToEntries()
                    findNavController().navigate(direction)
                }
            } else {
                passesFab.clickWithDebounce {
                    val errorMessage = getString(R.string.passes_empty_db)
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handleNewPasses(state: DataState<List<SatPass>>) {
        binding?.run {
            when (state) {
                is DataState.Success -> {
                    passesAdapter.submitList(state.data)
                    tickMainTimer(state.data)
                    if (state.data.isNotEmpty()) { // show new passes list
                        passesEmpty.visibility = View.INVISIBLE
                        passesProgress.visibility = View.INVISIBLE
                    } else { // show no passes message
                        passesEmpty.visibility = View.VISIBLE
                        passesProgress.visibility = View.INVISIBLE
                    }
                }
                is DataState.Loading -> {
                    passesEmpty.visibility = View.INVISIBLE
                    passesProgress.visibility = View.VISIBLE
                    passesTimer.text = 0L.toTimerString()
                }
                else -> {}
            }
        }
    }

    private fun tickMainTimer(passes: List<SatPass>) {
        binding?.run {
            if (passes.isNotEmpty()) {
                val timeNow = System.currentTimeMillis()
                try {
                    val nextPass = passes.first { it.aosTime.minus(timeNow) > 0 }
                    val millisBeforeStart = nextPass.aosTime.minus(timeNow)
                    passesTimer.text = millisBeforeStart.toTimerString()
                } catch (e: NoSuchElementException) {
                    val lastPass = passes.last()
                    val millisBeforeEnd = lastPass.losTime.minus(timeNow)
                    passesTimer.text = millisBeforeEnd.toTimerString()
                }
            } else {
                passesTimer.text = 0L.toTimerString()
            }
            passesBtnRefresh.isEnabled = true
        }
    }
}
