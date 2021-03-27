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
import android.widget.SearchView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.data.model.Result
import com.rtbishop.look4sat.data.model.SatItem
import com.rtbishop.look4sat.data.model.TleSource
import com.rtbishop.look4sat.databinding.FragmentEntriesBinding
import com.rtbishop.look4sat.utility.RecyclerDivider
import com.rtbishop.look4sat.utility.getNavResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EntriesFragment : Fragment(R.layout.fragment_entries), SearchView.OnQueryTextListener {

    private val viewModel: EntriesViewModel by viewModels()
    private val filePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { viewModel.importSatDataFromFile(uri) }
        }
    private var binding: FragmentEntriesBinding? = null
    private var entriesAdapter: EntriesAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupComponents(view)
        setupObservers()
    }

    private fun setupComponents(view: View) {
        entriesAdapter = EntriesAdapter()
        binding = FragmentEntriesBinding.bind(view).apply {
            entriesRecycler.apply {
                setHasFixedSize(true)
                adapter = entriesAdapter
                layoutManager = LinearLayoutManager(requireContext())
                (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
                addItemDecoration(RecyclerDivider(R.drawable.rec_divider_light))
            }
            importWeb.setOnClickListener { findNavController().navigate(R.id.nav_dialog_sources) }
            importFile.setOnClickListener { filePicker.launch("*/*") }
            selectMode.setOnClickListener { showModesDialog() }
            selectAll.setOnClickListener { entriesAdapter?.selectCurrentItems() }
            entriesSubmit.setOnClickListener { updateEntriesSelection() }
            searchBar.setOnQueryTextListener(this@EntriesFragment)
        }
    }

    private fun setupObservers() {
        viewModel.satData.observe(viewLifecycleOwner, { result ->
            when (result) {
                is Result.Success -> setLoaded(result.data)
                is Result.InProgress -> setLoading()
                is Result.Error -> setError()
            }
        })
        getNavResult<List<String>>(R.id.nav_entries, "sources") { result ->
            result.map { TleSource(it) }.let { sources ->
                if (sources.isNullOrEmpty()) {
                    viewModel.importSatDataFromSources()
                } else {
                    viewModel.importSatDataFromSources(sources)
                }
            }
        }
    }

    private fun setLoaded(items: List<SatItem>) {
        if (items.isEmpty()) {
            binding?.apply {
                entriesError.visibility = View.VISIBLE
                entriesProgress.visibility = View.INVISIBLE
                entriesRecycler.visibility = View.INVISIBLE
            }
        } else {
            entriesAdapter?.submitAllItems(items)
            binding?.apply {
                entriesError.visibility = View.INVISIBLE
                entriesProgress.visibility = View.INVISIBLE
                entriesRecycler.visibility = View.VISIBLE
                entriesRecycler.scrollToPosition(0)
            }
        }
    }

    private fun setLoading() {
        binding?.apply {
            entriesError.visibility = View.INVISIBLE
            entriesProgress.visibility = View.VISIBLE
            entriesRecycler.visibility = View.INVISIBLE
        }
    }

    private fun setError() {
        binding?.apply {
            entriesError.visibility = View.INVISIBLE
            entriesProgress.visibility = View.INVISIBLE
            entriesRecycler.visibility = View.VISIBLE
        }
        val errorMsg = getString(R.string.entries_update_error)
        Snackbar.make(requireView(), errorMsg, Snackbar.LENGTH_SHORT).show()
    }

    private fun filterAndScroll(query: String) {
        entriesAdapter?.filterItems(query)
        binding?.entriesRecycler?.scrollToPosition(0)
    }

    private fun updateEntriesSelection() {
        setLoading()
        lifecycleScope.launch {
            entriesAdapter?.getSelectedIds()?.let { ids ->
                viewModel.updateEntriesSelection(ids, true)
            }
            findNavController().navigate(R.id.nav_passes)
        }
    }

    private fun showModesDialog() {
        val modes = arrayOf(
            "AFSK", "AFSK S-Net", "AFSK SALSAT", "AHRPT", "AM", "APT", "BPSK", "BPSK PMT-A3",
            "CERTO", "CW", "DQPSK", "DSTAR", "DUV", "FFSK", "FM", "FMN", "FSK", "FSK AX.100 Mode 5",
            "FSK AX.100 Mode 6", "FSK AX.25 G3RUH", "GFSK", "GFSK Rktr", "GMSK", "HRPT", "LoRa",
            "LRPT", "LSB", "MFSK", "MSK", "MSK AX.100 Mode 5", "MSK AX.100 Mode 6", "OFDM", "OQPSK",
            "PSK", "PSK31", "PSK63", "QPSK", "QPSK31", "QPSK63", "SSTV", "USB", "WSJT"
        )
        val savedModes = BooleanArray(modes.size)
        val selectedModes = viewModel.getModesSelection().toMutableList()
        selectedModes.forEach { savedModes[modes.indexOf(it)] = true }
        MaterialAlertDialogBuilder(requireContext()).apply {
            setTitle(getString(R.string.modes_title))
            setMultiChoiceItems(modes, savedModes) { _, which, isChecked ->
                when {
                    isChecked -> selectedModes.add(modes[which])
                    selectedModes.contains(modes[which]) -> selectedModes.remove(modes[which])
                }
            }
            setPositiveButton(getString(android.R.string.ok)) { _, _ ->
                viewModel.filterByModes(selectedModes)
            }
            setNeutralButton(getString(android.R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            create()
            show()
        }
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        return true
    }

    override fun onQueryTextChange(newText: String): Boolean {
        filterAndScroll(newText)
        return true
    }

    override fun onDestroyView() {
        entriesAdapter = null
        binding = null
        super.onDestroyView()
    }
}
