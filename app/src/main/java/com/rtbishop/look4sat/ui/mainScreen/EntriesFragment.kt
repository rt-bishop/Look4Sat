/*******************************************************************************
 Look4Sat. Amateur radio satellite tracker and pass predictor.
 Copyright (C) 2019, 2020 Arty Bishop (bishop.arty@gmail.com)

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along
 with this program; if not, write to the Free Software Foundation, Inc.,
 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 ******************************************************************************/

package com.rtbishop.look4sat.ui.mainScreen

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
import com.rtbishop.look4sat.SharedViewModel
import com.rtbishop.look4sat.data.SatEntry
import com.rtbishop.look4sat.databinding.FragmentEntriesBinding
import com.rtbishop.look4sat.ui.adapters.EntriesAdapter
import com.rtbishop.look4sat.utility.RecyclerDivider
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EntriesFragment : Fragment(R.layout.fragment_entries) {
    
    private val viewModel: SharedViewModel by activityViewModels()
    private val filePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            viewModel.updateEntriesFromFile(uri)
        }
    private var binding: FragmentEntriesBinding? = null
    private var entriesAdapter: EntriesAdapter? = null
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentEntriesBinding.bind(view)
        setupComponents()
        observeEntries()
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
            selectAll.setOnClickListener { entriesAdapter?.selectAll() }
            entriesFab.setOnClickListener { navigateToPasses() }
            searchBar.setOnQueryTextListener(entriesAdapter)
            searchBar.clearFocus()
        }
    }

    private fun observeEntries() {
        viewModel.getEntries().observe(viewLifecycleOwner, { entries ->
            if (entries.isNullOrEmpty()) setError()
            else {
                viewModel.setEntries(entries)
                entriesAdapter?.setEntries(entries as MutableList<SatEntry>)
                setLoaded()
            }
            observeEvents()
        })
    }

    private fun observeEvents() {
        viewModel.getAppEvent().observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                if (it == 0) setLoading()
                else if (it == 1) {
                    val errorMsg = getString(R.string.entries_update_error)
                    Snackbar.make(requireView(), errorMsg, Snackbar.LENGTH_SHORT).show()
                    setError()
                }
            }
        })
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
            entriesRecycler.visibility = View.INVISIBLE
            entriesProgress.visibility = View.VISIBLE
        }
    }
    
    private fun setError() {
        binding?.apply {
            entriesProgress.visibility = View.INVISIBLE
            entriesRecycler.visibility = View.INVISIBLE
            entriesError.visibility = View.VISIBLE
        }
    }
    
    private fun showImportFromWebDialog() {
        findNavController().navigate(R.id.action_entries_to_dialog_sources)
    }
    
    private fun navigateToPasses() {
        binding?.searchBar?.clearFocus()
        entriesAdapter?.let {
            val entries = it.getEntries()
            if (entries.isNotEmpty()) {
                viewModel.updateEntriesSelection(entries)
                requireView().findNavController().navigate(R.id.action_entries_to_passes)
            }
        }
    }

    override fun onDestroyView() {
        entriesAdapter = null
        binding = null
        super.onDestroyView()
    }
}