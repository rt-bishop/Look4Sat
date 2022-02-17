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
package com.rtbishop.look4sat.presentation.entriesScreen

import android.os.Bundle
import android.view.View
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.snackbar.Snackbar
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.databinding.FragmentEntriesBinding
import com.rtbishop.look4sat.domain.model.DataState
import com.rtbishop.look4sat.domain.model.SatItem
import com.rtbishop.look4sat.presentation.getNavResult
import com.rtbishop.look4sat.presentation.setNavResult
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EntriesFragment : Fragment(R.layout.fragment_entries) {

    private lateinit var binding: FragmentEntriesBinding
    private val viewModel: EntriesViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentEntriesBinding.bind(view)
        setupComponents()
    }

    private fun setupComponents() {
        val context = requireContext()
        val adapter = EntriesAdapter(viewModel)
        val layoutManager = GridLayoutManager(context, 2)
        val itemDecoration = DividerItemDecoration(context, layoutManager.orientation)
        binding.run {
            entriesRecycler.apply {
                setHasFixedSize(true)
                this.adapter = adapter
                this.layoutManager = layoutManager
                addItemDecoration(itemDecoration)
                (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        if (dy > 0 && entriesFab.visibility == View.VISIBLE) entriesFab.hide()
                        else if (dy < 0 && entriesFab.visibility != View.VISIBLE) entriesFab.show()
                    }
                })
            }
            entriesBackBtn.setOnClickListener { findNavController().navigateUp() }
            entriesSearch.doOnTextChanged { text, _, _, _ -> viewModel.setQuery(text.toString()) }
            entriesModesBtn.setOnClickListener { navigateToModesDialog() }
            entriesSelectBtn.setOnClickListener { viewModel.selectCurrentItems(true) }
            entriesClearBtn.setOnClickListener { viewModel.selectCurrentItems(false) }
            entriesFab.setOnClickListener {
                setNavResult("selection", viewModel.saveSelection())
                findNavController().popBackStack()
            }
        }
        viewModel.satData.observe(viewLifecycleOwner) { satData ->
            handleSatData(satData, binding, adapter)
        }
        getNavResult<List<String>>(R.id.nav_entries, "modes") { modes ->
            viewModel.saveSelectedModes(modes)
        }
    }

    private fun handleSatData(
        dataState: DataState<List<SatItem>>,
        binding: FragmentEntriesBinding,
        entriesAdapter: EntriesAdapter
    ) {
        when (dataState) {
            is DataState.Success -> {
                entriesAdapter.submitList(dataState.data)
                binding.entriesProgress.visibility = View.INVISIBLE
                binding.entriesRecycler.visibility = View.VISIBLE
            }
            is DataState.Loading -> {
                binding.entriesProgress.visibility = View.VISIBLE
                binding.entriesRecycler.visibility = View.INVISIBLE
            }
            is DataState.Error -> {
                binding.entriesProgress.visibility = View.INVISIBLE
                binding.entriesRecycler.visibility = View.VISIBLE
                val message = getString(R.string.entries_update_error)
                Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
            }
            DataState.Handled -> {}
        }
    }

    private fun navigateToModesDialog() {
        val direction = EntriesFragmentDirections.actionEntriesToModes()
        findNavController().navigate(direction)
    }
}
