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
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.snackbar.Snackbar
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.databinding.FragmentEntriesBinding
import com.rtbishop.look4sat.domain.model.DataState
import com.rtbishop.look4sat.domain.model.SatItem
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EntriesFragment : Fragment(R.layout.fragment_entries) {

    private val viewModel: EntriesViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupComponents(view)
    }

    private fun setupComponents(view: View) {
        val context = requireContext()
        val adapter = EntriesAdapter(viewModel)
        val layoutManager = GridLayoutManager(context, 2)
        val itemDecoration = DividerItemDecoration(context, layoutManager.orientation)
        val binding = FragmentEntriesBinding.bind(view).apply {
            entriesRecycler.apply {
                setHasFixedSize(true)
                this.adapter = adapter
                this.layoutManager = layoutManager
                addItemDecoration(itemDecoration)
                (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
            }
            entriesBack.setOnClickListener { findNavController().navigateUp() }
            entriesSearch.doOnTextChanged { text, _, _, _ -> viewModel.setQuery(text.toString()) }
            entriesMode.setOnClickListener { showModesDialog() }
            entriesSelectAll.setOnClickListener { viewModel.selectCurrentItems() }
        }
        viewModel.satData.observe(viewLifecycleOwner, { satData ->
            handleSatData(satData, binding, adapter)
        })
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
        AlertDialog.Builder(requireContext()).apply {
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
            create().show()
        }
    }
}
