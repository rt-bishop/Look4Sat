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
import android.text.Editable
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.snackbar.Snackbar
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.data.model.Result
import com.rtbishop.look4sat.data.model.TleSource
import com.rtbishop.look4sat.databinding.FragmentEntriesBinding
import com.rtbishop.look4sat.utility.RecyclerDivider
import com.rtbishop.look4sat.utility.getNavResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview

@FlowPreview
@AndroidEntryPoint
class EntriesFragment : Fragment(R.layout.fragment_entries), EntriesAdapter.EntriesClickListener {

    private val viewModel: EntriesViewModel by viewModels()
    private val filePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { viewModel.importSatDataFromFile(uri) }
        }
    private var binding: FragmentEntriesBinding? = null
    private var entriesAdapter: EntriesAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentEntriesBinding.bind(view)
        setupComponents()
        observeSatelliteData()
        observeSourcesResult()
    }

    private fun setupComponents() {
        entriesAdapter = EntriesAdapter().apply {
            setEntriesClickListener(this@EntriesFragment)
        }
        binding?.apply {
            entriesRecycler.apply {
                setHasFixedSize(true)
                adapter = entriesAdapter
                layoutManager = LinearLayoutManager(requireContext())
                (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
                addItemDecoration(RecyclerDivider(R.drawable.rec_divider_light))
            }
            importWeb.setOnClickListener { showImportFromWebDialog() }
            importFile.setOnClickListener { filePicker.launch("*/*") }
            selectAll.setOnClickListener { entriesAdapter?.selectAllItems() }
            searchBar.addTextChangedListener { query -> filterByQuery(query) }
            searchBar.clearFocus()
        }
    }

    private fun observeSatelliteData() {
        viewModel.satData.observe(viewLifecycleOwner, { result ->
            when (result) {
                is Result.Success -> {
                    if (result.data.isEmpty()) {
                        setEmpty()
                    } else {
                        entriesAdapter?.submitAllItems(result.data)
                        setLoaded()
                    }
                }
                is Result.InProgress -> {
                    setLoading()
                }
                is Result.Error -> {
                    val errorMsg = getString(R.string.entries_update_error)
                    Snackbar.make(requireView(), errorMsg, Snackbar.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun observeSourcesResult() {
        getNavResult<List<String>>(R.id.nav_entries, "sources") { result ->
            val sources = result.map { TleSource(it) }
            if (sources.isNullOrEmpty()) {
                viewModel.importSatDataFromSources()
            } else {
                viewModel.importSatDataFromSources(sources)
            }
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

    private fun filterByQuery(it: Editable?) {
        entriesAdapter?.filterItems(it.toString())
        binding?.entriesRecycler?.scrollToPosition(0)
    }

    private fun showImportFromWebDialog() {
        findNavController().navigate(R.id.nav_dialog_sources)
    }

    override fun updateSelection(catNums: List<Int>, isSelected: Boolean) {
        viewModel.updateEntriesSelection(catNums, isSelected)
    }

    override fun onDestroyView() {
        entriesAdapter = null
        binding = null
        super.onDestroyView()
    }
}
