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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.model.SatItem
import com.rtbishop.look4sat.databinding.FragmentEntriesBinding
import com.rtbishop.look4sat.common.DataState
import com.rtbishop.look4sat.presentation.ItemDivider
import com.rtbishop.look4sat.utility.getNavResult
import com.rtbishop.look4sat.utility.navigateSafe
import com.rtbishop.look4sat.utility.showSnack
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EntriesFragment : Fragment(R.layout.fragment_entries) {

    private val viewModel: EntriesViewModel by viewModels()
    private val contentContract = ActivityResultContracts.GetContent()
    private val filePicker = registerForActivityResult(contentContract) { uri ->
        uri?.let { viewModel.updateEntriesFromFile(uri) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupComponents(view)
    }

    private fun setupComponents(view: View) {
        val entriesAdapter = EntriesAdapter(object: EntriesAdapter.EntriesClickListener {
            override fun updateSelection(catNums: List<Int>, isSelected: Boolean) {
                viewModel.updateSelection(catNums, isSelected)
            }
        })
        val binding = FragmentEntriesBinding.bind(view).apply {
            entriesRecycler.apply {
                setHasFixedSize(true)
                adapter = entriesAdapter
                layoutManager = LinearLayoutManager(requireContext())
                (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
                addItemDecoration(ItemDivider(R.drawable.rec_divider_light))
            }
            entriesImportWeb.setOnClickListener {
                findNavController().navigateSafe(R.id.action_entries_to_sources)
            }
            entriesImportFile.setOnClickListener { filePicker.launch("*/*") }
            entriesSelectMode.setOnClickListener { showModesDialog() }
            entriesSelectAll.setOnClickListener { viewModel.selectCurrentItems() }
            entriesSearchBar.setOnQueryTextListener(viewModel)
        }
        viewModel.satData.observe(viewLifecycleOwner, { satData ->
            handleSatData(satData, binding, entriesAdapter)
        })
        getNavResult<List<String>>(R.id.nav_entries, "sources") { sources ->
            viewModel.updateEntriesFromWeb(sources)
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
                requireView().showSnack(getString(R.string.entries_update_error))
            }
            else -> {
            }
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
        val selectedModes = viewModel.loadSelectedModes().toMutableList()
        selectedModes.forEach { savedModes[modes.indexOf(it)] = true }
        val dialogBuilder = MaterialAlertDialogBuilder(requireContext()).apply {
            setTitle(context.getString(R.string.modes_title))
            setMultiChoiceItems(modes, savedModes) { _, which, isChecked ->
                when {
                    isChecked -> selectedModes.add(modes[which])
                    selectedModes.contains(modes[which]) -> selectedModes.remove(modes[which])
                }
            }
            setPositiveButton(context.getString(android.R.string.ok)) { _, _ ->
                viewModel.saveSelectedModes(selectedModes)
            }
            setNeutralButton(context.getString(android.R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
        }
        dialogBuilder.create().show()
    }
}
