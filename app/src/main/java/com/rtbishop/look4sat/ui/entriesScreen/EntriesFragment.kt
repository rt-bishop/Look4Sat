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
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.data.model.Result
import com.rtbishop.look4sat.data.model.TleSource
import com.rtbishop.look4sat.databinding.FragmentEntriesBinding
import com.rtbishop.look4sat.ui.SharedViewModel
import com.rtbishop.look4sat.utility.RecyclerDivider
import com.rtbishop.look4sat.utility.getNavResult
import com.rtbishop.look4sat.utility.navigateSafe
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@FlowPreview
@AndroidEntryPoint
class EntriesFragment : Fragment(R.layout.fragment_entries) {

    private val viewModel: SharedViewModel by activityViewModels()
    private val filePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { viewModel.updateSatDataFromFile(uri) }
        }
    private var binding: FragmentEntriesBinding? = null
    private var entriesAdapter: EntriesAdapter? = null

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentEntriesBinding.bind(view)
        setupComponents()
        setupObservers()
        observeSourcesResult()
    }

    private fun setupComponents() {
        entriesAdapter = EntriesAdapter()
        binding?.apply {
            entriesRecycler.apply {
                setHasFixedSize(true)
                adapter = entriesAdapter
                layoutManager = LinearLayoutManager(requireContext())
                addItemDecoration(RecyclerDivider(R.drawable.rec_divider_light))
            }
            importWeb.setOnClickListener { showImportFromWebDialog() }
            importFile.setOnClickListener { filePicker.launch("*/*") }
            selectAll.setOnClickListener { entriesAdapter?.selectAllItems() }
            entriesFab.setOnClickListener { navigateToPasses() }
            searchBar.setOnQueryTextListener(entriesAdapter)
            searchBar.clearFocus()
        }
    }

    @ExperimentalCoroutinesApi
    private fun setupObservers() {
        viewModel.satData.observe(viewLifecycleOwner, { result ->
            when (result) {
                is Result.Success -> {
                    if (result.data.isEmpty()) {
                        setEmpty()
                    } else {
                        entriesAdapter?.setItems(result.data)
                        setLoaded()
                    }
                }
                is Result.InProgress -> {
                    setLoading()
                }
                is Result.Error -> {
                    val errorMsg = getString(R.string.entries_update_error)
                    Snackbar.make(requireView(), errorMsg, Snackbar.LENGTH_SHORT).show()
                    entriesAdapter?.let { adapter ->
                        if (adapter.getItems().isEmpty()) {
                            setEmpty()
                        } else {
                            setLoaded()
                        }
                    }
                }
            }
        })
    }

    private fun observeSourcesResult() {
        getNavResult<List<String>>(R.id.nav_entries, "sources") { result ->
            viewModel.updateSatDataFromSources(result.map { TleSource(it) })
        }
    }

    private fun setLoaded() {
        binding?.apply {
            entriesError.visibility = View.INVISIBLE
            entriesProgress.visibility = View.INVISIBLE
            entriesRecycler.visibility = View.VISIBLE
        }
    }

    private fun setLoading() {
        binding?.apply {
            entriesError.visibility = View.INVISIBLE
            entriesProgress.visibility = View.VISIBLE
            entriesRecycler.visibility = View.INVISIBLE
        }
    }

    private fun setEmpty() {
        binding?.apply {
            entriesError.visibility = View.VISIBLE
            entriesProgress.visibility = View.INVISIBLE
            entriesRecycler.visibility = View.INVISIBLE
        }
    }

    private fun showImportFromWebDialog() {
        findNavController().navigate(R.id.nav_dialog_sources)
    }

    private fun navigateToPasses() {
        binding?.searchBar?.clearFocus()
        entriesAdapter?.let {
            val satItems = it.getItems()
            if (satItems.isNotEmpty()) {
                viewModel.updateItemsSelection(satItems)
                requireView().findNavController().navigateSafe(R.id.action_entries_to_passes)
            }
        }
    }

    override fun onDestroyView() {
        entriesAdapter = null
        binding = null
        super.onDestroyView()
    }
}