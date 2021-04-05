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
package com.rtbishop.look4sat.ui.entriesScreen

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.snackbar.Snackbar
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.data.model.Result
import com.rtbishop.look4sat.data.model.SatItem
import com.rtbishop.look4sat.data.model.TleSource
import com.rtbishop.look4sat.databinding.FragmentEntriesBinding
import com.rtbishop.look4sat.utility.RecyclerDivider
import com.rtbishop.look4sat.utility.getNavResult
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EntriesFragment : Fragment(R.layout.fragment_entries) {

    private val viewModel: EntriesViewModel by viewModels()
    private val contentContract = ActivityResultContracts.GetContent()
    private val filePicker = registerForActivityResult(contentContract) { uri ->
        uri?.let { viewModel.importSatDataFromFile(uri) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupComponents(view)
    }

    private fun setupComponents(view: View) {
        val entriesAdapter = EntriesAdapter().apply {
            setEntriesClickListener(viewModel)
        }
        val binding = FragmentEntriesBinding.bind(view).apply {
            entriesRecycler.apply {
                setHasFixedSize(true)
                adapter = entriesAdapter
                layoutManager = LinearLayoutManager(requireContext())
                (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
                addItemDecoration(RecyclerDivider(R.drawable.rec_divider_light))
            }
            importWeb.setOnClickListener { findNavController().navigate(R.id.nav_dialog_sources) }
            importFile.setOnClickListener { filePicker.launch("*/*") }
            selectMode.setOnClickListener { viewModel.createModesDialog(requireContext()).show() }
            selectAll.setOnClickListener { viewModel.selectCurrentItems() }
            searchBar.setOnQueryTextListener(viewModel)
        }
        viewModel.satData.observe(viewLifecycleOwner, { satData ->
            handleSatData(satData, binding, entriesAdapter)
        })
        getNavResult<List<String>>(R.id.nav_entries, "sources") { navResult ->
            handleNavResult(navResult)
        }
    }

    private fun handleSatData(
        result: Result<List<SatItem>>,
        binding: FragmentEntriesBinding,
        entriesAdapter: EntriesAdapter
    ) {
        when (result) {
            is Result.Success -> {
                entriesAdapter.submitList(result.data)
                binding.entriesProgress.visibility = View.INVISIBLE
                binding.entriesRecycler.visibility = View.VISIBLE
                binding.entriesRecycler.scrollToPosition(0)
            }
            is Result.InProgress -> {
                binding.entriesProgress.visibility = View.VISIBLE
                binding.entriesRecycler.visibility = View.INVISIBLE
            }
            is Result.Error -> {
                binding.entriesProgress.visibility = View.INVISIBLE
                binding.entriesRecycler.visibility = View.VISIBLE
                Snackbar.make(requireView(), R.string.entries_update_error, Snackbar.LENGTH_SHORT)
                    .setAnchorView(R.id.nav_bottom).show()
            }
        }
    }

    private fun handleNavResult(result: List<String>) {
        result.map { sourceUrl -> TleSource(sourceUrl) }.let { sources ->
            viewModel.importSatDataFromSources(sources)
        }
    }
}
